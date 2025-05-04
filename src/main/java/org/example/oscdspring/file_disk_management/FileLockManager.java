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
        
        lock.notifyWaitingProcesses(scheduler);
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
            lock.removeFromWaitingQueue(pid);
        }
        
        LogEmitterService.getInstance().sendLog("进程 " + pid + " 的所有文件锁已释放");
    }
    
    private FileLock getOrCreateLock(String filename) {
        return lockMap.computeIfAbsent(filename, k -> new FileLock(filename));
    }
    
    /**
     * 自定义文件锁实现
     */
    private class FileLock {
        private String filename;
        private List<Integer> readLockHolders; // 持有读锁的进程ID列表
        private int writeLockHolder; // 持有写锁的进程ID，-1表示没有进程持有写锁
        
        // 等待进程对象，包含进程和请求类型
        private static class WaitingProcess {
            PCB process;
            boolean isRead; // true表示读操作，false表示写操作
            
            public WaitingProcess(PCB process, boolean isRead) {
                this.process = process;
                this.isRead = isRead;
            }
        }
        
        private Queue<WaitingProcess> waitingQueue; // 统一的等待队列
        
        public FileLock(String filename) {
            this.filename = filename;
            readLockHolders = new ArrayList<>();
            writeLockHolder = -1;
            waitingQueue = new LinkedList<>();
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(100); // 每100毫秒检查一次
                        Iterator<WaitingProcess> iterator = waitingQueue.iterator();
                        while (iterator.hasNext()) {
                            WaitingProcess wp = iterator.next();
                            if (wp.process.getState() == ProcessState.TERMINATED) {
                                iterator.remove();
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
            waitingQueue.add(new WaitingProcess(process, true));
        }
        
        // 添加等待写锁的进程
        public void addWriteWaitingProcess(PCB process) {
            waitingQueue.add(new WaitingProcess(process, false));
        }
        
        // 通知等待进程
        public void notifyWaitingProcesses(Scheduler scheduler) {
            if (waitingQueue.isEmpty()) return;
            
            // 如果队首是写进程，且可以获取写锁，则唤醒该进程
            if (!waitingQueue.isEmpty()) {
                WaitingProcess first = waitingQueue.peek();
                
                if (!first.isRead) { // 写操作进程
                    if (canAcquireWriteLock()) {
                        WaitingProcess wp = waitingQueue.poll();
                        PCB process = wp.process;
                        process.setState(ProcessState.READY);
                        scheduler.addReadyProcess(process);
                        LogEmitterService.getInstance().sendLog("文件 " + filename + " 锁释放，唤醒等待写锁的进程 " + process.getPid());
                    }
                } else { // 读操作进程
                    if (canAcquireReadLock()) {
                        // 唤醒队首及后续连续的读操作进程
                        List<PCB> readProcessesToWake = new ArrayList<>();
                        
                        while (!waitingQueue.isEmpty() && waitingQueue.peek().isRead) {
                            WaitingProcess wp = waitingQueue.poll();
                            readProcessesToWake.add(wp.process);
                        }
                        
                        for (PCB process : readProcessesToWake) {
                            process.setState(ProcessState.READY);
                            scheduler.addReadyProcess(process);
                            LogEmitterService.getInstance().sendLog("文件 " + filename + " 锁释放，唤醒等待读锁的进程 " + process.getPid());
                        }
                    }
                }
            }
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
        public void removeFromWaitingQueue(int pid) {
            Queue<WaitingProcess> newQueue = new LinkedList<>();
            for (WaitingProcess wp : waitingQueue) {
                if (wp.process.getPid() != pid) {
                    newQueue.add(wp);
                }
            }
            waitingQueue = newQueue;
        }
    }
    
    public List<PCB> getWaitingProcess() {
        List<PCB> processes = new ArrayList<>();
        for (FileLock lock : lockMap.values()) {
            for (FileLock.WaitingProcess wp : lock.waitingQueue) {
                processes.add(wp.process);
            }
        }
        return processes;
    }
}