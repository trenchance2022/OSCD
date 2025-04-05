package main;
import device_management.DeviceManager;
import file_disk_management.FileSystemImpl;
import interrupt_management.InterruptRequestLine;
import lombok.Data;
import lombok.Getter;
import memory_management.MemoryManagement;
import process_management.Scheduler;


public class Library {
    //文件管理系统
    private static FileSystemImpl fileSystem;
    //内存管理系统
    private static MemoryManagement memoryManagement;
    //设备管理系统
    private static DeviceManager deviceManager;
    //调度器
    private static Scheduler scheduler;
    //中断请求线
    private static InterruptRequestLine interruptRequestLine;

    public static FileSystemImpl getFileSystem() {
        return fileSystem;
    }

    public static void setFileSystem(FileSystemImpl fileSystem) {
        Library.fileSystem = fileSystem;
    }

    public static MemoryManagement getMemoryManagement() {
        return memoryManagement;
    }

    public static void setMemoryManagement(MemoryManagement memoryManagement) {
        Library.memoryManagement = memoryManagement;
    }

    public static DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public static void setDeviceManager(DeviceManager deviceManager) {
        Library.deviceManager = deviceManager;
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static void setScheduler(Scheduler scheduler) {
        Library.scheduler = scheduler;
    }

    public static InterruptRequestLine getInterruptRequestLine() {
        return interruptRequestLine;
    }

    public static void setInterruptRequestLine(InterruptRequestLine interruptRequestLine) {
        Library.interruptRequestLine = interruptRequestLine;
    }

}
