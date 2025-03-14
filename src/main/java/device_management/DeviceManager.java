package device_management;

import process_management.PCB;
import process_management.ProcessState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DeviceManager {
    private Map<Integer, IODevice> devices;
    private BlockingQueue<PCB> readyQueue;

    public DeviceManager(BlockingQueue<PCB> readyQueue) {
        this.devices = new HashMap<>();
        this.readyQueue = readyQueue;
    }

    public void addDevice(int deviceId, int processingTime) {
        IODevice device = new IODevice(deviceId, readyQueue, processingTime);
        devices.put(deviceId, device);
        device.start();
    }

    public void requestIO(PCB pcb, int deviceId) {
        if (devices.containsKey(deviceId)) {
            pcb.setState(ProcessState.WAITING); // 修改为WAITING状态而不是BLOCKED
            
            // 将进程添加到设备队列
            devices.get(deviceId).addProcess(pcb);
            System.out.println("进程 " + pcb.getPid() + " 请求设备 " + deviceId + " 的IO操作，进程已阻塞");
        } else {
            System.out.println("设备 " + deviceId + " 不存在");
        }
    }
}