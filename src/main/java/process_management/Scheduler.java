package process_management;

import main.Constants;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static main.Main.fileSystem;

public class Scheduler extends Thread {




    public enum SchedulingPolicy { FCFS, SJF, RR, PRIORITY, MLFQ ,PRIORITY_Preemptive}
    private static Scheduler instance;
    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private List<CPU> cpus; // 管理多个 CPU
    private SchedulingPolicy currentPolicy; // 当前调度策略
    private final ReentrantLock lock = new ReentrantLock(); // 用于线程安全
    private final int[] timeSlices = new int[4]; // 各队列的时间片长度
    private final Map<Integer, Integer> waitingTimeMap = new HashMap<>(); // 进程等待时间记录
    private static final int AGING_THRESHOLD = 5000; // 老化阈值（5秒）


    public int getTimeSlice(int queueIndex) {
        return timeSlices[queueIndex];
    }

    // 单例模式
    public static synchronized Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    private Scheduler() {
        readyQueues = new ArrayList<>();
        cpus = new ArrayList<>();
        currentPolicy = SchedulingPolicy.FCFS; // 默认策略
    }

    public PCB createProcess(String filename, int priority) {
        // 分配 PID
        int pid = PIDBitmap.getInstance().allocatePID();
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

        // 初始化 PCB
        PCB pcb = new PCB(pid, codeSize, diskAddressBlock, priority);
        pcb.setState(ProcessState.READY);
        pcb.setExecutedFile(filename);
        //pcb.setPc();

        // 设置时间片
        pcb.setTimeSlice(timeSlices[pcb.getCurrentQueue()]);

        // 将新进程加入就绪队列
        addReadyProcess(pcb);
        System.out.println("创建进程 " + pid + "，执行文件: " + filename + "，优先级为 " + priority);

        return pcb;
    }


    // 配置调度策略
    public void configure(SchedulingPolicy policy) {
        lock.lock();
        try {
            this.currentPolicy = policy;
            readyQueues.clear();

            switch (policy) {
                case FCFS -> {
                    readyQueues.add(new LinkedBlockingQueue<>());
                    timeSlices[0] = Integer.MAX_VALUE;
                }
                case SJF -> {
                    readyQueues.add(new PriorityBlockingQueue<>(11, Comparator.comparingInt(PCB::getCodeSize)
                    ));
                    timeSlices[0] = Integer.MAX_VALUE;
                }
                case RR -> {
                    readyQueues.add(new LinkedBlockingQueue<>());
                    timeSlices[0] = 100;
                }
                case PRIORITY -> {
                    readyQueues.add(new PriorityBlockingQueue<>(11, Comparator.comparingInt(PCB::getPriority)
                    ));
                    timeSlices[0] = Integer.MAX_VALUE;
                }
                case MLFQ -> {
                    readyQueues.add(new LinkedBlockingQueue<>());
                    readyQueues.add(new LinkedBlockingQueue<>());
                    readyQueues.add(new LinkedBlockingQueue<>());
                    readyQueues.add(new LinkedBlockingQueue<>());
                    timeSlices[0] = 100;
                    timeSlices[1] = 200;
                    timeSlices[2] = 400;
                    timeSlices[3] = 800;
                }
                case PRIORITY_Preemptive -> {
                    readyQueues.add(new PriorityBlockingQueue<>(11, Comparator.comparingInt(PCB::getPriority)
                    ));
                    timeSlices[0] = Integer.MAX_VALUE;
                }
            }
            System.out.println("调度策略已设置为: " + policy);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 检查所有CPU，为空闲CPU分配进程
                schedule();

                // 如果是MLFQ策略，更新等待时间并处理老化
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
        lock.lock();
        try {
            for (CPU cpu : cpus) {
                if (cpu.isIdle()) {
                    PCB nextProcess = getNextProcess();
                    if (nextProcess != null) {
                        assignProcessToCPU(nextProcess, cpu);
                        nextProcess.addPCB();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // 将进程分配给CPU
    private void assignProcessToCPU(PCB pcb, CPU cpu) {
        pcb.setState(ProcessState.RUNNING);
        System.out.println("调度器分配进程 " + pcb.getPid() + " 到 CPU-" + cpu.getCpuId());
        cpu.changeProcess(pcb);
    }


    // 获取下一个要执行的进程
    public PCB getNextProcess() {
        lock.lock();
        try {
            if (currentPolicy == SchedulingPolicy.MLFQ) {
                // 按优先级从高到低检查队列
                for (int i = 0; i < 4; i++) {
                    if (!readyQueues.get(i).isEmpty()) {
                        PCB pcb = readyQueues.get(i).poll();
                        pcb.setCurrentQueue(i); // 记录当前所在队列
                        return pcb;
                    }
                }
            } else {
                // 其他策略处理
                return readyQueues.get(0).poll();
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    // 更新等待时间并处理老化
    private void updateWaitingTimeAndAging() {
        lock.lock();
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
            lock.unlock();
        }
    }

    // 添加 CPU 到调度器
    public void addCPU(CPU cpu) {
        cpus.add(cpu);
        System.out.println("CPU-" + cpu.getCpuId() + " 已注册到调度器");
    }

    // 将进程加入就绪队列
    public void addReadyProcess(PCB pcb) {
        lock.lock();
        try {
            if (pcb != null && pcb.getState() == ProcessState.READY) {
                // 设置时间片
                pcb.setTimeSlice(timeSlices[pcb.getCurrentQueue()]);

                // 重置等待时间
                waitingTimeMap.put(pcb.getPid(), 0);

                readyQueues.get(pcb.getCurrentQueue()).add(pcb);
                System.out.println("进程 " + pcb.getPid() + " 加入优先级为 " + pcb.getCurrentQueue() + " 的就绪队列");

                // 进程加入就绪队列后立即尝试调度
                schedule();
            }
        } finally {
            lock.unlock();
        }
    }

    // 终止进程
    public void terminateProcess(PCB pcb) {
        lock.lock();
        try {
            if (pcb != null) {
                System.out.println("调度器: 终止进程 " + pcb.getPid());

                // 释放资源
                memory_management.PageTableArea.getInstance().removePageTable(pcb.getPid());
                PIDBitmap.getInstance().freePID(pcb.getPid());
                file_disk_management.FileLockManager.getInstance().releaseAllLocks(pcb.getPid());

                // 从活跃PCB列表中移除
                pcb.removePCB();

                System.out.println("进程 " + pcb.getPid() + " 已终止，所有资源已释放");
            }else{
                System.out.println("进程不存在");
            }
        } finally {
            lock.unlock();
        }
    }


    public SchedulingPolicy getCurrentPolicy(){
        return currentPolicy;
    }
    public void putPCBback(PCB pcb){
        readyQueues.get(pcb.getCurrentQueue()).add(pcb);
    }

}
