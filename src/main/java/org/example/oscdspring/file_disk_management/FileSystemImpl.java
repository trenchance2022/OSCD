package org.example.oscdspring.file_disk_management;

import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.example.oscdspring.util.LogEmitterService;
import org.springframework.beans.factory.annotation.Autowired;


@Component
public class FileSystemImpl implements FileDiskManagement {
    public static Disk disk;
    public static Directory root;
    public static Directory currentDirectory;

    @Autowired
    private LogEmitterService logEmitterService;

    public FileSystemImpl() {
        disk = Disk.getInstance();
        root = new Directory("root", null);
        currentDirectory = root;
    }

    // 创建文件
    public void createFile(String fileName, int fileSize) {
        // 检查是否已存在同名文件
        for (Inode inode : currentDirectory.files) {
            if (inode.fileName.equals(fileName)) {
                logEmitterService.sendLog("File " + fileName + " already exists.");
                return;
            }
        }
        int inodeNumber = currentDirectory.files.size() + 1;  // 生成新的inode编号
        currentDirectory.createFile(disk, fileName, fileSize, inodeNumber);
    }

    // 创建子目录
    public void createDirectory(String directoryName) {
        currentDirectory.createDirectory(directoryName);
    }

    // 分配一个块
    public int allocateBlock() {
        return disk.allocateBlock();
    }

    // 释放一个块
    public void freeBlock(int blockIndex) {
        disk.freeBlock(blockIndex);
    }

    public byte[] readBlock(int blockAddress) {
        return disk.readBlock(blockAddress);
    }

    public void writeBlock(int blockAddress, byte[] data) {
        disk.writeBlock(blockAddress, data);
    }

    // 列出当前目录内容
    public void listDirectory() {
        currentDirectory.listDirectory();
    }

    // 切换目录
    public void changeDirectory(String directoryName) {
        if (directoryName.equals("/")) {
            currentDirectory = root;
            return;
        }
        if (directoryName.startsWith("/")) {  // 绝对路径
            currentDirectory = root;
            directoryName = directoryName.substring(1);  // 去掉路径前的 '/'
        }

        String[] dirs = directoryName.split("/");
        for (String dir : dirs) {
            Directory newDir = currentDirectory.changeDirectory(dir);
            if (newDir != null) {
                currentDirectory = newDir;
            } else {
                return;
            }
        }
    }

    // 返回上级目录
    public void goBack() {
        Directory newDir = currentDirectory.goBack();
        if (newDir != null) {
            currentDirectory = newDir;
        }
    }

    // 删除文件
    public void removeFile(String fileName) {
        currentDirectory.removeFile(disk, fileName);
    }

    // 删除空目录
    public void removeDirectory(String directoryName) {
        currentDirectory.removeDirectory(directoryName);
    }

    // 递归删除目录及其所有内容
    public void removeDirectoryRecursively(String directoryName) {
        currentDirectory.removeDirectoryRecursively(disk, directoryName);
    }

    // 展示文件占用的磁盘块
    public void showFileBlock(String fileName) {
        currentDirectory.showFileBlock(fileName);
    }

    // 显示磁盘信息
    public void displayDiskInfo() {
        disk.displayDiskInfo();
    }

    // 展示目录结构
    public void showDirectoryStructure() {
        root.showDirectoryStructure("");
    }

    // 获取当前路径
    public String getCurrentPath() {
        StringBuilder path = new StringBuilder();
        Directory temp = currentDirectory;
        while (temp != null) {
            path.insert(0, "/" + temp.name);
            temp = temp.parent;
        }
        return path.toString();
    }

    // 编辑文件内容
    public void editFile(String fileName, String newContent) {
        // 找到文件对应的 Inode，进行编辑
        Inode fileInode = null;
        for (Inode inode : currentDirectory.files) {
            if (inode.fileName.equals(fileName)) {
                fileInode = inode;
                break;
            }
        }
        if (fileInode == null) {
            logEmitterService.sendLog("File " + fileName + " not found.");
            return;
        }


        // 更新文件内容
        byte[] newData = newContent.getBytes();
        int blocksRequired = (int) Math.ceil((double) newData.length / 1024);
        int index = 0;

        if (disk.getOccupiedBlocks() + blocksRequired > disk.getDiskSize()) {
            logEmitterService.sendLog("Not enough space on disk.");
            return;
        }

        // 回收旧的块
        for (int i = 0; i < fileInode.blockIndexes.length; i++) {
            disk.freeBlock(fileInode.blockIndexes[i]);
        }

        // 分配新块并写入数据
        fileInode.blockIndexes = new int[blocksRequired];
        for (int i = 0; i < blocksRequired; i++) {
            int blockIndex = disk.allocateBlock();
            if (blockIndex != -1) {
                fileInode.blockIndexes[i] = blockIndex;
                int remainingData = Math.min(newData.length - index, 1024);  // 剩余数据
                disk.writeDataToDisk(blockIndex * 1024, Arrays.copyOfRange(newData, index, index + remainingData));
                index += remainingData;
            }
        }

        fileInode.size = newData.length;  // 更新文件大小
        logEmitterService.sendLog("File " + fileName + " has been updated.");
    }

    public boolean fileExists(String fileName) {
        if (currentDirectory == null || currentDirectory.files == null) {
            return false;
        }
        for (Inode inode : currentDirectory.files) {
            if (inode.fileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public void showFileData(String fileName) {
        for (Inode inode : currentDirectory.files) {
            if (inode.fileName.equals(fileName)) {
                StringBuilder contentBuilder = new StringBuilder();
                for (int i = 0; i < inode.blockIndexes.length; i++) {
                    int blockIndex = inode.blockIndexes[i];
                    int bytesToRead = (i == inode.blockIndexes.length - 1) ? inode.size % 1024 : 1024;
                    String fileContent = disk.readDataFromDisk(blockIndex * 1024, bytesToRead);
                    contentBuilder.append(fileContent);
                }
                // 将累积的内容整体发送
                logEmitterService.sendLog(contentBuilder.toString());
                return;
            }
        }
        logEmitterService.sendLog("File " + fileName + " not found.");
    }


    public String readFileData(String fileName) {
        StringBuilder fileContent = new StringBuilder();
        for (Inode inode : currentDirectory.files) {
            if (inode.fileName.equals(fileName)) {
                for (int i = 0; i < inode.blockIndexes.length; i++) {
                    int blockIndex = inode.blockIndexes[i];
                    int bytesToRead = (i == inode.blockIndexes.length - 1) ? inode.size % 1024 : 1024;
                    String content = disk.readDataFromDisk(blockIndex * 1024, bytesToRead);
                    fileContent.append(content);
                }
                return fileContent.toString();  // 返回拼接好的文件内容
            }
        }

        // 文件未找到时，返回提示信息
        return "-1";
    }

    public int[] getFileDiskBlock(String filename){
        return currentDirectory.getFileDiskBlock(filename);
    }

}
