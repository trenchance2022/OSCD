package device_management;

import process_management.PCB;
import process_management.Scheduler;
import interrupt_management.InterruptHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IODevice extends Thread {
    private final int id;
    private final String name;
    private BlockingQueue<IORequest> requests = new LinkedBlockingQueue<>();

    public IODevice(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addRequest(IORequest request) {
        requests.add(request);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                IORequest request = requests.take();
                Thread.sleep(request.getProcessingTime());
                // 触发设备中断
                PCB pcb = request.getPcb();
                InterruptHandler.getInstance().handleDeviceInterrupt(pcb, Scheduler.getInstance());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        this.interrupt();
    }
}