package device_management;

import process_management.PCB;
import process_management.ProcessState;
import process_management.Scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IODevice extends Thread {
    private int deviceId;
    private BlockingQueue<IORequest> requestQueue;
    private volatile boolean running = true;

    public IODevice(int deviceId) {
        this.deviceId = deviceId;
        this.requestQueue = new LinkedBlockingQueue<>();
    }

    public void addRequest(IORequest request) {
        try {
            requestQueue.put(request);
            System.out.println("设备 " + deviceId + " 接收到进程 " + request.getPcb().getPid() + 
                    " 的IO请求，处理时间: " + request.getProcessingTime() + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                IORequest request = requestQueue.take();
                PCB pcb = request.getPcb();
                int processingTime = request.getProcessingTime();
                
                System.out.println("设备 " + deviceId + " 正在处理进程 " + pcb.getPid() + 
                        " 的IO请求，预计耗时: " + processingTime + "ms");
                
                // 模拟IO操作时间
                Thread.sleep(processingTime);
                
                // IO操作完成，将进程状态改为就绪，并放入就绪队列
                pcb.setState(ProcessState.READY);
                pcb.resetTimeUsed();
                
                // 使用调度器的addReadyProcess方法
                Scheduler.getInstance().addReadyProcess(pcb);
                
                System.out.println("设备 " + deviceId + " 完成进程 " + pcb.getPid() + 
                        " 的IO操作，进程已放回就绪队列");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
    
    public int getDeviceId() {
        return deviceId;
    }
}