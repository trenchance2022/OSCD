package process_management;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IODevice extends Thread {
    private int deviceId;
    private BlockingQueue<PCB> deviceQueue;
    private int processingTime; // 模拟IO操作的处理时间（毫秒）
    private volatile boolean running = true;

    public IODevice(int deviceId, BlockingQueue<PCB> readyQueue, int processingTime) {
        this.deviceId = deviceId;
        this.deviceQueue = new LinkedBlockingQueue<>();
        this.processingTime = processingTime;
    }

    public void addProcess(PCB pcb) {
        try {
            deviceQueue.put(pcb);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                PCB pcb = deviceQueue.take();
                System.out.println("设备 " + deviceId + " 正在处理进程 " + pcb.getPid());
                
                // 模拟IO操作时间
                Thread.sleep(processingTime);
                
                // IO操作完成，将进程状态改为就绪，并放入就绪队列
                pcb.setState(ProcessState.READY);
                pcb.resetTimeUsed();
                
                // 使用调度器的addReadyProcess方法
                Scheduler.getInstance().addReadyProcess(pcb);
                
                System.out.println("设备 " + deviceId + " 完成进程 " + pcb.getPid() + " 的IO操作，进程已放回就绪队列");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}