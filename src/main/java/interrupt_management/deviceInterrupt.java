package interrupt_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;

public class deviceInterrupt extends Interrupt {
    private PCB pcb;

    public deviceInterrupt(int deviceId, PCB pcb) {
        super(InterruptType.DEVICE);
        this.pcb = pcb;
    }

    public PCB getPcb() {
        return pcb;
    }

    /**
     * 处理设备中断
     * 将进程状态设置为就绪，重置时间片，并将进程放回就绪队列
     */
    public void handle() {
        // 将进程状态改为就绪
        pcb.setState(ProcessState.READY);
        pcb.resetTimeUsed();
        
        // 将进程放入就绪队列
        Scheduler.getInstance().addReadyProcess(pcb);

    }
}
