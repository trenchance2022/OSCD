package org.example.oscdspring.memory_management;

import org.example.oscdspring.main.Constants;
import org.example.oscdspring.process_management.PIDBitmap;
import org.example.oscdspring.util.LogEmitterService;

public class Memory {
    byte[][] memoryData = new byte[Constants.MEMORY_PAGE_SIZE][Constants.PAGE_SIZE_BYTES];
    MemoryBlockStatus[] blockStatus = new MemoryBlockStatus[Constants.MEMORY_PAGE_SIZE];

    private static final Memory INSTANCE = new Memory();

    private Memory() {
        int i = 0;
        while (i < Constants.SYSTEM_MEMORY_PAGE_SIZE) {
            blockStatus[i] = new MemoryBlockStatus(i, PIDBitmap.SYSTEM_PID);
            i++;
        }
        while (i < Constants.MEMORY_PAGE_SIZE) {
            blockStatus[i] = new MemoryBlockStatus(i, PIDBitmap.EMPTY_PID);
            i++;
        }
    }

    public static Memory getInstance() {
        return INSTANCE;
    }

    //找到一个空闲块,用于缺页中断处理
    public int findEmptyBlock() {
        for (int i = Constants.SYSTEM_MEMORY_PAGE_SIZE; i < Constants.MEMORY_PAGE_SIZE; i++) {
            if (blockStatus[i].getPid() == PIDBitmap.EMPTY_PID) {
                return i;
            }
        }
        return -1;
    }

    // 写入一个块,返回是否成功,用于缺页中断处理,从磁盘读取数据
    public void writeBlock(int blockNumber, byte[] data, int pid, int pageId) {
        this.memoryData[blockNumber] = data;
        blockStatus[blockNumber].setPid(pid);
        blockStatus[blockNumber].setPageId(pageId);
    }

    // 读取一个块，返回数据，用于缺页中断处理,将数据写入磁盘
    public byte[] readBlock(int blockNumber) {
        if (blockNumber < Constants.SYSTEM_MEMORY_PAGE_SIZE) {
            return null;
        }
        if (blockStatus[blockNumber].getPid() == PIDBitmap.EMPTY_PID) {
            return null;
        }
        byte[] data = new byte[Constants.PAGE_SIZE_BYTES];
        System.arraycopy(this.memoryData[blockNumber], 0, data, 0, Constants.PAGE_SIZE_BYTES);
        return data;
    }

    // 删除一个块，返回是否成功，用于缺页中断处理和MemoryManagementImpl释放进程内存
    public void freeBlock(int blockNumber) {
        blockStatus[blockNumber].setPid(PIDBitmap.EMPTY_PID);
    }

    // 读取数据，由MemoryManagementImpl调用
    public byte[] read(int physicalAddress, int length) {
        byte[] data = new byte[length];
        int pageId = physicalAddress / Constants.PAGE_SIZE_BYTES;
        int offset = physicalAddress % Constants.PAGE_SIZE_BYTES;
        int firstPageReadSize = Math.min(Constants.PAGE_SIZE_BYTES - offset, length);
        System.arraycopy(memoryData[pageId], offset, data, 0, firstPageReadSize);
        int remainBlock = (length - firstPageReadSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            System.arraycopy(memoryData[pageId + i + 1], 0, data, firstPageReadSize + i * Constants.PAGE_SIZE_BYTES, Constants.PAGE_SIZE_BYTES);
        }
        int lastPageReadSize = length - firstPageReadSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        if (lastPageReadSize > 0)
            System.arraycopy(memoryData[pageId + remainBlock + 1], 0, data, firstPageReadSize + remainBlock * Constants.PAGE_SIZE_BYTES, lastPageReadSize);
        return data;
    }

    // 写入数据，由MemoryManagementImpl调用
    public void write(int physicalAddress, int length, byte[] data) {
        int pageId = physicalAddress / Constants.PAGE_SIZE_BYTES;
        int offset = physicalAddress % Constants.PAGE_SIZE_BYTES;
        int firstPageWriteSize = Math.min(Constants.PAGE_SIZE_BYTES - offset, length);
        System.arraycopy(data, 0, memoryData[pageId], offset, firstPageWriteSize);
        int remainBlock = (length - firstPageWriteSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            System.arraycopy(data, firstPageWriteSize + i * Constants.PAGE_SIZE_BYTES, memoryData[pageId + i + 1], 0, Constants.PAGE_SIZE_BYTES);
        }
        int lastPageWriteSize = length - firstPageWriteSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        if (lastPageWriteSize > 0)
            System.arraycopy(data, firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES, memoryData[pageId + remainBlock + 1], 0, lastPageWriteSize);

    }

    // 展示内存使用情况，从start到end，这里直接输出到控制台，由MemoryManagementImpl调用
    public void showPageUse(int start, int end) {
        start = Math.max(start, 0);
        end = Math.min(end, Constants.MEMORY_PAGE_SIZE);
        LogEmitterService.getInstance().sendLog("Memory Page Use:");
        for (int i = start; i < end; i++) {
            LogEmitterService.getInstance().sendLog("Page " + i + " pid: " + blockStatus[i].getPid() + " pageId:" + blockStatus[i].getPageId());
        }
    }

}

// 记录内存块的状态，包括物理页号，进程号，页号，是否有效，用于打印内存使用情况
class MemoryBlockStatus {

    final int frameNumber;//物理页号
    int pid;//被分配的进程的id，
    int pageId;//对应进程虚拟页号


    public MemoryBlockStatus(int frameNumber, int pid) {
        this.frameNumber = frameNumber;
        this.pid = pid;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

}
