package process_management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Scheduler {
    private List<BlockingQueue<PCB>> readyQueues; // 多级反馈队列
    private List<CPU> cpus; // 管理多个CPU
    
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
    
    public void addCPU(CPU cpu) {
        cpus.add(cpu);
    }
    
    public PCB createProcess(String executablePath) {
        // 使用PIDBitmap分配PID
        int pid = PIDBitmap.getInstance().allocatePID();
        
        // 检查是否成功分配PID
        if (pid == PIDBitmap.EMPTY_PID) {
            System.out.println("无法创建新进程：PID资源已耗尽");
            return null;
        }
        
        // 创建Process对象
        Process process = new Process(executablePath);
        process.setPid(pid);
        
        // 所有新进程都拥有最高优先级(0)
        int highestPriority = 0;
        PCB pcb = new PCB(pid, highestPriority, process);
        pcb.setState(ProcessState.READY);
        pcb.setExecutablePath(executablePath);
        
        // 将新进程添加到最高优先级的就绪队列
        readyQueues.get(highestPriority).add(pcb);
        System.out.println("创建进程 " + pid + "，执行文件: " + executablePath + "，优先级为 " + highestPriority);
        
        // 检查是否有空闲CPU可以立即执行该进程
        for (CPU cpu : cpus) {
            if (cpu.isIdle()) {
                notifyCPUIdle(cpu);
                break;
            }
        }
        
        return pcb;
    }
    
    public void addReadyProcess(PCB pcb) {
        if (pcb != null && pcb.getState() == ProcessState.READY) {
            readyQueues.get(pcb.getPriority()).add(pcb);
            System.out.println("进程 " + pcb.getPid() + " 加入优先级为 " + pcb.getPriority() + " 的就绪队列");
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
    
    public void notifyCPUIdle(CPU cpu) {
        // CPU空闲时，尝试分配新进程
        PCB nextProcess = getNextProcess();
        if (nextProcess != null) {
            nextProcess.setState(ProcessState.RUNNING);
            System.out.println("CPU-" + cpu.getCpuId() + " 空闲，调度器分配进程 " + nextProcess.getPid());
            // 将进程分配给CPU
            cpu.changeProcess(nextProcess);
        }
    }
}