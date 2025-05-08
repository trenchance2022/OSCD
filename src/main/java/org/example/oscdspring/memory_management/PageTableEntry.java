package org.example.oscdspring.memory_management;

public class PageTableEntry {

    private int frameNumber; // 物理块号
    private boolean valid;       // 有效位，表示页面是否在内存中
    private boolean dirty;      // 修改位
    private boolean accessed;      // 访问字段，用于页面置换算法
    private int diskAddress;         // 外存地址，-1表示不在外存中。
    private boolean allocatedDisk; // 是否已经分配外存地址

    public PageTableEntry(int diskAddress) {
        this.diskAddress = diskAddress;
        this.valid = false; // 初始时页面不在内存中
        this.accessed = false;
        this.dirty = false;
        this.allocatedDisk = false;
    }

    @Override
    public PageTableEntry clone() {
        PageTableEntry pageTableEntry = new PageTableEntry(this.diskAddress);
        pageTableEntry.frameNumber = this.frameNumber;
        pageTableEntry.valid = this.valid;
        pageTableEntry.dirty = this.dirty;
        pageTableEntry.accessed = this.accessed;
        pageTableEntry.allocatedDisk = this.allocatedDisk;
        return pageTableEntry;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PageTableEntry that = (PageTableEntry) obj;
        return frameNumber == that.frameNumber && valid == that.valid && dirty == that.dirty && accessed == that.accessed && diskAddress == that.diskAddress && allocatedDisk == that.allocatedDisk;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public boolean isAllocatedDisk() {
        return allocatedDisk;
    }

    public void AllocatedDisk() {
        this.allocatedDisk = true;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public void setDiskAddress(int diskAddress) {
        this.diskAddress = diskAddress;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isAccessed() {
        return accessed;
    }

    public void setAccessed(boolean accessed) {
        this.accessed = accessed;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public int getDiskAddress() {
        return diskAddress;
    }
}
