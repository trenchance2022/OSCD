package process_management;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import main.Constants;

public class Scheduler extends Thread {
    // 调度策略枚举
    public enum SchedulingPolicy {
        FCFS,  // 先来先服务
        SJF,   // 最短作业优先
        RR,    // 时间片轮转
        PRIORITY, // 优先级调度
        MLFQ   // 多级反馈队列
    }

    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private List<CPU> cpus; // 管理多个 CPU
    private static FileDiskManagement fileSystem = new FileSystemImpl();
    private volatile boolean running = true;
    private ReentrantLock schedulerLock = new ReentrantLock(); // 使用 ReentrantLock

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

    // 当前系统使用的调度策略
    private static SchedulingPolicy currentPolicy = SchedulingPolicy.MLFQ;

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

                // 如果是MLFQ策略，才需要更新等待时间和处理老化
                if (currentPolicy == SchedulingPolicy.MLFQ) {
                    updateWaitingTimeAndAging();
                }

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

    // 设置系统调度策略
    public void setSchedulingPolicy(SchedulingPolicy policy) {
        currentPolicy = policy;
        System.out.println("系统调度策略设置为: " + policy);
    }

    // 获取下一个要执行的进程
    public PCB getNextProcess() {
        schedulerLock.lock();
        try {
            if (currentPolicy == SchedulingPolicy.MLFQ) {
                // MLFQ策略特殊处理：从高优先级队列开始查找就绪进程
                for (int i = 0; i < readyQueues.size(); i++) {
                    BlockingQueue<PCB> queue = readyQueues.get(i);
                    if (!queue.isEmpty()) {
                        // 最低优先级队列使用FCFS
                        if (i == readyQueues.size() - 1) {
                            return queue.poll();
                        }
                        // 其他队列使用时间片轮转
                        return queue.poll();
                    }
                }
            } else {
                // 其他策略：将所有就绪进程合并到一个队列中进行调度
                List<PCB> allProcesses = new ArrayList<>();
                for (BlockingQueue<PCB> queue : readyQueues) {
                    allProcesses.addAll(queue);
                    queue.clear();
                }
                
                if (!allProcesses.isEmpty()) {
                    PCB selectedPCB = selectProcessFromQueue(allProcesses, currentPolicy);
                    if (selectedPCB != null) {
                        return selectedPCB;
                    }
                }
            }
            return null;
        } finally {
            schedulerLock.unlock();
        }
    }

    // 根据调度策略从进程列表中选择进程
    private PCB selectProcessFromQueue(List<PCB> processes, SchedulingPolicy policy) {
        if (processes.isEmpty()) {
            return null;
        }

        PCB selectedPCB = null;
        switch (policy) {
            case FCFS:
                selectedPCB = processes.get(0);
                break;
            case SJF:
                selectedPCB = processes.stream()
                    .min(Comparator.comparingInt(p -> p.getTimeSlice() - p.getTimeUsed()))
                    .orElse(null);
                break;
            case RR:
                selectedPCB = processes.get(0);
                break;
            case PRIORITY:
                selectedPCB = processes.stream()
                    .min(Comparator.comparingInt(PCB::getPriority))
                    .orElse(null);
                break;
            default:
                selectedPCB = processes.get(0);
        }

        if (selectedPCB != null) {
            processes.remove(selectedPCB);
        }
        return selectedPCB;
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

                // 从活跃PCB列表中移除
                pcb.removePCB();

                System.out.println("进程 " + pcb.getPid() + " 已终止，所有资源已释放");
            } finally {
                schedulerLock.unlock();
            }
        }
    }

    // 添加 CPU 到调度器
    public void addCPU(CPU cpu) {
        cpus.add(cpu);
        System.out.println("CPU-" + cpu.getCpuId() + " 已注册到调度器");
    }

    // 创建新进程
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

        // 将新进程添加到活跃PCB列表中
        pcb.addPCB();

        // 创建进程后立即尝试调度
        schedule();

        return pcb;
    }

    // 将进程加入就绪队列
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
}