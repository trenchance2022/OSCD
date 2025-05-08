package org.example.oscdspring.file_disk_management;

import org.example.oscdspring.main.Constants;
import org.example.oscdspring.util.LogEmitterService;

import java.util.List;
import java.util.Random;

class Disk {
    private int diskSize = Constants.DISK_SIZE;  // 1MB磁盘，块大小为1KB，总共有1024个块
    private int blockSize = Constants.BLOCK_SIZE_BYTES;  // 块大小为1KB
    private Bitmap bitmap;        // 位图管理空闲块
    private byte[] diskData;      // 模拟磁盘的数据数组

    private static final Disk INSTANCE = new Disk();

    private Disk() {
        this.bitmap = new Bitmap(diskSize);
        this.diskData = new byte[diskSize * blockSize];
    }

    public static Disk getInstance() {
        return INSTANCE;
    }

    public int allocateBlock() {
        int blockIndex = bitmap.allocateBlock();
        if (blockIndex == -1) {
            LogEmitterService.getInstance().sendLog("No free disk block available.");
            return -1;
        }
        return blockIndex;
    }

    public void freeBlock(int blockIndex) {
        bitmap.freeBlock(blockIndex);
    }

    // 在磁盘上填充指定大小的随机字符串
    public void writeRandomDataToDisk(int startIndex, int sizeInBytes) {
        Random rand = new Random();
        for (int i = 0; i < sizeInBytes; i++) {
            diskData[startIndex + i] = (byte) (rand.nextInt(26) + 97);  // 生成随机字母 a-z
        }
    }

    // 在磁盘上填充指定大小的字节数据（用户输入内容）
    public void writeDataToDisk(int startIndex, byte[] data) {
        System.arraycopy(data, 0, diskData, startIndex, data.length);
    }

    public String readDataFromDisk(int startIndex, int sizeInBytes) {
        StringBuilder fileContent = new StringBuilder();
        for (int i = 0; i < sizeInBytes; i++) {
            fileContent.append((char) diskData[startIndex + i]);
        }
        return fileContent.toString();
    }

    public byte[] readBlock(int blockAddress) {
        byte[] data = new byte[1024];
        System.arraycopy(diskData, blockAddress * 1024, data, 0, 1024);
        return data;
    }

    public void writeBlock(int blockAddress, byte[] data) {
        System.arraycopy(data, 0, diskData, blockAddress * 1024, 1024);
    }

    // 显示磁盘的空闲块情况
    public void displayDiskInfo() {
        LogEmitterService.getInstance().sendLog("Occupied blocks: " + bitmap.getOccupiedBlocks() + ", Free blocks: " + bitmap.getFreeBlocks());
        bitmap.displayBitmap();
    }

    // 获取磁盘的占用块号
    public List<Integer> getOccupiedBlockIndices() {
        return bitmap.getOccupiedBlockIndices();
    }

    public int getOccupiedBlocks() {
        return bitmap.getOccupiedBlocks();
    }

    public int getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(int diskSize) {
        this.diskSize = diskSize;
    }

    public int getFreeSpace() {
        return bitmap.getFreeBlocks() * 1024;
    }

    public byte[] getDiskData() {
        return diskData;
    }


}
