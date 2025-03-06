package main;

import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import memory_management.MemoryManagement;
import memory_management.MemoryManagementImpl;

public class Main {
    public static void main(String[] args) {
        FileDiskManagement fileDiskManagement = new FileSystemImpl();
        MemoryManagement memoryManagement = new MemoryManagementImpl();
        Shell shell = new Shell(fileDiskManagement, memoryManagement);
        shell.start();
    }
}
