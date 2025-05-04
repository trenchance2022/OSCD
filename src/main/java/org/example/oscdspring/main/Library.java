package org.example.oscdspring.main;
import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.memory_management.MemoryManagement;
import org.example.oscdspring.process_management.Scheduler;


public class Library {
    //文件管理系统
    private static FileSystemImpl fileSystem;
    //内存管理系统
    private static MemoryManagement memoryManagement;
    //设备管理系统
    private static DeviceManager deviceManager;
    //调度器
    private static Scheduler scheduler;

    public static FileSystemImpl getFileSystem() {
        if(fileSystem== null){
            fileSystem = new FileSystemImpl();
        }
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

}
