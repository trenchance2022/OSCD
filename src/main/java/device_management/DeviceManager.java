package device_management;

import process_management.PCB;
import process_management.ProcessState;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager {
    private static Map<String, IODevice> devices = new HashMap<>();

    public void addDevice(int deviceId, String deviceName) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        if(devices.containsKey(deviceKey)){
            System.out.println("添加失败：已存在id为" + deviceId + "，名称为" + deviceName + "的设备");
            return;
        }
        IODevice device = new IODevice(deviceId, deviceName);
        devices.put(deviceKey, device);
        device.start();
        System.out.println("设备管理器: 添加设备 " + deviceName + " ID: " + deviceId);
    }

    public void requestIO(PCB pcb, int processingTime, String deviceName, int deviceId) {
        String deviceKey = generateDeviceKey(deviceId, deviceName);
        if (devices.containsKey(deviceKey)) {
            pcb.setState(ProcessState.WAITING);
            devices.get(deviceKey).addRequest(new IORequest(pcb, processingTime, deviceName, deviceId));
        } else {
            System.out.println("设备管理器: 设备 " + deviceName + "(ID:" + deviceId + ") 不存在");
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
            System.out.println("设备管理器: 移除设备 " + deviceName + "(ID:" + deviceId + ")");
        }else{
            System.out.println("设备管理器: 设备 " + deviceName + "(ID:" + deviceId + ") 不存在");
        }
    }
}
