package interrupt_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;
import process_management.CPU;

public class InterruptHandler {
    // 单例模式
    private static InterruptHandler instance;
    
    private InterruptHandler() {}
    
    public static synchronized InterruptHandler getInstance() {
        if (instance == null) {
            instance = new InterruptHandler();
        }
        return instance;
    }

    // 处理时钟中断方法
    public void handleClockInterrupt(CPU cpu, PCB currentPCB, Scheduler scheduler) {
        if (currentPCB != null) {
            // 获取当前队列的时间片
            int queueIndex = currentPCB.getCurrentQueue();
            int timeSlice = scheduler.getTimeSlice(queueIndex);

            // 获取原始指令和剩余指令的时间差作为已使用时间片
            String[] originalParts = currentPCB.getOriginalInstruction().split("\\s+");
            String[] remainParts = currentPCB.getRemainInstruction().isEmpty() ?
                    originalParts : currentPCB.getRemainInstruction().split("\\s+");
            int originalTime = Integer.parseInt(originalParts[1]);
            int remainTime = remainParts.length > 1 ? Integer.parseInt(remainParts[1]) : 0;
            int usedTime = originalTime - remainTime;
            if (usedTime == 0) usedTime = originalTime;
            
            if(scheduler.getCurrentPolicy()==Scheduler.SchedulingPolicy.PRIORITY_Preemptive) {
                PCB nextProcess = scheduler.getNextProcess();
                if(nextProcess!=null && nextProcess.getPriority() < currentPCB.getPriority()) {
                    System.out.println("CPU-" + cpu.getCpuId() + " 进程 " + currentPCB.getPid() + "被抢占 已使用时间片 " + usedTime + "/" + originalTime);
                    currentPCB.setState(ProcessState.READY);
                    scheduler.addReadyProcess(currentPCB);
                    nextProcess.setState(ProcessState.RUNNING);
                    cpu.changeProcess(nextProcess);
                } else if(nextProcess!=null) {
                    scheduler.putPCBback(nextProcess);
                }
            } else {
                System.out.println("CPU-" + cpu.getCpuId() + " 时钟中断：进程 " + currentPCB.getPid() + " 已使用时间片 " + usedTime + "/" + originalTime);
                // 检查时间片是否用尽
                if (currentPCB.getTimeUsed() >= timeSlice) {
                    System.out.println("CPU-" + cpu.getCpuId() + " 进程 " + currentPCB.getPid() + " 时间片用尽，进行进程切换");

                    // 时间片用尽，将进程放回就绪队列
                    currentPCB.setState(ProcessState.READY);
                    currentPCB.resetTimeUsed();

                    // 如果不是最低优先级，降低优先级
                    if (scheduler.getCurrentPolicy() == Scheduler.SchedulingPolicy.MLFQ && queueIndex < 3) {
                        currentPCB.setCurrentQueue(queueIndex + 1);
                        System.out.println("CPU-" + cpu.getCpuId() + " 进程 " + currentPCB.getPid() + " 优先级降低为 " + currentPCB.getCurrentQueue());
                    }

                    scheduler.addReadyProcess(currentPCB);

                    // 请求新进程执行
                    PCB nextProcess = scheduler.getNextProcess();
                    if (nextProcess != null) {
                        nextProcess.setState(ProcessState.RUNNING);
                        cpu.changeProcess(nextProcess);
                    } else {
                        // 没有可用进程，CPU进入空闲状态
                        cpu.setCurrentPCB(null);
                        System.out.println("CPU-" + cpu.getCpuId() + " 进入空闲状态");
                    }
                }
            }
        }
    }

    // 处理设备中断方法
    public void handleDeviceInterrupt(PCB pcb, Scheduler scheduler) {
        pcb.setState(ProcessState.READY);
        scheduler.addReadyProcess(pcb);
        System.out.println("设备中断：进程 " + pcb.getPid() + " I/O操作完成，进入就绪状态");
    }

    // 处理IO中断方法
    public void handleIOInterrupt(PCB pcb, String fileName, Scheduler scheduler, boolean isReadOperation) {
        // 判断是否需要释放读锁或写锁
        file_disk_management.FileLockManager lockManager = file_disk_management.FileLockManager.getInstance();
        
        if (isReadOperation) {
            // 释放读锁
            lockManager.releaseReadLock(fileName, pcb.getPid());
            System.out.println("IO中断：进程 " + pcb.getPid() + " 读取文件 " + fileName + " 完成");
        } else {
            // 释放写锁
            lockManager.releaseWriteLock(fileName, pcb.getPid());
            System.out.println("IO中断：进程 " + pcb.getPid() + " 写入文件 " + fileName + " 完成");
        }
        
        // 然后设置进程状态并加入就绪队列
        pcb.setState(ProcessState.READY);
        scheduler.addReadyProcess(pcb);
    }
    
}
