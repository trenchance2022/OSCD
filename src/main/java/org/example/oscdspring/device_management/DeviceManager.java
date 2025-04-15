package org.example.oscdspring.device_management;

import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.ProcessState;
import org.example.oscdspring.util.LogEmitterService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DeviceManager {
    private static Map<String, IODevice> devices = new HashMap<>();

    public void addDevice(int deviceId, String deviceName) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        if(devices.containsKey(deviceKey)){
            LogEmitterService.getInstance().sendLog("添加失败：已存在id为" + deviceId + "，名称为" + deviceName + "的设备");
            return;
        }
        IODevice device = new IODevice(deviceId, deviceName);
        devices.put(deviceKey, device);
        device.start();
        LogEmitterService.getInstance().sendLog("设备管理器: 添加设备 " + deviceName + " ID: " + deviceId);
    }

    public void requestIO(PCB pcb, int processingTime, String deviceName, int deviceId) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        if (devices.containsKey(deviceKey)) {
            pcb.setState(ProcessState.WAITING);
            devices.get(deviceKey).addRequest(new IORequest(pcb, processingTime, deviceName, deviceId));
        } else {
            LogEmitterService.getInstance().sendLog("设备管理器: 设备 " + deviceName + "(ID:" + deviceId + ") 不存在");
        }
    }

    private String generateDeviceKey(int deviceId, String deviceName) {
        return deviceName + "_" + deviceId;
    }

    public boolean deviceExists(int deviceId, String deviceName) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        return devices.containsKey(deviceKey);
    }

    public void shutdown() {
        for (IODevice device : devices.values()) {
            device.shutdown();
        }
    }

    public void removeDevice(int deviceId, String deviceName) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        if(devices.containsKey(deviceKey)){
            IODevice device = devices.get(deviceKey);
            device.shutdown();
            devices.remove(deviceKey);
            LogEmitterService.getInstance().sendLog("设备管理器: 移除设备 " + deviceName + "(ID:" + deviceId + ")");
        }else{
            LogEmitterService.getInstance().sendLog("设备管理器: 设备 " + deviceName + "(ID:" + deviceId + ") 不存在");
        }
    }
}
