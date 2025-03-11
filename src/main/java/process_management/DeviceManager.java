package process_management;

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
            pcb.setState(ProcessState.WAITING);
            devices.get(deviceId).addProcess(pcb);
            System.out.println("进程 " + pcb.getPID() + " 请求设备 " + deviceId + " 的IO操作，进程已阻塞");
        } else {
            System.out.println("设备 " + deviceId + " 不存在");
        }
    }

    public void shutdown() {
        for (IODevice device : devices.values()) {
            device.shutdown();
        }
    }
}
