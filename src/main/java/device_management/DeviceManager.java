package device_management;

import process_management.PCB;
import process_management.ProcessState;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager {
    private Map<Integer, IODevice> devices = new HashMap<>();

    public void addDevice(int deviceId, String deviceName) {
        if(devices.containsKey(deviceId)){
            System.out.println("添加失败：已存在id为"+deviceId+"的设备");
            return;
        }
        IODevice device = new IODevice(deviceId, deviceName);
        devices.put(deviceId, device);
        device.start();
        System.out.println("设备管理器: 添加设备 " + deviceName + " ID: " + deviceId);
    }

    public void requestIO(PCB pcb, int deviceId, int processingTime) {
        if (devices.containsKey(deviceId)) {
            pcb.setState(ProcessState.WAITING);
            devices.get(deviceId).addRequest(new IORequest(pcb, processingTime));
        } else {
            System.out.println("设备管理器: 设备 " + deviceId + " 不存在");
        }
    }

    public boolean deviceExists(int deviceId) {
        return devices.containsKey(deviceId);
    }

    public void shutdown() {
        for (IODevice device : devices.values()) {
            device.shutdown();
        }
    }

    public void removeDevice(int deviceId) {
        if(devices.containsKey(deviceId)){
            IODevice device=devices.get(deviceId);
            device.shutdown();
            devices.remove(deviceId);
            System.out.println("设备管理器: 移除设备 " + deviceId);
        }else{
            System.out.println("设备管理器: 设备 " + deviceId + " 不存在");
        }
    }
}
