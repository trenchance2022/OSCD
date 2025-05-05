package org.example.oscdspring.snapshot;

import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.device_management.IODevice;
import org.example.oscdspring.device_management.IORequest;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.main.Library;
import org.example.oscdspring.memory_management.MemoryManagement;
import org.example.oscdspring.process_management.CPU;
import org.example.oscdspring.process_management.ProcessState;
import org.example.oscdspring.util.SnapshotEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.example.oscdspring.main.Constants;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SystemSnapshot {

    private final FileSystemImpl fileSystem;
    private final MemoryManagement memoryManagement;
    private final SnapshotEmitterService snapshotEmitterService;

    @Autowired
    public SystemSnapshot(FileSystemImpl fileSystem,MemoryManagement memoryManagement, SnapshotEmitterService snapshotEmitterService) {
        this.fileSystem = fileSystem;
        this.memoryManagement = memoryManagement;
        this.snapshotEmitterService = snapshotEmitterService;
    }

    // 每100毫秒采集一次快照
    @Scheduled(fixedRate = 100, initialDelay = 500)
    public void updateSnapshot() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> snapshot = new HashMap<>();

            // 模拟进程管理部分示例数据
            Map<String, Object> processManagement = new HashMap<>();
            processManagement.put("cpuRunning", Library.getScheduler().getRunningProcess());
            processManagement.put("readyQueue", Library.getScheduler().getReadyProcess());
            processManagement.put("waitingQueue", Library.getScheduler().getWaitingProcess());
            // 添加 CPU 数量和调度策略
            List<CPU> cpuList = Library.getScheduler().getCpus();
            processManagement.put("cpuCount", cpuList.size()); // 新增：CPU 数量
            processManagement.put("schedulingPolicy", Library.getScheduler().getCurrentPolicy()); // 新增：调度策略
            // 添加 CPU 详细信息
            List<Map<String, Object>> cpuDetails = new ArrayList<>();
            for(int i = 0; i < cpuList.size(); i++){
                Map<String, Object> temp = new HashMap<>();
                temp.put("cpuId", i+1);
                CPU cpu=Library.getScheduler().getCpus().get(i);
                if(cpu!=null&&cpu.getCurrentPCB()!=null&&cpu.getCurrentPCB().getState()== ProcessState.RUNNING){
                    temp.put("pid", cpu.getCurrentPCB().getPid());
                    temp.put("name", cpu.getCurrentPCB().getExecutedFile());
                    temp.put("instruction", cpu.getCurrentPCB().getOriginalInstruction());
                    temp.put("remainingTime", cpu.getCurrentPCB().getTimeRemain());
                    temp.put("priority", cpu.getCurrentPCB().getPriority());
                }
                cpuDetails.add(temp);
            }

            processManagement.put("cpuDetails", cpuDetails);
            snapshot.put("processManagement", processManagement);


            // 内存管理数据
            snapshot.put("memoryManagement", memoryManagement.getPageUse());


            // 获取文件目录结构字符串
            String fileDir = fileSystem.getDirectoryStructure();
            snapshot.put("fileDirectory", fileDir);


            // 模拟磁盘管理示例数据
            Map<String, Object> diskManagement = new HashMap<>();
            diskManagement.put("totalBlocks", Constants.DISK_SIZE);  // 磁盘块总数
            diskManagement.put("occupiedBlocks", fileSystem.getOccupiedBlockIndices()); // 占用块
            snapshot.put("diskManagement", diskManagement);


            // 模拟设备列表示例数据
            Map<String, Object> deviceManagement = new HashMap<>();
            List<Map<String, Object>> deviceList = new ArrayList<>();

            for(IODevice device: DeviceManager.getDevices().values()){

                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("deviceName", device.getname()+"_"+device.getId());
                if(device.getRunningrequest()!=null) {
                    deviceInfo.put("runningProcess", device.getRunningrequest().getPcb().getPid());
                    if(device.getRequests()!=null){
                    List<Integer> waitingQueue = new ArrayList<>();
                    for (IORequest request : device.getRequests()) {
                        waitingQueue.add(request.getPcb().getPid());
                    }
                    deviceInfo.put("waitingQueue", waitingQueue);
                    }

                }
                else{
                    deviceInfo.put("runningProcess", "空闲");
                }
                deviceList.add(deviceInfo);
            }

            deviceManagement.put("deviceCount", DeviceManager.getDevices().size()); // 设备数量
            deviceManagement.put("deviceList", deviceList);
            snapshot.put("deviceManagement", deviceManagement);

            String jsonSnapshot = mapper.writeValueAsString(snapshot);
            snapshotEmitterService.sendSnapshot(jsonSnapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
