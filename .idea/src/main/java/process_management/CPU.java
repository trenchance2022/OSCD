package process_management;

import device_management.DeviceManager;
import interrupt_management.Interrupt;
import interrupt_management.InterruptRequestLine;
import memory_management.MMU;
import memory_management.MemoryManagement;
import java.util.concurrent.BlockingQueue;
import main.Constants;
import java.util.Random;

public class CPU extends Thread {
    private int cpuId;
    private PCB currentPCB;
    private Scheduler scheduler;
    private DeviceManager deviceManager;
    private BlockingQueue<Interrupt> interruptQueue;
    private MemoryManagement memoryManagement = new memory_management.MemoryManagementImpl();

    // 有多个CPU，每个CPU都有一个MMU，并且有一个PCB标识当前运行的进程
    private final MMU mmu = new MMU();

    public CPU(int cpuId, Scheduler scheduler, DeviceManager deviceManager, InterruptRequestLine interruptQueue) {
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
                    // 本程序指令为原子操作，指令结束后可能积累多个时钟中断，若积累，则统一视作一个时钟中断
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
                else {
                    Thread.sleep(100); // 空闲时等待
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
        // 如果有剩余指令未执行完，则继续执行剩余指令
        if (!currentPCB.getRemainInstruction().isEmpty()) {
            return currentPCB.getRemainInstruction();
        }

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
                    // 如果是从中断恢复的指令，使用保存的剩余时间
                    if (!currentPCB.getRemainInstruction().isEmpty()) {
                        computeTime = Integer.parseInt(currentPCB.getRemainInstruction().split("\\s+")[1]);
                        currentPCB.setRemainInstruction("");
                    }
                    
                    // 计算本次能执行的时间
                    int remainingTimeSlice = currentPCB.getTimeSlice() - currentPCB.getTimeUsed();
                    int executeTime = Math.min(computeTime, remainingTimeSlice);
                    
                    Thread.sleep(executeTime);
                    currentPCB.incrementTimeUsed(executeTime);
                    
                    // 如果还有剩余时间未执行完，保存剩余时间
                    if (executeTime < computeTime) {
                        currentPCB.setRemainInstruction("C " + (computeTime - executeTime));
                        // 将PC值回退，使下次执行时重新执行该指令
                        int currentPC = currentPCB.getPc();
                        int instructionLength = instruction.length() + 1;
                        currentPCB.setPc(currentPC - instructionLength);
                    }
                    break;
                case "R":
                    String filename = parts[1];
                    int readtime = Integer.parseInt(parts[2]);
                    
                    // 尝试获取文件读锁
                    if (file_disk_management.FileLockManager.getInstance().acquireReadLock(filename, currentPCB.getPid())) {
                        // 将进程设置为阻塞状态
                        currentPCB.setState(ProcessState.WAITING);
                        
                        // 创建一个新线程来处理文件读取
                        new Thread(() -> {
                            try {
                                Thread.sleep(readtime);
                                // 读取完成后，释放锁并将进程加入就绪队列
                                file_disk_management.FileLockManager.getInstance().releaseReadLock(filename, currentPCB.getPid());
                                currentPCB.setState(ProcessState.READY);
                                scheduler.addReadyProcess(currentPCB);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                        
                        // 请求新进程执行
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            currentPCB = null;
                        }
                    } else {
                        // 无法获取锁，将进程加入等待队列
                        currentPCB.setState(ProcessState.WAITING);
                        file_disk_management.FileLockManager.getInstance().addReadWaitingProcess(filename, currentPCB);
                        
                        // 将PC值回退
                        int currentPC = currentPCB.getPc();
                        int instructionLength = instruction.length() + 1;
                        currentPCB.setPc(currentPC - instructionLength);
                        
                        // 请求新进程执行
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            currentPCB = null;
                        }
                    }
                    break;
                case "W":
                    filename = parts[1];
                    int writeTime = Integer.parseInt(parts[2]);
                    
                    // 尝试获取文件写锁
                    if (file_disk_management.FileLockManager.getInstance().acquireWriteLock(filename, currentPCB.getPid())) {
                        // 将进程设置为阻塞状态
                        currentPCB.setState(ProcessState.WAITING);
                        
                        // 创建一个新线程来处理文件写入
                        new Thread(() -> {
                            try {
                                Thread.sleep(writeTime);
                                // 写入完成后，释放锁并将进程加入就绪队列
                                file_disk_management.FileLockManager.getInstance().releaseWriteLock(filename, currentPCB.getPid());
                                currentPCB.setState(ProcessState.READY);
                                scheduler.addReadyProcess(currentPCB);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                        
                        // 请求新进程执行
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            currentPCB = null;
                        }
                    } else {
                        // 无法获取锁，将进程加入等待队列
                        currentPCB.setState(ProcessState.WAITING);
                        file_disk_management.FileLockManager.getInstance().addWriteWaitingProcess(filename, currentPCB);
                        
                        // 将PC值回退
                        int currentPC = currentPCB.getPc();
                        int instructionLength = instruction.length() + 1;
                        currentPCB.setPc(currentPC - instructionLength);
                        
                        // 请求新进程执行
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            currentPCB = null;
                        }
                    }
                    break;
                case "D":
                    int deviceId = Integer.parseInt(parts[1]);
                    int ioTime = Integer.parseInt(parts[2]);

                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() +
                            " 请求设备 " + deviceId + " 进行IO操作，预计耗时: " + ioTime + "ms");

                    // 检查设备是否存在
                    if (deviceManager.deviceExists(deviceId)) {
                        // 保存当前进程的状态
                        PCB currentProcess = currentPCB;

                        // 将进程交给设备管理器处理，并传递IO操作时间
                        deviceManager.requestIO(currentProcess, deviceId, ioTime);

                        // 请求新进程执行
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            // 没有可用进程，CPU进入空闲状态
                            currentPCB = null;
                            System.out.println("CPU-" + cpuId + " 进入空闲状态");
                        }
                    } else {
                        System.out.println("CPU-" + cpuId + " 请求的设备 " + deviceId + " 不存在");
                    }
                    break;
                case "Q":
                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 执行结束指令，即将退出");

                    // 保存当前进程的引用
                    PCB terminatingProcess = currentPCB;

                    // 将进程状态设置为TERMINATED
                    terminatingProcess.setState(ProcessState.TERMINATED);

                    // 释放进程占用的所有资源
                    // 1. 释放内存资源
                    memoryManagement.releaseMemory(terminatingProcess.getPid());
                    // 2. 释放PID
                    PIDBitmap.getInstance().freePID(terminatingProcess.getPid());
                    // 3. 从活跃PCB列表中移除
                    terminatingProcess.removePCB();
                    // 4. 清理调度器中的相关信息
                    scheduler.terminateProcess(terminatingProcess);

                    // 请求新进程执行
                    PCB nextProc = scheduler.getNextProcess();
                    if (nextProc != null) {
                        nextProc.setState(ProcessState.RUNNING);
                        changeProcess(nextProc);
                    } else {
                        currentPCB = null;
                        System.out.println("CPU-" + cpuId + " 进入空闲状态");
                    }
                    break;
                case "M":
                    int bytes = Integer.parseInt(parts[1]);
                    // 申请内存，具体实现需要在内存管理模块中完成
                    if (!memoryManagement.allocateMemory(currentPCB.getPid(), bytes)) {
                        System.out.println("CPU-" + cpuId + " 内存分配失败");
                    }
                    break;
                case "MW":
                    int logicAddress = Integer.parseInt(parts[1]);
                    int writeBytes = Integer.parseInt(parts[2]);
                    byte[] randomData = new byte[writeBytes];
                    new Random().nextBytes(randomData);
                    if (!memoryManagement.Write(this, logicAddress, randomData, writeBytes)) {
                        System.out.println("CPU-" + cpuId + " 内存写入失败");
                    }
                    break;
                case "MR":
                    logicAddress = Integer.parseInt(parts[1]);
                    int readBytes = Integer.parseInt(parts[2]);
                    byte[] readData = new byte[readBytes];
                    if (!memoryManagement.Read(this, logicAddress, readData, readBytes)) {
                        System.out.println("CPU-" + cpuId + " 内存读取失败");
                    }
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