package interrupt_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;

public class interruptHandler {
    // 单例模式
    private static interruptHandler instance;
    
    private interruptHandler() {}
    
    public static synchronized interruptHandler getInstance() {
        if (instance == null) {
            instance = new interruptHandler();
        }
        return instance;
    }

    /**
     * 处理时钟中断
     * @param currentPCB 当前运行的进程
     * @param scheduler 调度器实例
     * @return 是否需要进行进程切换
     */
    public boolean handleClockInterrupt(PCB currentPCB, Scheduler scheduler) {
        if (currentPCB == null) {
            return false;
        }

        // 检查进程是否用完时间片
        if (currentPCB.getTimeUsed() >= currentPCB.getTimeSlice()) {
            // 时间片用完，需要进行进程切换
            System.out.println("进程 " + currentPCB.getPid() + " 时间片用完");
            
            // 降低优先级
            int currentPriority = currentPCB.getPriority();
            int newPriority = Math.min(currentPriority + 1, 3); // 最低优先级为3
            currentPCB.setPriority(newPriority);
            
            // 重置进程状态
            currentPCB.setState(ProcessState.READY);
            currentPCB.resetTimeUsed();
            
            // 将进程重新加入就绪队列
            scheduler.addReadyProcess(currentPCB);
            
            return true;
        }
        
        return false;
    }

    /**
     * 处理设备中断
     * @param interrupt 设备中断对象
     */
    public void handleDeviceInterrupt(deviceInterrupt interrupt) {
        PCB pcb = interrupt.getPcb();
        if (pcb != null) {
            // 将进程状态改为就绪
            pcb.setState(ProcessState.READY);
            pcb.resetTimeUsed();
            
            // 将进程放入就绪队列
            Scheduler.getInstance().addReadyProcess(pcb);
            
            System.out.println("设备中断处理: 设备 " + interrupt.getDeviceId() + 
                    " 的IO操作完成，进程 " + pcb.getPid() + " 已放回就绪队列");
        }
    }

    /**
     * 处理IO中断
     * @param deviceId 设备ID
     * @param currentPCB 当前运行的进程
     */
    public void handleIOInterrupt(int deviceId, PCB currentPCB) {
        if (currentPCB != null) {
            System.out.println("处理设备 " + deviceId + " 的IO中断，进程ID: " + currentPCB.getPid());
            // TODO: 实现IO中断处理逻辑
        }
    }
}
