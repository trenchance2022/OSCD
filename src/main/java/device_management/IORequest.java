package device_management;

import process_management.PCB;

/**
 * IO请求类，封装进程对设备的IO请求
 */
public class IORequest {
    private PCB pcb;
    private int processingTime;
    
    public IORequest(PCB pcb, int processingTime) {
        this.pcb = pcb;
        this.processingTime = processingTime;
    }
    
    public PCB getPcb() {
        return pcb;
    }
    
    public int getProcessingTime() {
        return processingTime;
    }


}