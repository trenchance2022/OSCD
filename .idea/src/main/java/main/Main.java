package main;

import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import memory_management.MemoryManagement;
import memory_management.MemoryManagementImpl;
import process_management.CPU;
import process_management.Scheduler;
import device_management.DeviceManager;
import interrupt_management.InterruptRequestLine;

public class Main {
    public static void main(String[] args) {
        // 初始化文件系统和内存管理模块
        FileDiskManagement fileDiskManagement = new FileSystemImpl();
        MemoryManagement memoryManagement = new MemoryManagementImpl();

        // 初始化设备管理器
        DeviceManager deviceManager = new DeviceManager();

        // 初始化中断请求线
        InterruptRequestLine interruptRequestLine = InterruptRequestLine.getInstance();

        // 初始化调度器
        Scheduler scheduler = Scheduler.getInstance();

        // 设置每个队列的调度策略
        scheduler.setQueuePolicy(0, Scheduler.SchedulingPolicy.FCFS); // 队列0：FCFS
        scheduler.setQueuePolicy(1, Scheduler.SchedulingPolicy.SJF);  // 队列1：SJF
        scheduler.setQueuePolicy(2, Scheduler.SchedulingPolicy.RR);   // 队列2：RR
        scheduler.setQueuePolicy(3, Scheduler.SchedulingPolicy.PRIORITY); // 队列3：优先级调度

        // 初始化 4 个 CPU
        for (int i = 0; i < 4; i++) {
            CPU cpu = new CPU(i, scheduler, deviceManager, interruptRequestLine);
            scheduler.addCPU(cpu); // 将 CPU 注册到调度器
            cpu.start(); // 启动 CPU 线程
        }

        // 启动 Shell
        Shell shell = new Shell(fileDiskManagement, memoryManagement);
        shell.start();
    }
}