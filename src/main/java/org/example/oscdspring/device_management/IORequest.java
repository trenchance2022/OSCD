package org.example.oscdspring.device_management;

import org.example.oscdspring.process_management.PCB;

/**
 * IO请求类，封装进程对设备的IO请求
 */
public class IORequest {
    private PCB pcb;
    private int processingTime;
    private String deviceName;
    private int deviceId;

    public IORequest(PCB pcb, int processingTime, String deviceName, int deviceId) {
        this.pcb = pcb;
        this.processingTime = processingTime;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
    }

    public PCB getPcb() {
        return pcb;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getDeviceId() {
        return deviceId;
    }
}