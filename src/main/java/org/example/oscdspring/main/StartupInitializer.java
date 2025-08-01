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

    // 从配置文件读取系统 CPU 数量
    @Value("${app.cpu.num}")
    private int cpuNum;

    // 从配置文件读取调度策略
    @Value("${app.scheduler.policy}")
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

        // 初始化测试用的设备
        deviceManager.addDevice(1, "Printer");
        deviceManager.addDevice(2, "Scanner");
        deviceManager.addDevice(3, "USB");

        // 初始化测试用的文件系统
        fileSystem.createDirectory("d1");
        fileSystem.changeDirectory("d1");

        // 设备管理测试程序1
        fileSystem.createFile("t1", 1);
        fileSystem.createFile("t2", 1);
        fileSystem.createFile("t3", 1);
        fileSystem.editFile("t1", "M 4096#C 5000#C 10000#D Printer 1 10000#Q#");
        fileSystem.editFile("t2", "M 4096#C 10000#C 2000#D Printer 1 10000#Q#");
        fileSystem.editFile("t3", "M 10240#C 5000#C 10000#D USB 3 10000#Q#");

        // 读写互斥测试程序
        fileSystem.createFile("t4", 1);
        fileSystem.createFile("t5", 1);
        fileSystem.editFile("t5", "M 4096#W t4 10000#Q#");
        fileSystem.createFile("t6", 1);
        fileSystem.editFile("t6", "M 4096#W t4 10000#Q#");
        fileSystem.createFile("t7", 1);
        fileSystem.editFile("t7", "M 4096#R t4 10000#Q#");
        fileSystem.createFile("t8", 1);
        fileSystem.editFile("t8", "M 4096#R t4 10000#Q#");

        // 进程调度测试程序
        fileSystem.createFile("t9", 1);
        fileSystem.editFile("t9", "C 2000#Q#");
        fileSystem.createFile("t10", 1);
        fileSystem.editFile("t10", "C 3000#Q#");
        fileSystem.createFile("t11", 1);
        fileSystem.editFile("t11", "C 4000#Q#");
        fileSystem.createFile("t12", 1);
        fileSystem.editFile("t12", "C 1000#C 2000#Q#");
        fileSystem.createFile("t13", 1);
        fileSystem.editFile("t13", "C 10000#Q#");
        fileSystem.createFile("t14", 1);
        fileSystem.editFile("t14", "C 2000#Q#");
        fileSystem.createFile("t15", 1);
        fileSystem.editFile("t15", "C 2000#Q#");
        fileSystem.createFile("t16", 1);
        fileSystem.editFile("t16", "C 2000#Q#");

        // 内存管理测试程序

        // 正常读写，分配10000字节内存
        fileSystem.createFile("m1", 1);
        fileSystem.editFile("m1", "C 20000#M 10000#C 20000#MR 0 1000#C 20000#MW 100 9000#C 20000#MR 0 1000#C 20000#Q#");
        // 读写越界,分配100000字节内存，读101000 终止操作，报错
        fileSystem.createFile("m2", 1);
        fileSystem.editFile("m2", "C 10000#M 10000#C 10000#MR 0 11000#Q#");

        // 设备管理测试程序2
        fileSystem.createFile("e1", 1);
        fileSystem.createFile("e2", 1);
        fileSystem.createFile("e3", 1);
        fileSystem.editFile("e1", "M 4096#D Printer 1 10000#C 5000#Q#");
        fileSystem.editFile("e2", "M 4096#D Scanner 2 2000#C 2000#Q#");
        fileSystem.editFile("e3", "M 10240#C 5000#D USB 3 10000#C 5000#Q#");

        // 复杂测试
        fileSystem.createFile("x1", 1);
        fileSystem.createFile("x2", 1);
        fileSystem.createFile("x3", 1);
        fileSystem.createFile("x4", 1);
        fileSystem.createFile("x5", 1);
        fileSystem.createFile("x6", 1);
        fileSystem.createFile("x7", 1);
        fileSystem.createFile("x8", 1);
        fileSystem.createFile("x9", 1);
        fileSystem.createFile("x10", 1);
        fileSystem.createFile("x11", 1);

        fileSystem.editFile("x1", "M 10000#C 3000#MR 0 10000#C 3000#MW 100 9000#C 3000#MR 0 10000#C 3000#Q#");
        fileSystem.editFile("x2", "M 4096#D Printer 1 10000#Q#");
        fileSystem.editFile("x3", "M 4096#D Printer 1 10000#Q#");
        fileSystem.editFile("x4", "M 10240#D USB 3 10000#Q#");
        fileSystem.editFile("x5", "M 4096#W t4 2000#Q#");
        fileSystem.editFile("x6", "M 4096#R t4 2000#Q#");
        fileSystem.editFile("x7", "M 4096#W t4 2000#Q#");
        fileSystem.editFile("x8", "M 4096#R t4 2000#Q#");
        fileSystem.editFile("x9", "C 2000#Q#");
        fileSystem.editFile("x10", "C 3000#Q#");
        fileSystem.editFile("x11", "C 4000#Q#");


        // 回到根目录
        fileSystem.goBack();
        // 启动调度器（调度器内部一般会启动自己的线程处理调度逻辑）
        scheduler.start();
    }
}
