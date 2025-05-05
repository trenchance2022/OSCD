package org.example.oscdspring.file_disk_management;

import java.util.List;

public interface FileDiskManagement {

        // 创建文件
        void createFile(String fileName, int fileSize);

        // 创建子目录
        void createDirectory(String directoryName);

        // 分配一个块
        int allocateBlock();

        // 释放一个块
        void freeBlock(int blockIndex);

        byte[] readBlock(int blockAddress);

        void writeBlock(int blockAddress, byte[] data);

        // 列出当前目录内容
        void listDirectory();

        // 切换目录
        void changeDirectory(String directoryName);

        // 返回上级目录
        void goBack();

        // 删除文件
        void removeFile(String fileName);

        // 删除空目录
        void removeDirectory(String directoryName);

        // 递归删除目录及其所有内容
        void removeDirectoryRecursively(String directoryName);

        // 展示文件占用的磁盘块
        void showFileBlock(String fileName);

        // 显示磁盘信息
        void displayDiskInfo();

        // 展示目录结构
        void showDirectoryStructure();

        // 获取当前路径
        String getCurrentPath();

        // 编辑文件内容
        void editFile(String fileName, String newContent);

        // 展示文件内容
        void showFileData(String fileName);

        // 读取文件内容
        String readFileData(String fileName);

        //返回文件占用磁盘块号
        int[] getFileDiskBlock(String filename);

        // 返回磁盘占用块号
        List<Integer> getOccupiedBlockIndices();

}
