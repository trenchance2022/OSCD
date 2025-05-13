package org.example.oscdspring.memory_management;

import org.example.oscdspring.main.Constants;

// 这里使用Clock算法来替换TLB中的映射关系
public class TLB {
    private final TLBEntry[] TLB = new TLBEntry[Constants.TLB_SIZE];
    private int clockHand = 0;//指针，指向下一个要替换的TLB项

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TLB tlb = (TLB) obj;
        for (int i = 0; i < Constants.TLB_SIZE; i++) {
            if (!TLB[i].equals(tlb.TLB[i])) {
                return false;
            }
        }
        return clockHand == tlb.clockHand;
    }

    @Override
    public TLB clone() {
        TLB tlb = new TLB();
        for (int i = 0; i < Constants.TLB_SIZE; i++) {
            tlb.TLB[i] = (TLBEntry) TLB[i].clone();
        }
        tlb.clockHand = clockHand;
        return tlb;
    }

    public boolean isEmpty() {
        for (TLBEntry entry : TLB) {
            if (entry.isValid()) {
                return false;
            }
        }
        return true;
    }

    public TLB() {
        for (int i = 0; i < Constants.TLB_SIZE; i++) {
            TLB[i] = new TLBEntry();
        }
    }

    // 添加映射关系，如果TLB已满，用Clock算法替换
    public void addEntry(int pageNumber, int frameNumber) {
        if(Constants.TLB_SIZE==0) return;

        // 先替换空的（无效的
        for (int i = 0; i < Constants.TLB_SIZE; i++) {
            if (!TLB[i].isValid()) {
                TLB[i] = new TLBEntry(pageNumber, frameNumber);
                return;
            }
        }
        // 如果TLB都是有效的，就用Clock算法替换
        while (true) {
            if (TLB[clockHand].isAccessed()) {
                TLB[clockHand].setAccessed(false);
                clockHand = (clockHand + 1) % Constants.TLB_SIZE;
            } else {
                TLB[clockHand] = new TLBEntry(pageNumber, frameNumber);
                clockHand = (clockHand + 1) % Constants.TLB_SIZE;
                break;
            }
        }

    }

    // 实际上用map可能更快？这里主要是看硬件实现，软件上我们就用数组模拟了
    public int getFrameNumber(int pageNumber, boolean dirty) {
        for (TLBEntry entry : TLB) {
            if (entry.isValid() && entry.getPageNumber() == pageNumber) {
                entry.setAccessed(true);//更新访问位
                if (dirty) {//如果写操作，设置修改位
                    entry.setDirty(true);
                }
                return entry.getFrameNumber();
            }
        }

        // TLB未命中,返回-1
        return -1;
    }

    public int getPageNumber(int i){
        if (i < 0 || i >= Constants.TLB_SIZE) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return TLB[i].getPageNumber();
    }
    public boolean getAccessed(int i){
        if (i < 0 || i >= Constants.TLB_SIZE) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return TLB[i].isAccessed();
    }

    // 删除TLB中的映射关系
    public void deleteEntry(int pageNumber) {
        for (TLBEntry entry : TLB) {
            if (entry.isValid() && entry.getPageNumber() == pageNumber) {
                entry.setValid(false);
                return;
            }
        }
    }

    // 刷新TLB,切换进程时调用
    public void refresh() {
        for (TLBEntry entry : TLB) {
            entry.setValid(false);
        }
    }

    // TLB写回页表，主要是更新页表项的修改位,访问位设置为true，
    public void writeBackPageTable(PageTable pageTable) {
        for (TLBEntry entry : TLB) {
            if (entry.isValid()) {
                PageTableEntry pageTableEntry = pageTable.getEntry(entry.getPageNumber(), false);
                if (entry.isDirty()) {
                    pageTableEntry.setDirty(true);
                }
                pageTableEntry.setAccessed(true);
            }

        }
    }

}

class TLBEntry {
    private int pageNumber;
    private int frameNumber;
    private boolean valid;//是否有效
    private boolean dirty;//是否被修改过
    private boolean accessed;//用于clock算法，是否被访问过

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TLBEntry tlbEntry = (TLBEntry) obj;
        return pageNumber == tlbEntry.pageNumber
                && frameNumber == tlbEntry.frameNumber
                && valid == tlbEntry.valid
                && dirty == tlbEntry.dirty
                && accessed == tlbEntry.accessed;
    }

    @Override
    public TLBEntry clone() {
        TLBEntry tlbEntry = new TLBEntry();
        tlbEntry.pageNumber = pageNumber;
        tlbEntry.frameNumber = frameNumber;
        tlbEntry.valid = valid;
        tlbEntry.dirty = dirty;
        tlbEntry.accessed = accessed;
        return tlbEntry;
    }

    public TLBEntry() {
        this.valid = false;
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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }


    public TLBEntry(int pageNumber, int frameNumber) {
        this.pageNumber = pageNumber;
        this.frameNumber = frameNumber;
        this.valid = true;
        this.dirty = false;
        this.accessed = true;
    }

}

