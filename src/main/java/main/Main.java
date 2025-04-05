package main;

import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import memory_management.MemoryManagement;
import memory_management.MemoryManagementImpl;
import process_management.CPU;
import process_management.Scheduler;
import device_management.DeviceManager;
import interrupt_management.InterruptRequestLine;

import java.util.Scanner;

public class Main {
    // 初始化文件系统和内存管理模块
    public static FileSystemImpl fileSystem = new FileSystemImpl();
    public static MemoryManagement memoryManagement = new MemoryManagementImpl(fileSystem);

    public static void main(String[] args) {

        // 初始化设备管理器
        DeviceManager deviceManager = new DeviceManager();

        // 初始化中断请求线
        InterruptRequestLine interruptRequestLine = InterruptRequestLine.getInstance();

        // 初始化调度器
        Scheduler scheduler = Scheduler.getInstance();

        Library.setFileSystem(fileSystem);
        Library.setDeviceManager(deviceManager);
        Library.setMemoryManagement(memoryManagement);
        Library.setScheduler(scheduler);
        Library.setInterruptRequestLine(interruptRequestLine);
        
        int cpuNum = 0;//待输入的cpu个数




        Scanner scanner = new Scanner(System.in);

        //用户选择系统CPU个数
        System.out.println("请预设系统CPU个数：");
        cpuNum = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符


        // 用户选择调度算法
        System.out.println("请选择进程调度算法：");
        System.out.println("1. FCFS");
        System.out.println("2. SJF");
        System.out.println("3. RR");
        System.out.println("4. PRIORITY");
        System.out.println("5. MLFQ（4个队列，优先级从高到低，时间片逐渐增多）");
        System.out.println("6. PRIORITY_Preemptive");
        System.out.print("输入数字选择：");
        int choice = 0;
        while (true) {
            try {
                choice = scanner.nextInt();
                if (choice >= 1 && choice <= 6) break;
                System.out.println("无效选择，请重新输入：");
            } catch (Exception e) {
                scanner.next();
            }
        }
        scanner.nextLine(); // 消耗换行符

        // 配置调度策略
        switch (choice) {
            case 1 -> scheduler.configure(Scheduler.SchedulingPolicy.FCFS);
            case 2 -> scheduler.configure(Scheduler.SchedulingPolicy.SJF);
            case 3 -> scheduler.configure(Scheduler.SchedulingPolicy.RR);
            case 4 -> scheduler.configure(Scheduler.SchedulingPolicy.PRIORITY);
            case 5 -> scheduler.configure(Scheduler.SchedulingPolicy.MLFQ);
            case 6 -> scheduler.configure(Scheduler.SchedulingPolicy.PRIORITY_Preemptive);
        }

        // 初始化 4 个 CPU
        for (int i = 0; i < cpuNum; i++) {
            CPU cpu = new CPU(i, scheduler, deviceManager, interruptRequestLine);
            scheduler.addCPU(cpu); // 将 CPU 注册到调度器
            cpu.start(); // 启动 CPU 线程
        }
        CPU.setCpuNum(cpuNum);// 设置当前cpu个数

        // 启动 Shell
        Shell shell = new Shell(fileSystem, memoryManagement, deviceManager);
        shell.start();
        Scheduler.getInstance().start();
    }
}
