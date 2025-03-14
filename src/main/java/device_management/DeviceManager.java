package device_management;

import process_management.PCB;
import process_management.ProcessState;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager {
    private Map<Integer, IODevice> devices;

    public DeviceManager() {
        this.devices = new HashMap<>();
    }

    public void addDevice(int deviceId) {
        IODevice device = new IODevice(deviceId);
        devices.put(deviceId, device);
        device.start();
        System.out.println("设备管理器: 添加设备 " + deviceId);
    }

    /**
     * 处理进程的IO请求
     * @param pcb 请求IO的进程
     * @param deviceId 设备ID
     * @param processingTime IO操作处理时间
     */
    public void requestIO(PCB pcb, int deviceId, int processingTime) {
        if (devices.containsKey(deviceId)) {
            // 创建IO请求
            IORequest request = new IORequest(pcb, processingTime);
            
            // 将进程状态设置为等待
            pcb.setState(ProcessState.WAITING);
            
            // 将请求发送给设备
            devices.get(deviceId).addRequest(request);
            
            System.out.println("设备管理器: 进程 " + pcb.getPid() + 
                    " 请求设备 " + deviceId + " 的IO操作，处理时间: " + 
                    processingTime + "ms，进程已阻塞");
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
}