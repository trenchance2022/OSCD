package file_disk_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileLockManager {
    private static FileLockManager instance;
    private Map<String, ReadWriteLock> fileLocks;
    // 为每个文件维护一个等待读锁的进程队列
    private Map<String, Queue<PCB>> readWaitingProcesses;
    // 为每个文件维护一个等待写锁的进程队列
    private Map<String, Queue<PCB>> writeWaitingProcesses;
    // 调度器引用
    private Scheduler scheduler;
    
    private FileLockManager() {
        fileLocks = new HashMap<>();
        readWaitingProcesses = new HashMap<>();
        writeWaitingProcesses = new HashMap<>();
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
        readWaitingProcesses.computeIfAbsent(filename, k -> new LinkedList<>()).add(process);
    }
    
    // 添加等待写锁的进程
    public void addWriteWaitingProcess(String filename, PCB process) {
        writeWaitingProcesses.computeIfAbsent(filename, k -> new LinkedList<>()).add(process);
    }
    
    // 获取文件的读锁
    public boolean acquireReadLock(String filename) {
        ReadWriteLock lock = getOrCreateLock(filename);
        try {
            lock.readLock().lock();
            return true;
        } catch (Exception e) {
            System.out.println("获取文件读锁失败: " + filename);
            return false;
        }
    }
    
    // 释放文件的读锁
    public void releaseReadLock(String filename) {
        ReadWriteLock lock = fileLocks.get(filename);
        if (lock != null) {
            lock.readLock().unlock();
            
            // 释放读锁后，检查是否有等待写锁的进程
            notifyWaitingProcesses(filename);
        }
    }
    
    // 获取文件的写锁
    public boolean acquireWriteLock(String filename) {
        ReadWriteLock lock = getOrCreateLock(filename);
        try {
            lock.writeLock().lock();
            return true;
        } catch (Exception e) {
            System.out.println("获取文件写锁失败: " + filename);
            return false;
        }
    }
    
    // 释放文件的写锁
    public void releaseWriteLock(String filename) {
        ReadWriteLock lock = fileLocks.get(filename);
        if (lock != null) {
            lock.writeLock().unlock();
            
            // 释放写锁后，通知所有等待该文件的进程
            notifyWaitingProcesses(filename);
        }
    }
    
    // 通知等待该文件的进程
    private void notifyWaitingProcesses(String filename) {
        if (scheduler == null) return;
        
        // 先处理等待写锁的进程（写操作通常优先级更高）
        Queue<PCB> writeQueue = writeWaitingProcesses.get(filename);
        if (writeQueue != null && !writeQueue.isEmpty()) {
            PCB process = writeQueue.poll();
            process.setState(ProcessState.READY);
            scheduler.addReadyProcess(process);
            System.out.println("文件 " + filename + " 锁释放，唤醒等待写锁的进程 " + process.getPid());
            return; // 一次只唤醒一个写进程
        }
        
        // 如果没有等待写锁的进程，则处理等待读锁的进程（可以同时唤醒多个）
        Queue<PCB> readQueue = readWaitingProcesses.get(filename);
        if (readQueue != null) {
            while (!readQueue.isEmpty()) {
                PCB process = readQueue.poll();
                process.setState(ProcessState.READY);
                scheduler.addReadyProcess(process);
                System.out.println("文件 " + filename + " 锁释放，唤醒等待读锁的进程 " + process.getPid());
            }
        }
    }
    
    private ReadWriteLock getOrCreateLock(String filename) {
        return fileLocks.computeIfAbsent(filename, k -> new ReentrantReadWriteLock());
    }
}