package org.example.oscdspring.file_disk_management;

import org.example.oscdspring.main.Library;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.ProcessState;
import org.example.oscdspring.process_management.Scheduler;
import org.example.oscdspring.util.LogEmitterService;

import java.util.*;

public class FileLockManager {
    private static FileLockManager instance;
    private Map<String, FileLock> lockMap;
    private Scheduler scheduler=Library.getScheduler();
    
    private FileLockManager() {
        lockMap = new HashMap<>();
    }
    
    public static synchronized FileLockManager getInstance() {
        if (instance == null) {
            instance = new FileLockManager();
        }
        return instance;
    }

    // 设置调度器
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    // 添加等待读锁的进程
    public void addReadWaitingProcess(String filename, PCB process) {
        getOrCreateLock(filename).addReadWaitingProcess(process);
    }
    
    // 添加等待写锁的进程
    public void addWriteWaitingProcess(String filename, PCB process) {
        getOrCreateLock(filename).addWriteWaitingProcess(process);
    }
    
    // 获取文件的读锁（指定进程ID）
    public synchronized boolean acquireReadLock(String filename, int pid) {
        return getOrCreateLock(filename).acquireReadLock(pid);
    }
    
    // 释放文件的读锁（指定进程ID）
    public synchronized void releaseReadLock(String filename, int pid) {
        FileLock lock = lockMap.get(filename);
        if (lock != null) {
            lock.releaseReadLock(pid);
            notifyWaitingProcesses(filename);
        }
    }
    
    // 获取文件的写锁（指定进程ID）
    public synchronized boolean acquireWriteLock(String filename, int pid) {
        return getOrCreateLock(filename).acquireWriteLock(pid);
    }
    
    // 释放文件的写锁（指定进程ID）
    public synchronized void releaseWriteLock(String filename, int pid) {
        FileLock lock = lockMap.get(filename);
        if (lock != null) {
            lock.releaseWriteLock(pid);
            notifyWaitingProcesses(filename);
        }
    }
    
    // 通知等待该文件的进程
    private void notifyWaitingProcesses(String filename) {
        if (scheduler == null) return;
        
        FileLock lock = lockMap.get(filename);
        if (lock == null) return;
        
        // 先处理等待写锁的进程（写操作通常优先级更高）
        if (lock.canAcquireWriteLock()) {
            PCB process = lock.getNextWriteWaitingProcess();
            if (process != null) {
                process.setState(ProcessState.READY);
                scheduler.addReadyProcess(process);
                LogEmitterService.getInstance().sendLog("文件 " + filename + " 锁释放，唤醒等待写锁的进程 " + process.getPid());
                return; // 一次只唤醒一个写进程
            }
        }
        
        // 如果没有等待写锁的进程或无法获取写锁，则处理等待读锁的进程
        if (lock.canAcquireReadLock()) {
            List<PCB> processes = lock.getAllReadWaitingProcesses();
            for (PCB process : processes) {
                process.setState(ProcessState.READY);
                scheduler.addReadyProcess(process);
                LogEmitterService.getInstance().sendLog("文件 " + filename + " 锁释放，唤醒等待读锁的进程 " + process.getPid());
            }
        }
    }
    
    /**
     * 释放进程持有的所有文件锁
     * @param pid 进程ID
     */
    public void releaseAllLocks(int pid) {
        for (Map.Entry<String, FileLock> entry : lockMap.entrySet()) {
            String filename = entry.getKey();
            FileLock lock = entry.getValue();
            
            // 检查并释放读锁
            if (lock.isReadLockHeldBy(pid)) {
                lock.releaseReadLock(pid);
                LogEmitterService.getInstance().sendLog("进程 " + pid + " 终止，释放文件 " + filename + " 的读锁");
                notifyWaitingProcesses(filename);
            }
            
            // 检查并释放写锁
            if (lock.isWriteLockHeldBy(pid)) {
                lock.releaseWriteLock(pid);
                LogEmitterService.getInstance().sendLog("进程 " + pid + " 终止，释放文件 " + filename + " 的写锁");
                notifyWaitingProcesses(filename);
            }
            
            // 从等待队列中移除
            lock.removeFromWaitingQueues(pid);
        }
        
        LogEmitterService.getInstance().sendLog("进程 " + pid + " 的所有文件锁已释放");
    }
    
    private FileLock getOrCreateLock(String filename) {
        return lockMap.computeIfAbsent(filename, k -> new FileLock());
    }
    
    /**
     * 自定义文件锁实现
     */
    private class FileLock {
        private List<Integer> readLockHolders; // 持有读锁的进程ID列表
        private int writeLockHolder; // 持有写锁的进程ID，-1表示没有进程持有写锁
        private Queue<PCB> readWaitingQueue; // 等待读锁的进程队列
        private Queue<PCB> writeWaitingQueue; // 等待写锁的进程队列
        
        public FileLock() {
            readLockHolders = new ArrayList<>();
            writeLockHolder = -1;
            readWaitingQueue = new LinkedList<>();
            writeWaitingQueue = new LinkedList<>();
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(100); // 每100毫秒检查一次
                        for(PCB pcb: readWaitingQueue) {
                            if (pcb.getState()==ProcessState.TERMINATED) {
                                readWaitingQueue.remove(pcb);
                            }
                        }
                        for(PCB pcb: writeWaitingQueue) {
                            if (pcb.getState()==ProcessState.TERMINATED) {
                                writeWaitingQueue.remove(pcb);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }
        
        // 判断是否可以获取读锁
        public boolean canAcquireReadLock() {
            return writeLockHolder == -1;
        }
        
        // 判断是否可以获取写锁
        public boolean canAcquireWriteLock() {
            return writeLockHolder == -1 && readLockHolders.isEmpty();
        }
        
        // 获取读锁（指定进程ID）
        public boolean acquireReadLock(int pid) {
            if (canAcquireReadLock()) {
                readLockHolders.add(pid);
                return true;
            }
            return false;
        }
        
        // 释放读锁（指定进程ID）
        public void releaseReadLock(int pid) {
            readLockHolders.remove(Integer.valueOf(pid));
        }
        
        // 获取写锁（指定进程ID）
        public boolean acquireWriteLock(int pid) {
            if (canAcquireWriteLock()) {
                writeLockHolder = pid;
                return true;
            }
            return false;
        }
        
        // 释放写锁（指定进程ID）
        public void releaseWriteLock(int pid) {
            if (writeLockHolder == pid) {
                writeLockHolder = -1;
            }
        }
        
        // 添加等待读锁的进程
        public void addReadWaitingProcess(PCB process) {
            readWaitingQueue.add(process);
        }
        
        // 添加等待写锁的进程
        public void addWriteWaitingProcess(PCB process) {
            writeWaitingQueue.add(process);
        }
        
        // 获取下一个等待写锁的进程
        public PCB getNextWriteWaitingProcess() {
            return writeWaitingQueue.poll();
        }
        
        // 获取所有等待读锁的进程
        public List<PCB> getAllReadWaitingProcesses() {
            List<PCB> processes = new ArrayList<>();
            while (!readWaitingQueue.isEmpty()) {
                processes.add(readWaitingQueue.poll());
            }
            return processes;
        }

        // 检查进程是否持有读锁
        public boolean isReadLockHeldBy(int pid) {
            return readLockHolders.contains(pid);
        }
        
        // 检查进程是否持有写锁
        public boolean isWriteLockHeldBy(int pid) {
            return writeLockHolder == pid;
        }
        
        // 从等待队列中移除指定进程
        public void removeFromWaitingQueues(int pid) {
            // 从读锁等待队列中移除
            Queue<PCB> newReadQueue = new LinkedList<>();
            for (PCB pcb : readWaitingQueue) {
                if (pcb.getPid() != pid) {
                    newReadQueue.add(pcb);
                }
            }
            readWaitingQueue = newReadQueue;
            
            // 从写锁等待队列中移除
            Queue<PCB> newWriteQueue = new LinkedList<>();
            for (PCB pcb : writeWaitingQueue) {
                if (pcb.getPid() != pid) {
                    newWriteQueue.add(pcb);
                }
            }
            writeWaitingQueue = newWriteQueue;
        }
    }
    public List<PCB> getWaitingProcess() {
        List<PCB> processes = new ArrayList<>();
        for (FileLock lock : lockMap.values()) {
            processes.addAll(lock.readWaitingQueue);
            processes.addAll(lock.writeWaitingQueue);
        }
        return processes;
    }
}