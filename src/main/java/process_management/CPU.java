package process_management;

import device_management.DeviceManager;
import interrupt_management.Interrupt;
import interrupt_management.InterruptRequestLine;
import memory_management.MMU;
import memory_management.MemoryManagement;
import java.util.concurrent.BlockingQueue;
import main.Constants;

public class CPU extends Thread {
    private int cpuId;
    private PCB currentPCB;
    private Scheduler scheduler;
    private DeviceManager deviceManager;
    private BlockingQueue<Interrupt> interruptQueue;
    private MemoryManagement memoryManagement = new memory_management.MemoryManagementImpl();

    // 有多个CPU，每个CPU都有一个MMU，并且有一个PCB标识当前运行的进程
    private final MMU mmu = new MMU();

    public CPU(int cpuId, Scheduler scheduler, DeviceManager deviceManager, BlockingQueue<Interrupt> interruptQueue) {
        this.cpuId = cpuId;
        this.scheduler = scheduler;
        this.deviceManager = deviceManager;
        // 通过中断请求线注册CPU并获取中断队列
        this.interruptQueue = InterruptRequestLine.getInstance().registerCPU(cpuId);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 检查中断队列，但只处理一个时钟中断
                boolean clockInterruptHandled = false;
                while (!interruptQueue.isEmpty()) {
                    Interrupt interrupt = interruptQueue.peek();
                    
                    // 如果是时钟中断且已经处理过一个时钟中断，则跳过
                    if (interrupt.getType() == Interrupt.InterruptType.CLOCK && clockInterruptHandled) {
                        interruptQueue.take(); // 移除但不处理
                        continue;
                    }
                    
                    // 处理中断
                    handleInterrupt(interruptQueue.take());
                    
                    // 如果处理的是时钟中断，标记为已处理
                    if (interrupt.getType() == Interrupt.InterruptType.CLOCK) {
                        clockInterruptHandled = true;
                    }
                }
                
                // 执行当前进程
                if (currentPCB != null) {
                    execute();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void execute() {
        String instruction = InstructionFetch();
        if (instruction != null) {
            executeInstruction(instruction);
        }
    }

    private String InstructionFetch() {
        // 获取当前进程的PC值（程序计数器）
        int pc = currentPCB.getPc();
        
        // 读取指令
        StringBuilder instruction = new StringBuilder();
        byte[] buffer = new byte[1];
        int currentAddress = pc;

        // 逐字节读取，直到遇到指令结束符"#"
        while (true) {
            if (!memoryManagement.Read(this, currentAddress, buffer, 1)) {
                System.out.println("CPU-" + cpuId + " 读取内存失败，地址: " + currentAddress);
                return null;
            }
            
            char c = (char) buffer[0];
            currentAddress++;
            
            if (c == '#') {
                break;
            }
            
            instruction.append(c);
            
            // 防止无限循环，设置最大指令长度
            if (instruction.length() > 100) {
                System.out.println("CPU-" + cpuId + " 指令过长，可能缺少结束符");
                return null;
            }
        }

        // 更新PC值，指向下一条指令
        currentPCB.setPc(currentAddress);

        return instruction.toString();
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

    // 解析并执行指令
    private void executeInstruction(String instruction) {
        System.out.println("CPU-" + cpuId + " 执行指令: " + instruction);
        
        // 将指令按空格分割成操作码和操作数
        String[] parts = instruction.split("\\s+");
        if (parts.length == 0) {
            System.out.println("CPU-" + cpuId + " 空指令");
            return;
        }
        
        String opcode = parts[0].toUpperCase();
        
        try{
            // 根据操作码执行相应操作
            switch (opcode) {
                case "C":
                    int computeTime = Integer.parseInt(parts[1]);
                    Thread.sleep(computeTime);
                    currentPCB.incrementTimeUsed(computeTime);
                    break;
                case "R":
                    String filename = parts[1];
                    int readtime = Integer.parseInt(parts[2]);
                    
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println("CPU-" + cpuId + " 指令解析错误: " + instruction);
        }
    }

    private void handleClockInterrupt() {
        if (currentPCB != null) {
            // 更新进程已使用的时间片
            currentPCB.incrementTimeUsed(Constants.CLOCK_INTERRUPT_INTERVAL_MS);
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

                // 立即请求新进程执行
                PCB nextProcess = scheduler.getNextProcess();
                if (nextProcess != null) {
                    nextProcess.setState(ProcessState.RUNNING);
                    changeProcess(nextProcess);
                } else {
                    // 没有可用进程，CPU进入空闲状态
                    currentPCB = null;
                    System.out.println("CPU-" + cpuId + " 进入空闲状态");
                }
            }
        }
    }

    private void handleIOInterrupt(int deviceId) {
        if (currentPCB != null) {
            System.out.println("CPU-" + cpuId + " IO中断：进程 " + currentPCB.getPid() + " 请求设备 " + deviceId);
            
            // 将进程交给设备管理器处理
            deviceManager.requestIO(currentPCB, deviceId);
        }
    }

    public void generateIOInterrupt(int deviceId) {
        InterruptRequestLine.getInstance().sendInterrupt(
            cpuId, new Interrupt(Interrupt.InterruptType.IO, deviceId));
    }

    public int getCpuId() {
        return cpuId;
    }

    public boolean isIdle() {
        return currentPCB == null;
    }

    public PCB getCurrentPCB() {
        return currentPCB;
    }

    public MMU getMMU() {
        return mmu;
    }

    public void changeProcess(PCB newPCB) {
        if (newPCB != null) {
            mmu.update(newPCB);
            this.currentPCB = newPCB;
            System.out.println("CPU-" + cpuId + " 开始执行进程 " + currentPCB.getPid());
        }
    }
}
