package process_management;

import device_management.DeviceManager;
import file_disk_management.FileSystemImpl;
import interrupt_management.Interrupt;
import interrupt_management.InterruptRequestLine;
import main.Constants;
import main.Main;
import memory_management.MMU;
import memory_management.MemoryManagement;
import java.util.concurrent.BlockingQueue;
import java.util.Random;

public class CPU extends Thread {
    private static int cpuNum;//当前CPU个数
    private int cpuId; // CPU 的唯一标识符
    private PCB currentPCB; // 当前正在执行的进程
    private Scheduler scheduler; // 调度器
    private DeviceManager deviceManager; // 设备管理器
    private BlockingQueue<Interrupt> interruptQueue; // 中断队列
    private MemoryManagement memoryManagement; // 内存管理模块
    private final MMU mmu = new MMU(); // 内存管理单元

    public static void setCpuNum(int cpuNum) {
        CPU.cpuNum = cpuNum;
    }

    public static int getCpuNum() {
        return cpuNum;
    }

    public CPU(int cpuId, Scheduler scheduler, DeviceManager deviceManager, InterruptRequestLine interruptQueue) {
        this.cpuId = cpuId;
        this.scheduler = scheduler;
        this.deviceManager = deviceManager;
        this.interruptQueue = interruptQueue.registerCPU(cpuId);
        this.memoryManagement = Main.memoryManagement;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 检查中断队列，处理中断
                while (!interruptQueue.isEmpty()) {
                    Interrupt interrupt = interruptQueue.take();
                    handleInterrupt(interrupt);
                }

                // 执行当前进程
                if (currentPCB != null) {
                    execute();
                } else {
                    Thread.sleep(100); // 空闲时等待
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 执行当前进程
    private void execute() {
        String instruction = InstructionFetch();
        if (instruction != null) {
            executeInstruction(instruction);
        }
    }

    // 从当前进程的程序计数器中获取指令
    private String InstructionFetch() {
        if (currentPCB == null) {
            return null;
        }

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
                System.exit(-1);
                return null;
            }

            char c = (char) buffer[0];
            currentAddress++;

            if (c == '#') {
                System.out.println(instruction);
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

    // 处理中断
    private void handleInterrupt(Interrupt interrupt) {
        switch (interrupt.getType()) {
            case CLOCK:
                handleClockInterrupt();
                break;
            case IO:
                handleIOInterrupt(interrupt);
                break;
            default:
                System.out.println("CPU-" + cpuId + " 未知中断类型: " + interrupt.getType());
        }
    }

    // 处理时钟中断
    private void handleClockInterrupt() {
        if (currentPCB != null) {
            // 获取当前队列的时间片
            int queueIndex = currentPCB.getCurrentQueue();
            int timeSlice = scheduler.getTimeSlice(queueIndex);


            //System.out.println("CPU-" + cpuId + " 时钟中断：进程 " + currentPCB.getPid() +" 已使用时间片 " + currentPCB.getTimeUsed() + "/" + timeSlice);
            // 更新进程已使用的时间片
            currentPCB.incrementTimeUsed(Constants.CLOCK_INTERRUPT_INTERVAL_MS);
            // 获取原始指令和剩余指令的时间差作为已使用时间片
            String[] originalParts = currentPCB.getOriginalInstruction().split("\\s+");
            String[] remainParts = currentPCB.getRemainInstruction().isEmpty() ? 
                                 originalParts : currentPCB.getRemainInstruction().split("\\s+");
            int originalTime = Integer.parseInt(originalParts[1]);
            int remainTime = remainParts.length > 1 ? Integer.parseInt(remainParts[1]) : 0;
            int usedTime = originalTime - remainTime;
            if(usedTime == 0)usedTime = originalTime;
            System.out.println("CPU-" + cpuId + " 时钟中断：进程 " + currentPCB.getPid() + " 已使用时间片 " + usedTime + "/" + timeSlice);


            
            // 检查时间片是否用尽
            if (currentPCB.getTimeUsed() >= timeSlice) {
                System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 时间片用尽，进行进程切换");

                // 时间片用尽，将进程放回就绪队列
                currentPCB.setState(ProcessState.READY);
                currentPCB.resetTimeUsed();

                // 如果不是最低优先级，降低优先级
                if (scheduler.getCurrentPolicy() == Scheduler.SchedulingPolicy.MLFQ && queueIndex < 3) {
                    currentPCB.setCurrentQueue(queueIndex + 1);
                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 优先级降低为 " + currentPCB.getCurrentQueue());
                }

                scheduler.addReadyProcess(currentPCB);

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
            }
        }
    }

    // 处理I/O中断
    private void handleIOInterrupt(Interrupt interrupt) {
        if (currentPCB != null) {
            System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 完成I/O操作，重新加入就绪队列");
            currentPCB.setState(ProcessState.READY);
            scheduler.addReadyProcess(currentPCB);

            // 请求新进程执行
            PCB nextProcess = scheduler.getNextProcess();
            if (nextProcess != null) {
                nextProcess.setState(ProcessState.RUNNING);
                changeProcess(nextProcess);
            } else {
                currentPCB = null;
                System.out.println("CPU-" + cpuId + " 进入空闲状态");
            }
        }
    }

    // 解析并执行指令
    // 在 CPU 类中完善 executeInstruction 方法
    private void executeInstruction(String instruction) {
        System.out.println("CPU-" + cpuId + " 执行指令: " + instruction);

        // 将指令按空格分割成操作码和操作数
        String[] parts = instruction.split("\\s+");
        if (parts.length == 0) {
            System.out.println("CPU-" + cpuId + " 空指令");
            return;
        }

        String opcode = parts[0].toUpperCase();

        try {
            // 根据操作码执行相应操作
            switch (opcode) {
                case "C": // 计算指令
                    int computeTime = Integer.parseInt(parts[1]);
                    // 如果是从中断恢复的指令，使用保存的剩余时间
                    if (currentPCB.getRemainInstruction().isEmpty()) {
                        //如果remainInstruction为空，则保存原指令消息
                        currentPCB.setOriginalInstruction(instruction);
                        currentPCB.setRemainInstruction(instruction);
                    }

                    // 计算本次能执行的时间
                    int remainingTimeSlice = currentPCB.getTimeSlice() - currentPCB.getTimeUsed();
                    int executeTime = Math.min(computeTime, remainingTimeSlice);

                    Thread.sleep(executeTime);
                    currentPCB.incrementTimeUsed(executeTime);

                    // 如果还有剩余时间未执行完，保存剩余时间
                    if (executeTime < computeTime) {
                        currentPCB.setRemainInstruction("C " + (computeTime - executeTime));
                    }
                    else{
                        currentPCB.setRemainInstruction("");
                    }

                    // 如果是RR调度模式，执行时钟中断
                    if (scheduler.getCurrentPolicy() == Scheduler.SchedulingPolicy.RR) {
                        handleClockInterrupt();
                    }
                    
                    break;

                case "R": // 读取文件指令
                    String filename = parts[1];
                    int readtime = Integer.parseInt(parts[2]);

                    // 尝试获取文件读锁
                    if (file_disk_management.FileLockManager.getInstance().acquireReadLock(filename, currentPCB.getPid())) {
                        // 将进程设置为阻塞状态
                        currentPCB.setState(ProcessState.WAITING);
                        // 拷贝变量（避免被主线程修改）
                        final PCB pcbToRelease = currentPCB;
                        final String fileToRead = filename;

                        // 创建一个新线程来处理文件读取
                        new Thread(() -> {
                            try {
                                Thread.sleep(readtime);
                                System.out.println("进程"+pcbToRelease.getPid()+"读取完毕");
                                // 读取完成后，释放锁并将进程加入就绪队列
                                file_disk_management.FileLockManager.getInstance().releaseReadLock(fileToRead, pcbToRelease.getPid());
                                pcbToRelease.setState(ProcessState.READY);
                                scheduler.addReadyProcess(pcbToRelease);
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
                case "W": // 写入文件指令
                    filename = parts[1];
                    int writeTime = Integer.parseInt(parts[2]);

                    // 尝试获取文件写锁
                    if (file_disk_management.FileLockManager.getInstance().acquireWriteLock(filename, currentPCB.getPid())) {
                        currentPCB.setState(ProcessState.WAITING);

                        // 拷贝变量（避免被主线程修改）
                        final PCB pcbToRelease = currentPCB;
                        final String fileToWrite = filename;

                        new Thread(() -> {
                            try {
                                Thread.sleep(writeTime);
                                System.out.println("进程"+pcbToRelease.getPid()+"写入完毕");
                                file_disk_management.FileLockManager.getInstance().releaseWriteLock(fileToWrite, pcbToRelease.getPid());
                                pcbToRelease.setState(ProcessState.READY);
                                scheduler.addReadyProcess(pcbToRelease);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();

                        // 切换进程
                        PCB nextProcess = scheduler.getNextProcess();
                        if (nextProcess != null) {
                            nextProcess.setState(ProcessState.RUNNING);
                            changeProcess(nextProcess);
                        } else {
                            currentPCB = null; // 这里不会影响子线程的 pcbToRelease
                        }
                    } else {
                        System.out.println("Waiting!!!!");
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
                case "D": // 设备I/O指令
                    String deviceName = parts[1];
                    int deviceId = Integer.parseInt(parts[2]);
                    int ioTime = Integer.parseInt(parts[3]);

                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() +
                            " 请求设备 "+ deviceName + " " + deviceId + " 进行I/O操作，预计耗时: " + ioTime + "ms");

                    // 检查设备是否存在
                    if (deviceManager.deviceExists(deviceId,deviceName)) {
                        // 保存当前进程的状态
                        PCB currentProcess = currentPCB;

                        // 将进程交给设备管理器处理，并传递IO操作时间
                        deviceManager.requestIO(currentProcess, ioTime , deviceName, deviceId);

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
                case "Q": // 结束进程指令
                    System.out.println("CPU-" + cpuId + " 进程 " + currentPCB.getPid() + " 执行结束指令，即将退出");

                    // 保存当前进程的引用
                    PCB terminatingProcess = currentPCB;

                    // 将进程状态设置为TERMINATED
                    terminatingProcess.setState(ProcessState.TERMINATED);

                    // 释放进程占用的所有资源
                    memoryManagement.FreeProcess(this);
                    PIDBitmap.getInstance().freePID(terminatingProcess.getPid());
                    file_disk_management.FileLockManager.getInstance().releaseAllLocks(terminatingProcess.getPid());

                    // 从活跃PCB列表中移除
                    terminatingProcess.removePCB();

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
                case "M": // 内存分配指令
                    int bytes = Integer.parseInt(parts[1]);
                    if (!memoryManagement.Allocate(this, bytes)) {
                        System.out.println("CPU-" + cpuId + " 内存分配失败");
                    }
                    break;
                case "MW": // 内存写入指令
                    int logicAddress = Integer.parseInt(parts[1]);
                    int writeBytes = Integer.parseInt(parts[2]);
                    byte[] randomData = new byte[writeBytes];
                    new Random().nextBytes(randomData);
                    if (!memoryManagement.Write(this, logicAddress, randomData, writeBytes)) {
                        System.out.println("CPU-" + cpuId + " 内存写入失败");
                    }
                    break;
                case "MR": // 内存读取指令
                    logicAddress = Integer.parseInt(parts[1]);
                    int readBytes = Integer.parseInt(parts[2]);
                    byte[] readData = new byte[readBytes];
                    if (!memoryManagement.Read(this, logicAddress, readData, readBytes)) {
                        System.out.println("CPU-" + cpuId + " 内存读取失败");
                    }
                    break;
                default:
                    System.out.println("CPU-" + cpuId + " 未知指令: " + opcode);
            }
        } catch (Exception e) {
            System.out.println("CPU-" + cpuId + " 指令解析错误: " + instruction);
            e.printStackTrace();
        }
    }

    // 切换当前进程
    public void changeProcess(PCB newPCB) {
        if (newPCB != null) {
            mmu.update(newPCB);
            this.currentPCB = newPCB;
            System.out.println("CPU-" + cpuId + " 开始执行进程 " + currentPCB.getPid());
        }
    }

    // 获取当前CPU的ID
    public int getCpuId() {
        return cpuId;
    }

    // 判断CPU是否空闲
    public boolean isIdle() {
        return currentPCB == null;
    }

    // 获取当前正在执行的进程
    public PCB getCurrentPCB() {
        return currentPCB;
    }

    // 获取内存管理单元
    public MMU getMMU() {
        return mmu;
    }
}