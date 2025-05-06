package org.example.oscdspring.process_management;

import org.example.oscdspring.file_disk_management.FileLockManager;
import static org.example.oscdspring.main.Library.getMemoryManagement;
import org.example.oscdspring.main.Constants;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.example.oscdspring.main.Library;
import org.example.oscdspring.memory_management.Memory;
import org.example.oscdspring.memory_management.PageTable;
import org.example.oscdspring.memory_management.PageTableArea;
import org.example.oscdspring.memory_management.PageTableEntry;
import org.example.oscdspring.util.LogEmitterService;
import org.springframework.stereotype.Component;

import static org.example.oscdspring.main.Library.getFileSystem;

@Component
public class Scheduler extends Thread {

    public enum SchedulingPolicy { FCFS, SJF, RR, PRIORITY, MLFQ ,PRIORITY_Preemptive}
    private static Scheduler instance;
    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private BlockingQueue<PCB> waitingQueue;
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
        waitingQueue = new LinkedBlockingQueue<>();
        readyQueues = new ArrayList<>();
        cpus = new ArrayList<>();
        currentPolicy = SchedulingPolicy.FCFS; // 默认策略
    }

    public PCB createProcess(String filename, int priority) {
        // 分配 PID
        int pid = PIDBitmap.getInstance().allocatePID();
        if (pid == PIDBitmap.EMPTY_PID) {
            LogEmitterService.getInstance().sendLog("无法创建新进程：PID资源已耗尽");
            return null;
        }


        String fileContent = getFileSystem().readFileData(filename);
        if (fileContent.equals("-1")) {
            LogEmitterService.getInstance().sendLog("无法读取可执行文件: " + filename);
            return null;
        }

        int codeSize = fileContent.length();
        int[] diskAddressBlock = getFileSystem().getFileDiskBlock(filename);

        // 初始化 PCB
        PCB pcb = new PCB(pid, codeSize, diskAddressBlock, priority);
        pcb.setState(ProcessState.READY);
        pcb.setExecutedFile(filename);
        //pcb.setPc();

        // 设置时间片
        pcb.setTimeSlice(timeSlices[pcb.getCurrentQueue()]);

        // 将新进程加入就绪队列
        addReadyProcess(pcb);
        LogEmitterService.getInstance().sendLog("创建进程 " + pid + "，执行文件: " + filename + "，优先级为 " + priority);

        pcb.addPCB_all();
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
                    timeSlices[0] = 500;
                    timeSlices[1] = 1000;
                    timeSlices[2] = 2000;
                    timeSlices[3] = 4000;
                }
                case PRIORITY_Preemptive -> {
                    readyQueues.add(new PriorityBlockingQueue<>(11, Comparator.comparingInt(PCB::getPriority)
                    ));
                    timeSlices[0] = Integer.MAX_VALUE;
                }
            }
            LogEmitterService.getInstance().sendLog("调度策略已设置为: " + policy);
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
        LogEmitterService.getInstance().sendLog("调度器分配进程 " + pcb.getPid() + " 到 CPU-" + cpu.getCpuId());
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
                        while(pcb.getState() != ProcessState.READY){
                            pcb = readyQueues.get(i).poll();
                            if(pcb == null){
                                break;
                            }
                        }
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
                        LogEmitterService.getInstance().sendLog("进程 " + pid + " 因长时间等待被提升优先级到 " + newPriority);

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
        LogEmitterService.getInstance().sendLog("CPU-" + cpu.getCpuId() + " 已注册到调度器");
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
                LogEmitterService.getInstance().sendLog("进程 " + pcb.getPid() + " 加入优先级为 " + pcb.getCurrentQueue() + " 的就绪队列");

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
                LogEmitterService.getInstance().sendLog("调度器: 终止进程 " + pcb.getPid());

                // 释放资源

                if(pcb.getState() == ProcessState.RUNNING) {
                    Library.getMemoryManagement().releaseProcess(pcb);
                    PIDBitmap.getInstance().freePID(pcb.getPid());
                    org.example.oscdspring.file_disk_management.FileLockManager.getInstance().releaseAllLocks(pcb.getPid());
                    pcb.setState(ProcessState.TERMINATED);
                    pcb.setTimeRemain(0);
                    pcb.setRemainInstruction("");
                }else if(pcb.getState()==ProcessState.READY){
                    //org.example.oscdspring.memory_management.PageTableArea.getInstance().removePageTable(pcb.getPid());
                    Library.getMemoryManagement().releaseProcess(pcb);
                    PIDBitmap.getInstance().freePID(pcb.getPid());
                    org.example.oscdspring.file_disk_management.FileLockManager.getInstance().releaseAllLocks(pcb.getPid());
                    pcb.setState(ProcessState.TERMINATED);
                    for(BlockingQueue<PCB> queue:readyQueues){
                        if(queue.contains(pcb)){
                            readyQueues.get(pcb.getCurrentQueue()).remove(pcb);
                            break;
                        }
                    }
                }else if(pcb.getState()==ProcessState.WAITING){
                    Library.getMemoryManagement().releaseProcess(pcb);
                    PIDBitmap.getInstance().freePID(pcb.getPid());
                    org.example.oscdspring.file_disk_management.FileLockManager.getInstance().releaseAllLocks(pcb.getPid());
                    pcb.setState(ProcessState.TERMINATED);
                    //正在进行设备io的会自动停止,队列中的会自动清除
                    //正在读写的会自动停止，请求锁队列会自动清除
                }
                pcb.removePCB_all();
                pcb.removePCB();
                LogEmitterService.getInstance().sendLog("进程 " + pcb.getPid() + " 已终止，所有资源已释放");

            }else{
                LogEmitterService.getInstance().sendLog("进程不存在");
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

    public void addWaitingProcess(PCB pcb) {
        // 将进程加入等待队列
        if (pcb != null && pcb.getState() == ProcessState.WAITING) {
            waitingQueue.add(pcb);
        }
    }
    public void removeWaitingProcess(PCB pcb) {
        // 从等待队列中移除进程
        if (pcb != null ) {
            waitingQueue.remove(pcb);
        }
    }

    public Object getRunningProcess() {
        // 遍历所有 CPU，找到正在运行的进程,并返回正在运行进程名字的数组
        List<String> runningProcessNames = new ArrayList<>();
        for (CPU cpu : cpus) {
            PCB runningProcess = cpu.getCurrentPCB();
            if (runningProcess != null&&runningProcess.getState()==ProcessState.RUNNING) {
                //将名字加入到字符串数组内
                runningProcessNames.add(runningProcess.getExecutedFile()+"(pid:"+runningProcess.getPid()+")");
            }
        }
        return runningProcessNames;
    }

    public Object getReadyProcess() {
        // 遍历所有就绪队列，找到所有就绪的进程，并返回就绪进程名字的数组
        List<String> readyProcessNames = new ArrayList<>();
        for (BlockingQueue<PCB> queue : readyQueues) {
            for (PCB pcb : queue) {
                if(pcb.getState()==ProcessState.READY){
                    //将名字加入到字符串数组内
                readyProcessNames.add(pcb.getExecutedFile()+"(pid:"+pcb.getPid()+")");
            }}
        }
        return readyProcessNames;
    }
    public Object getWaitingProcess() {
        // 遍历所有就绪队列，找到所有就绪的进程，并返回就绪进程名字的数组
        List<String> waitingProcessNames = new ArrayList<>();
        for (PCB pcb : waitingQueue) {
            waitingProcessNames.add(pcb.getExecutedFile()+"(pid:"+pcb.getPid()+")");
        }
        for(PCB pcb: FileLockManager.getInstance().getWaitingProcess()){
            waitingProcessNames.add(pcb.getExecutedFile()+"(pid:"+pcb.getPid()+")");
        }
        return waitingProcessNames;
    }

    public List<CPU> getCpus() {
        return cpus;
    }
    public void addReadyProcess_test(PCB pcb) {
        lock.lock();
        try {
            if (pcb != null && pcb.getState() == ProcessState.READY) {
                readyQueues.get(pcb.getCurrentQueue()).add(pcb);
            }
        } finally {
            lock.unlock();
        }
    }

    public int[] getTimeSlices() {
        return timeSlices;
    }
}
