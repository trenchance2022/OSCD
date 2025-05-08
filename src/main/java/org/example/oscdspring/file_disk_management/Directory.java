package org.example.oscdspring.file_disk_management;

import org.example.oscdspring.util.LogEmitterService;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Directory {
    String name;
    Directory parent;  // 父目录
    List<Inode> files;  // 存储目录下的文件
    List<Directory> subdirectories;  // 存储子目录

    public Directory(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.files = new ArrayList<>();
        this.subdirectories = new ArrayList<>();
    }

    // 创建文件
    public void createFile(Disk disk, String fileName, int fileSize, int inodeNumber) {
        if (fileSize > disk.getFreeSpace()) {
            LogEmitterService.getInstance().sendLog("Not enough space to create file " + fileName);
            return;
        }

        Inode inode = new Inode(inodeNumber, fileName, fileSize);
        int totalBlocksNeeded = (int) Math.ceil((double) fileSize / 1024);  // 计算所需的块数

        int remainingSize = fileSize;  // 记录还需要写入的字节数

        for (int i = 0; i < totalBlocksNeeded; i++) {
            int blockIndex = disk.allocateBlock();
            if (blockIndex != -1) {
                inode.blockIndexes[i] = blockIndex;

                // 计算本次应该写入的字节数（可能小于 1024）
                int bytesToWrite = Math.min(remainingSize, 1024);
                disk.writeRandomDataToDisk(blockIndex * 1024, bytesToWrite);

                remainingSize -= bytesToWrite;
            } else {
                LogEmitterService.getInstance().sendLog("Not enough space to create file " + fileName);
                return;
            }
        }

        files.add(inode);
        LogEmitterService.getInstance().sendLog("File " + fileName + " created with size " + fileSize + "B.");
    }


    // 创建子目录
    public void createDirectory(String directoryName) {
        // 检查是否已存在同名目录
        for (Directory dir : subdirectories) {
            if (dir.name.equals(directoryName)) {
                LogEmitterService.getInstance().sendLog("Directory " + directoryName + " already exists.");
                return;
            }
        }
        Directory dir = new Directory(directoryName, this);
        subdirectories.add(dir);
        LogEmitterService.getInstance().sendLog("Directory " + directoryName + " created.");
    }

    // 删除文件
    public void removeFile(Disk disk, String fileName) {
        for (int i = 0; i < files.size(); i++) {
            Inode inode = files.get(i);
            if (inode.fileName.equals(fileName)) {
                // 释放磁盘块
                for (int blockIndex : inode.blockIndexes) {
                    disk.freeBlock(blockIndex);
                }
                files.remove(i);
                LogEmitterService.getInstance().sendLog("File " + fileName + " deleted.");
                return;
            }
        }
        LogEmitterService.getInstance().sendLog("File " + fileName + " not found.");
    }

    // 删除空目录
    public void removeDirectory(String directoryName) {
        for (int i = 0; i < subdirectories.size(); i++) {
            Directory dir = subdirectories.get(i);
            if (dir.name.equals(directoryName)) {
                if (dir.files.isEmpty() && dir.subdirectories.isEmpty()) { // 确保目录为空
                    subdirectories.remove(i);
                    LogEmitterService.getInstance().sendLog("Directory " + directoryName + " deleted.");
                    return;
                } else {
                    LogEmitterService.getInstance().sendLog("Directory " + directoryName + " is not empty. Use rmrdir to remove non-empty directories.");
                    return;
                }
            }
        }
        LogEmitterService.getInstance().sendLog("Directory " + directoryName + " not found.");
    }

    // 递归删除目录及其所有内容
    public void removeDirectoryRecursively(Disk disk, String directoryName) {
        // 使用栈来迭代处理目录
        Stack<Directory> stack = new Stack<>();
        Directory targetDir = null;

        // 查找目标目录
        for (Directory dir : subdirectories) {
            if (dir.name.equals(directoryName)) {
                targetDir = dir;
                break;
            }
        }

        if (targetDir == null) {
            return;
        }

        // 将目标目录加入栈中
        stack.push(targetDir);

        // 迭代删除文件和子目录
        while (!stack.isEmpty()) {
            Directory currentDir = stack.pop();

            // 先删除当前目录中的所有文件
            for (Inode inode : currentDir.files) {
                for (int blockIndex : inode.blockIndexes) {
                    disk.freeBlock(blockIndex);
                }
            }

            // 将当前目录中的子目录加入栈中
            for (Directory subDir : currentDir.subdirectories) {
                stack.push(subDir);
            }

            // 删除当前目录
            subdirectories.remove(currentDir);
        }
    }

    // 查看目录内容
    public void listDirectory() {
        for (Inode inode : files) {
            LogEmitterService.getInstance().sendLog(inode.fileName + " ");
        }
        for (Directory dir : subdirectories) {
            LogEmitterService.getInstance().sendLog(dir.name + "/");
        }
    }

    // 切换到子目录
    public Directory changeDirectory(String directoryName) {
        for (Directory dir : subdirectories) {
            if (dir.name.equals(directoryName)) {
                return dir;
            }
        }
        LogEmitterService.getInstance().sendLog("Directory " + directoryName + " not found.");
        return null;
    }

    // 返回上级目录
    public Directory goBack() {
        return parent;
    }

    // 展示文件占用的磁盘块号
    public void showFileBlock(String fileName) {
        for (Inode inode : files) {
            if (inode.fileName.equals(fileName)) {
                LogEmitterService.getInstance().sendLog("File " + fileName + " occupies blocks: ");
                StringBuilder sb = new StringBuilder();
                for (int blockIndex : inode.blockIndexes) {
                    sb.append(blockIndex).append(" ");
                }
                LogEmitterService.getInstance().sendLog(sb.toString());
                return;
            }
        }
        LogEmitterService.getInstance().sendLog("File " + fileName + " not found.");
    }


    // 递归地展示目录结构
    public void showDirectoryStructure(String prefix) {
        LogEmitterService.getInstance().sendLog(prefix + name + "/");
        for (Inode inode : files) {
            LogEmitterService.getInstance().sendLog(prefix + "  " + inode.fileName);
        }
        for (Directory dir : subdirectories) {
            dir.showDirectoryStructure(prefix + "  ");
        }
    }

    public void appendDirectoryStructure(String prefix, boolean isLast, PrintWriter writer) {
        String connector = prefix.isEmpty() ? "" : (isLast ? "└── " : "├── ");
        writer.println(prefix + connector + name + "/");

        String childPrefix = prefix + (isLast ? "    " : "│   ");

        for (int i = 0; i < files.size(); i++) {
            boolean isLastFile = (i == files.size() - 1) && subdirectories.isEmpty();
            String fileConnector = isLastFile ? "└── " : "├── ";
            writer.println(childPrefix + fileConnector + files.get(i).fileName);
        }

        for (int j = 0; j < subdirectories.size(); j++) {
            boolean isLastDir = (j == subdirectories.size() - 1);
            subdirectories.get(j).appendDirectoryStructure(childPrefix, isLastDir, writer);
        }
    }


    public int[] getFileDiskBlock(String filename) {
        for (Inode inode : files) {
            if (inode.fileName.equals(filename)) {
                return inode.blockIndexes;
            }
        }
        return null;
    }
}
