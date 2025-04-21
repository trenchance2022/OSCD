package org.example.oscdspring.snapshot;

import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.process_management.CPU;
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
    private final SnapshotEmitterService snapshotEmitterService;

    @Autowired
    public SystemSnapshot(FileSystemImpl fileSystem, SnapshotEmitterService snapshotEmitterService) {
        this.fileSystem = fileSystem;
        this.snapshotEmitterService = snapshotEmitterService;
    }

    // 每500毫秒采集一次快照
    @Scheduled(fixedRate = 500)
    public void updateSnapshot() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> snapshot = new HashMap<>();

            // 模拟进程管理部分示例数据
            Map<String, Object> processManagement = new HashMap<>();
            processManagement.put("cpuRunning", "进程A");
            processManagement.put("readyQueue", Arrays.asList("进程B", "进程C"));
            processManagement.put("waitingQueue", Arrays.asList("进程D"));
            // 添加 CPU 数量和调度策略
            int cpuCount = 123;
            processManagement.put("cpuCount", 3); // 新增：CPU 数量
            processManagement.put("schedulingPolicy", "FCFS"); // 新增：调度策略
            // 添加 CPU 详细信息
            List<Map<String, Object>> cpuDetails = new ArrayList<>();

            List<CPU> cpus = scheduler.getCpus();

            for(int i = 0; i < cpuCount; i++){
                Map<String, Object> temp = new HashMap<>();
                temp.put("cpuId", 1);
                temp.put("pid", 1001);
                temp.put("name", "进程A");
                temp.put("instruction", "exec");
                temp.put("remainingTime", 10);
                temp.put("priority", 1);
                cpuDetails.add(temp);
            }

//            Map<String, Object> cpu1 = new HashMap<>();
//            cpu1.put("cpuId", 1);
//            cpu1.put("pid", 1001);
//            cpu1.put("name", "进程A");
//            cpu1.put("instruction", "exec");
//            cpu1.put("remainingTime", 10);
//            cpu1.put("priority", 1);
//            cpuDetails.add(cpu1);
//
//            Map<String, Object> cpu2 = new HashMap<>();
//            cpu2.put("cpuId", 2);
//            cpu2.put("pid", 1002);
//            cpu2.put("name", "进程B");
//            cpu2.put("instruction", "calc");
//            cpu2.put("remainingTime", 5);
//            cpu2.put("priority", 2);
//            cpuDetails.add(cpu2);
//
//            Map<String, Object> cpu3 = new HashMap<>();
//            cpu3.put("cpuId", 3);
//            cpu3.put("pid", 1003);
//            cpu3.put("name", "进程C");
//            cpu3.put("instruction", "io");
//            cpu3.put("remainingTime", 8);
//            cpu3.put("priority", 3);
//            cpuDetails.add(cpu3);

            processManagement.put("cpuDetails", cpuDetails);
            snapshot.put("processManagement", processManagement);


            // 模拟内存管理示例数据
            Map<String, Object> memoryManagement = new HashMap<>();
            memoryManagement.put("totalFrames", Constants.MEMORY_PAGE_SIZE);  // 内存帧总数
            List<Map<String, Object>> frameInfo = new ArrayList<>();

            Map<String, Object> frame1 = new HashMap<>();
            frame1.put("frameId", 1);
            frame1.put("pid", 1);
            frame1.put("page", 2);
            frameInfo.add(frame1);

            Map<String, Object> frame2 = new HashMap<>();
            frame2.put("frameId", 4);
            frame2.put("pid", 2);
            frame2.put("page", 1);
            frameInfo.add(frame2);

            Map<String, Object> frame3 = new HashMap<>();
            frame3.put("frameId", 20);
            frame3.put("pid", 3);
            frame3.put("page", 0);
            frameInfo.add(frame3);

            Map<String, Object> frame4 = new HashMap<>();
            frame4.put("frameId", 35);
            frame4.put("pid", 1);
            frame4.put("page", 3);
            frameInfo.add(frame4);

            memoryManagement.put("frameInfo", frameInfo);
            snapshot.put("memoryManagement", memoryManagement);


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

            Map<String, Object> device1 = new HashMap<>();
            device1.put("deviceName", "Usb1");
            device1.put("runningProcess", "pid1");
            device1.put("waitingQueue", Arrays.asList("pid2", "pid3"));
            deviceList.add(device1);

            Map<String, Object> device2 = new HashMap<>();
            device2.put("deviceName", "Keyboard1");
            device2.put("runningProcess", "pid6");
            device2.put("waitingQueue", Arrays.asList("pid4", "pid5"));
            deviceList.add(device2);

            deviceManagement.put("deviceCount", 2); // 设备数量
            deviceManagement.put("deviceList", deviceList);
            snapshot.put("deviceManagement", deviceManagement);

            String jsonSnapshot = mapper.writeValueAsString(snapshot);
            snapshotEmitterService.sendSnapshot(jsonSnapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
