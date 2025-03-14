package process_management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import main.Constants;

public class Scheduler extends Thread {
    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private List<CPU> cpus; // 管理多个CPU
    private static FileDiskManagement fileSystem = new FileSystemImpl();
    private volatile boolean running = true;
    private ReentrantLock schedulerLock = new ReentrantLock();
    
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
        
        // 将新进程添加到最高优先级的就绪队列
        readyQueues.get(highestPriority).add(pcb);
        System.out.println("创建进程 " + pid + "，执行文件: " + filename + "，优先级为 " + highestPriority);

        // 创建进程后立即尝试调度
        schedule();
        
        return pcb;
    }
    
    public void addReadyProcess(PCB pcb) {
        if (pcb != null && pcb.getState() == ProcessState.READY) {
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
}