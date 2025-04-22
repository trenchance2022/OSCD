package org.example.oscdspring.device_management;

import org.example.oscdspring.interrupt_management.InterruptHandler;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.Scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IODevice extends Thread {
    private final int id;
    private final String name;
    private BlockingQueue<IORequest> requests = new LinkedBlockingQueue<>();
    private IORequest runningrequest = null;

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
                runningrequest = request;
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

    @Override
    public long getId() {
        return id;
    }

    public String getname() {
        return name;
    }

    public BlockingQueue<IORequest> getRequests() {
        return requests;
    }

    public IORequest getRunningrequest() {
        return runningrequest;
    }

}