package file_disk_management;

import main.Constants;

import java.util.Random;

class Disk {
    private int diskSize = Constants.DISK_SIZE;  // 1MB磁盘，块大小为1KB，总共有1024个块
    private int blockSize = Constants.BLOCK_SIZE_BYTES;  // 块大小为1KB
    private Bitmap bitmap;        // 位图管理空闲块
    private byte[] diskData;      // 模拟磁盘的数据数组
    private int occupiedBlocks = 0;  // 已分配的块数

    // 使用单例模式，私有化构造方法
    private static final Disk INSTANCE = new Disk();

    private Disk() {
        this.bitmap = new Bitmap(diskSize);  // 创建位图
        this.diskData = new byte[diskSize * blockSize];  // 创建磁盘数据数组
    }

    // 提供公共的静态方法来获取唯一实例
    public static Disk getInstance() {
        return INSTANCE;
    }

    // 分配一个块
    public int allocateBlock() {
        return bitmap.allocateBlock();
    }

    // 释放一个块
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

    // 读取文件数据（用于后续查看文件内容）
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
        System.out.println("Occupied blocks: " + occupiedBlocks + ", Free blocks: " + (diskSize - occupiedBlocks));
        bitmap.displayBitmap();
    }

    public int getOccupiedBlocks() {
        return occupiedBlocks;
    }

    public int getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(int diskSize) {
        this.diskSize = diskSize;
    }

    public void setOccupiedBlocks(int occupiedBlocks) {
        this.occupiedBlocks = occupiedBlocks;
    }

    public int getFreeSpace() {
        return (diskSize - occupiedBlocks) * 1024;
    }

    public byte[] getDiskData() {
        return diskData;
    }
}
