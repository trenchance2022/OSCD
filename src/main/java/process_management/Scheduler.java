package process_management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import main.Constants;
import java.util.HashMap;
import java.util.Map;

public class Scheduler extends Thread {
    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private List<CPU> cpus; // 管理多个CPU
    private static FileDiskManagement fileSystem = new FileSystemImpl();
    private volatile boolean running = true;
    private ReentrantLock schedulerLock = new ReentrantLock();

    // 记录进程在低优先级队列中的等待时间
    private Map<Integer, Integer> waitingTimeMap = new HashMap<>();

    // 老化阈值，当进程在低优先级队列中等待超过这个时间时提升优先级
    private static final int AGING_THRESHOLD = 5000; // 5秒

    // 各优先级队列的时间片长度
    private static final int[] TIME_SLICE_BY_PRIORITY = {
        1000,  // 优先级0的时间片为1000ms
        2000,  // 优先级1的时间片为2000ms
        4000,  // 优先级2的时间片为4000ms
        Integer.MAX_VALUE  // 优先级3的时间片无限长（FCFS策略）
    };
    
    // 单例模式
    private static Scheduler instance;
    
    public static synchronized Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    public Scheduler() {
        // 初始化4个优先级队列
        readyQueues = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            readyQueues.add(new LinkedBlockingQueue<>());
        }
        cpus = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            while (running) {
                // 主动检查所有CPU状态并进行调度
                schedule();
                
                // 更新等待时间并处理老化
                updateWaitingTimeAndAging();
                
                Thread.sleep(Constants.CLOCK_INTERRUPT_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 主动调度方法
    private void schedule() {
        schedulerLock.lock();
        try {
            // 检查所有CPU，为空闲CPU分配进程
            for (CPU cpu : cpus) {
                if (cpu.isIdle()) {
                    PCB nextProcess = getNextProcess();
                    if (nextProcess != null) {
                        assignProcessToCPU(nextProcess, cpu);
                    }
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    // 将进程分配给CPU
    private void assignProcessToCPU(PCB pcb, CPU cpu) {
        pcb.setState(ProcessState.RUNNING);
        System.out.println("调度器分配进程 " + pcb.getPid() + " 到 CPU-" + cpu.getCpuId());
        cpu.changeProcess(pcb);
    }
    
    public void addCPU(CPU cpu) {
        cpus.add(cpu);
    }
    
    public PCB createProcess(String filename) {
        // 使用PIDBitmap分配PID
        int pid = PIDBitmap.getInstance().allocatePID();
        
        // 检查是否成功分配PID
        if (pid == PIDBitmap.EMPTY_PID) {
            System.out.println("无法创建新进程：PID资源已耗尽");
            return null;
        }

        String fileContent = fileSystem.readFileData(filename);
        if (fileContent.equals("-1")) {
            System.out.println("无法读取可执行文件: " + filename);
            return null;
        }

        int codeSize = fileContent.length();
        int[] diskAddressBlock = fileSystem.getFileDiskBlock(filename);
        
        // 所有新进程都拥有最高优先级(0)
        int highestPriority = 0;
        PCB pcb = new PCB(pid, codeSize, diskAddressBlock, highestPriority);
        pcb.setState(ProcessState.READY);
        pcb.setExecutedFile(filename);

        // 设置时间片
        pcb.setTimeSlice(TIME_SLICE_BY_PRIORITY[highestPriority]);
        
        // 将新进程添加到最高优先级的就绪队列
        readyQueues.get(highestPriority).add(pcb);
        System.out.println("创建进程 " + pid + "，执行文件: " + filename + "，优先级为 " + highestPriority);

        // 创建进程后立即尝试调度
        schedule();
        
        return pcb;
    }
    
    public void addReadyProcess(PCB pcb) {
        if (pcb != null && pcb.getState() == ProcessState.READY) {
            // 设置时间片
            pcb.setTimeSlice(TIME_SLICE_BY_PRIORITY[pcb.getPriority()]);

            // 重置等待时间
            waitingTimeMap.put(pcb.getPid(), 0);

            readyQueues.get(pcb.getPriority()).add(pcb);
            System.out.println("进程 " + pcb.getPid() + " 加入优先级为 " + pcb.getPriority() + " 的就绪队列");

            // 进程加入就绪队列后立即尝试调度
            schedule();
        }
    }
    
    public PCB getNextProcess() {
        // 从高优先级队列开始查找就绪进程
        for (BlockingQueue<PCB> queue : readyQueues) {
            if (!queue.isEmpty()) {
                PCB pcb = queue.poll();
                System.out.println("调度器选择进程 " + pcb.getPid() + "，优先级为 " + pcb.getPriority());
                return pcb;
            }
        }
        return null;
    }

    // 更新等待时间并处理老化
    private void updateWaitingTimeAndAging() {
        schedulerLock.lock();
        try {
            // 从优先级1开始（跳过最高优先级队列）
            for (int priority = 1; priority < readyQueues.size(); priority++) {
                BlockingQueue<PCB> queue = readyQueues.get(priority);
                List<PCB> tempList = new ArrayList<>();
                
                // 将队列中的进程取出来处理
                while (!queue.isEmpty()) {
                    PCB pcb = queue.poll();
                    int pid = pcb.getPid();
                    
                    // 更新等待时间
                    int waitingTime = waitingTimeMap.getOrDefault(pid, 0) + Constants.CLOCK_INTERRUPT_INTERVAL_MS;
                    waitingTimeMap.put(pid, waitingTime);
                    
                    // 检查是否需要提升优先级
                    if (waitingTime >= AGING_THRESHOLD) {
                        // 提升优先级
                        int newPriority = Math.max(0, priority - 1);
                        pcb.setPriority(newPriority);
                        System.out.println("进程 " + pid + " 因长时间等待被提升优先级到 " + newPriority);
                        
                        // 重置等待时间
                        waitingTimeMap.put(pid, 0);
                        
                        // 将进程加入新的优先级队列
                        readyQueues.get(newPriority).add(pcb);
                    } else {
                        // 不需要提升优先级，放回临时列表
                        tempList.add(pcb);
                    }
                }
                
                // 将未提升优先级的进程放回原队列
                for (PCB pcb : tempList) {
                    queue.add(pcb);
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    // 当进程结束时，清理相关资源
    public void terminateProcess(PCB pcb) {
        if (pcb != null) {
            schedulerLock.lock();
            try {
                System.out.println("调度器: 终止进程 " + pcb.getPid());
                
                // 从等待时间映射中移除
                waitingTimeMap.remove(pcb.getPid());
                
                // 释放页表和内存资源
                memory_management.PageTableArea.getInstance().removePageTable(pcb.getPid());
                
                // 释放PID
                PIDBitmap.getInstance().freePID(pcb.getPid());
                
                // 如果进程持有任何文件锁，释放它们
                file_disk_management.FileLockManager.getInstance().releaseAllLocks(pcb.getPid());
                
                System.out.println("进程 " + pcb.getPid() + " 已终止，所有资源已释放");
            } finally {
                schedulerLock.unlock();
            }
        }
    }
}