package org.example.oscdspring.device_management;

import org.example.oscdspring.interrupt_management.InterruptHandler;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.Scheduler;
import org.example.oscdspring.process_management.ProcessState;
import org.example.oscdspring.main.Library;
import org.example.oscdspring.main.Constants;

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
        new Thread(()->{
            //检查请求队列中是否已经有进程terminal
            while(true){
                try {
                    Thread.sleep(100);
                    for(IORequest request: requests){
                        if(request.getPcb().getState()== ProcessState.TERMINATED){
                            requests.remove(request);
                            Library.getScheduler().removeWaitingProcess(request.getPcb());
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        try {
            while (!isInterrupted()) {
                IORequest request = requests.take();
                runningrequest = request;
                int time=request.getProcessingTime();
                while(time> Constants.CLOCK_INTERRUPT_INTERVAL_MS){
                    Thread.sleep(Constants.CLOCK_INTERRUPT_INTERVAL_MS);
                    time-=Constants.CLOCK_INTERRUPT_INTERVAL_MS;
                    if(request.getPcb().getState()== ProcessState.TERMINATED){
                        break;
                    }
                }
                if(request.getPcb().getState()== ProcessState.TERMINATED) {
                    // 执行完毕恢复原状
                    runningrequest = null;
                    // 触发设备中断
                    PCB pcb = request.getPcb();
                    Library.getScheduler().removeWaitingProcess(pcb);

                }else {
                    Thread.sleep(time);
                    // 执行完毕恢复原状
                    runningrequest = null;
                    // 触发设备中断
                    PCB pcb = request.getPcb();
                    InterruptHandler.getInstance().handleDeviceInterrupt(pcb, Scheduler.getInstance());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        runningrequest = null;
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