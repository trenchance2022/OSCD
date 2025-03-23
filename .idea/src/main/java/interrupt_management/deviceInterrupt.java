package interrupt_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;

public class DeviceInterrupt extends Interrupt {
    private PCB pcb;

    public DeviceInterrupt(int deviceId, PCB pcb) {
        super(InterruptType.DEVICE, deviceId);
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
        
        System.out.println("设备中断处理: 设备 " + getDeviceId() + 
                " 的IO操作完成，进程 " + pcb.getPid() + " 已放回就绪队列");
    }
}
