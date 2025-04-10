package org.example.oscdspring.main;

import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.memory_management.MemoryManagement;
import org.example.oscdspring.process_management.CPU;
import org.example.oscdspring.process_management.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    @Autowired
    private FileSystemImpl fileSystem;

    @Autowired
    private MemoryManagement memoryManagement;

    @Autowired
    private DeviceManager deviceManager;

    // 单例
    Scheduler scheduler = Scheduler.getInstance();

    // 从配置文件读取系统 CPU 数量，默认 4 个
    @Value("${app.cpu.num:4}")
    private int cpuNum;

    // 从配置文件读取调度策略，默认 FCFS
    @Value("${app.scheduler.policy:FCFS}")
    private String schedulerPolicy;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 将核心模块注入 Library 以供全局共享
        Library.setFileSystem(fileSystem);
        Library.setMemoryManagement(memoryManagement);
        Library.setDeviceManager(deviceManager);
        Library.setScheduler(scheduler);

        // 配置调度器策略，根据配置字符串转换为对应枚举
        switch (schedulerPolicy.toUpperCase()) {
            case "FCFS":
                scheduler.configure(Scheduler.SchedulingPolicy.FCFS);
                break;
            case "SJF":
                scheduler.configure(Scheduler.SchedulingPolicy.SJF);
                break;
            case "RR":
                scheduler.configure(Scheduler.SchedulingPolicy.RR);
                break;
            case "PRIORITY":
                scheduler.configure(Scheduler.SchedulingPolicy.PRIORITY);
                break;
            case "MLFQ":
                scheduler.configure(Scheduler.SchedulingPolicy.MLFQ);
                break;
            case "PRIORITY_PREEMPTIVE":
                scheduler.configure(Scheduler.SchedulingPolicy.PRIORITY_Preemptive);
                break;
            default:
                scheduler.configure(Scheduler.SchedulingPolicy.FCFS);
        }

        // 根据配置的 CPU 数量创建并启动 CPU 线程
        for (int i = 0; i < cpuNum; i++) {
            CPU cpu = new CPU(i, scheduler, deviceManager);
            scheduler.addCPU(cpu);
            cpu.start();
        }

        // 启动调度器（调度器内部一般会启动自己的线程处理调度逻辑）
        scheduler.start();
    }
}
