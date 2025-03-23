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
    private final Object lock = new Object(); // 用于同步的锁对象

    public IODevice(int deviceId) {
        this.deviceId = deviceId;
        this.requestQueue = new LinkedBlockingQueue<>();
    }

    public void addRequest(IORequest request) {
        try {
            requestQueue.put(request);
            System.out.println("设备 " + deviceId + " 接收到进程 " + request.getPcb().getPid() + 
                    " 的IO请求，处理时间: " + request.getProcessingTime() + "ms");
            
            // 唤醒可能正在休眠的设备线程
            synchronized (lock) {
                lock.notify();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                IORequest request = null;
                
                // 检查队列中是否有请求
                request = requestQueue.poll();
                
                if (request == null) {
                    // 没有请求，设备进入休眠状态
                    System.out.println("设备 " + deviceId + " 没有IO请求，进入休眠状态");
                    synchronized (lock) {
                        lock.wait(); // 等待被唤醒
                    }
                    // 被唤醒后，再次检查队列
                    request = requestQueue.poll();
                    if (request == null) continue; // 如果仍然没有请求，继续循环
                }
                
                // 处理IO请求
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
        // 确保在休眠状态下也能正常关闭
        synchronized (lock) {
            lock.notify();
        }
        interrupt();
    }
    
    public int getDeviceId() {
        return deviceId;
    }
}