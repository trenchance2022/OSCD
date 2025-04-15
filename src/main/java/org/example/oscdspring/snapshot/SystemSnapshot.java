package org.example.oscdspring.snapshot;

import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.util.SnapshotEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SystemSnapshot {

    private final FileSystemImpl fileSystem;
    private final SnapshotEmitterService snapshotEmitterService;

    @Autowired
    public SystemSnapshot(FileSystemImpl fileSystem, SnapshotEmitterService snapshotEmitterService) {
        this.fileSystem = fileSystem;
        this.snapshotEmitterService = snapshotEmitterService;
    }

    // 每100毫秒采集一次快照（可根据需要调整间隔）
    @Scheduled(fixedRate = 100)
    public void updateSnapshot() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> snapshot = new HashMap<>();

            // 模拟进程管理部分示例数据
            Map<String, Object> processManagement = new HashMap<>();
            processManagement.put("cpuRunning", "进程A");
            processManagement.put("readyQueue", Arrays.asList("进程B", "进程C"));
            processManagement.put("waitingQueue", Collections.singletonList("进程D"));
            List<Map<String, Object>> cpuDetails = new ArrayList<>();
            Map<String, Object> cpu1 = new HashMap<>();
            cpu1.put("cpuId", 1);
            cpu1.put("pid", 1001);
            cpu1.put("name", "进程A");
            cpu1.put("instruction", "exec");
            cpu1.put("remainingTime", 10);
            cpu1.put("priority", 1);
            cpuDetails.add(cpu1);
            processManagement.put("cpuDetails", cpuDetails);
            snapshot.put("processManagement", processManagement);

            // 模拟内存管理示例数据
            Map<String, Object> memoryManagement = new HashMap<>();
            List<Map<String, Object>> frameInfo = new ArrayList<>();
            Map<String, Object> frame1 = new HashMap<>();
            frame1.put("frameId", 1);
            frame1.put("pid", 1001);
            frame1.put("page", 2);
            frameInfo.add(frame1);
            memoryManagement.put("frameInfo", frameInfo);
            snapshot.put("memoryManagement", memoryManagement);

            // 获取文件目录结构字符串
            String fileDir = fileSystem.getDirectoryStructure();
            snapshot.put("fileDirectory", fileDir);

            // 模拟磁盘管理示例数据
            Map<String, Object> diskManagement = new HashMap<>();
            diskManagement.put("occupiedBlocks", Arrays.asList(1,2,5,7));
            diskManagement.put("freeBlocks", Arrays.asList(3,4,6,8,9));
            snapshot.put("diskManagement", diskManagement);

            // 模拟设备列表示例数据
            List<Map<String, Object>> deviceList = new ArrayList<>();
            Map<String, Object> device1 = new HashMap<>();
            device1.put("deviceName", "device1");
            device1.put("runningProcess", "prog1");
            device1.put("waitingQueue", Arrays.asList("prog2"));
            deviceList.add(device1);
            Map<String, Object> device2 = new HashMap<>();
            device2.put("deviceName", "device2");
            device2.put("runningProcess", null);
            device2.put("waitingQueue", Arrays.asList("prog3"));
            deviceList.add(device2);
            Map<String, Object> device3 = new HashMap<>();
            device3.put("deviceName", "device3");
            device3.put("runningProcess", "prog4");
            device3.put("waitingQueue", Collections.emptyList());
            deviceList.add(device3);
            snapshot.put("deviceList", deviceList);

            String jsonSnapshot = mapper.writeValueAsString(snapshot);
            snapshotEmitterService.sendSnapshot(jsonSnapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
