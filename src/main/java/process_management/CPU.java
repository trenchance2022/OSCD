package process_management;

import memory_management.MMU;
import memory_management.MemoryManagement;
import memory_management.MemoryManagementImpl;
import java.util.concurrent.BlockingQueue;
import java.util.Arrays;

public class CPU extends Thread {
    private int cpuId;
    private PCB currentPCB;
    private Scheduler scheduler;
    private DeviceManager deviceManager;
    private BlockingQueue<Interrupt> interruptQueue;
    private volatile boolean running = true;
    private MemoryManagement memoryManager;

    // 有多个CPU，每个CPU都有一个MMU，并且有一个PCB标识当前运行的进程
    private final MMU mmu = new MMU();

    public CPU(int cpuId, Scheduler scheduler, DeviceManager deviceManager, BlockingQueue<Interrupt> interruptQueue) {
        this.cpuId = cpuId;
        this.scheduler = scheduler;
        this.deviceManager = deviceManager;
        this.interruptQueue = interruptQueue;
        this.memoryManager = new MemoryManagementImpl();
    }

    @Override
    public void run() {
        try {
            // 初始状态运行空闲进程
            runIdleProcess();
            
            while (running) {
                // 检查中断
                if (!interruptQueue.isEmpty()) {
                    handleInterrupt(interruptQueue.take());
                }
                
                // 如果当前没有进程运行，请求调度器分配进程
                if (currentPCB == null) {
                    requestProcess();
                }
                
                // 执行当前进程
                if (currentPCB != null) {
                    executeCurrentProcess();
                    Thread.sleep(100); // 短暂休眠，避免CPU占用过高
                } else {
                    Thread.sleep(100); // 空闲时休眠
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeCurrentProcess() {
        if (currentPCB != null) {
            Process process = (Process) currentPCB.getProcess();
            
            // 如果进程还有指令要执行
            if (process.hasNextInstruction()) {
                String instruction = process.getNextInstruction();
                process.executeInstruction(instruction);
                
                // 检查是否还有更多指令
                if (!process.hasNextInstruction()) {
                    // 进程执行完毕
                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 执行完毕");
                    currentPCB.setState(ProcessState.TERMINATED);
                    
                    // 释放PID资源
                    PIDBitmap.getInstance().freePID(currentPCB.getPid());
                    System.out.println("释放进程 " + currentPCB.getPid() + " 的PID资源");
                    
                    runIdleProcess();
                }
            } else {
                // 进程执行完毕
                System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 执行完毕");
                currentPCB.setState(ProcessState.TERMINATED);
                
                // 释放PID资源
                PIDBitmap.getInstance().freePID(currentPCB.getPid());
                System.out.println("释放进程 " + currentPCB.getPid() + " 的PID资源");
                
                runIdleProcess();
            }
        }
    }

    private void handleInterrupt(Interrupt interrupt) {
        switch (interrupt.getType()) {
            case CLOCK:
                handleClockInterrupt();
                break;
            case IO:
                handleIOInterrupt(interrupt.getDeviceId());
                break;
        }
    }

    private void handleClockInterrupt() {
        if (currentPCB != null) {
            // 更新进程已使用的时间片
            currentPCB.incrementTimeUsed();
            System.out.println("CPU-" + cpuId + " 时钟中断：进程 " + currentPCB.getPid() + 
                    " 已使用时间片 " + currentPCB.getTimeUsed() + "/" + currentPCB.getTimeSlice());
            
            // 检查时间片是否用尽
            if (currentPCB.getTimeUsed() >= currentPCB.getTimeSlice()) {
                System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 时间片用尽，进行进程切换");
                
                // 时间片用尽，将进程放回就绪队列
                currentPCB.setState(ProcessState.READY);
                currentPCB.resetTimeUsed();
                
                // 如果不是最低优先级，降低优先级
                if (currentPCB.getPriority() < 3) {
                    currentPCB.setPriority(currentPCB.getPriority() + 1);
                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 优先级降低为 " + currentPCB.getPriority());
                }
                
                scheduler.addReadyProcess(currentPCB);
                
                // CPU进入空闲状态
                runIdleProcess();
            }
        }
    }

    private void handleIOInterrupt(int deviceId) {
        if (currentPCB != null) {
            System.out.println("CPU-" + cpuId + " IO中断：进程 " + currentPCB.getPid() + " 请求设备 " + deviceId);
            
            // 将进程交给设备管理器处理
            deviceManager.requestIO(currentPCB, deviceId);
            
            // CPU进入空闲状态
            runIdleProcess();
        }
    }

    private void requestProcess() {
        PCB process = scheduler.getNextProcess();
        if (process != null) {
            currentPCB = process;
            currentPCB.setState(ProcessState.RUNNING);
            // 更新MMU
            mmu.update(currentPCB);
            System.out.println("CPU-" + cpuId + " 开始执行进程 " + currentPCB.getPid());
        }
    }

    private void runIdleProcess() {
        currentPCB = null;
        System.out.println("CPU-" + cpuId + " 进入空闲状态，运行空闲进程");
        // 通知调度器CPU空闲
        scheduler.notifyCPUIdle(this);
    }

    public void generateIOInterrupt(int deviceId) {
        try {
            interruptQueue.put(new Interrupt(Interrupt.InterruptType.IO, deviceId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getCpuId() {
        return cpuId;
    }

    public boolean isIdle() {
        return currentPCB == null;
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    public PCB getCurrentPCB() {
        return currentPCB;
    }

    public MMU getMMU() {
        return mmu;
    }

    public void changeProcess(PCB currentPCB) {
        mmu.update(currentPCB);
        this.currentPCB = currentPCB;
    }
}
