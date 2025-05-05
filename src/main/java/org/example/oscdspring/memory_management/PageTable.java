package org.example.oscdspring.memory_management;

import org.example.oscdspring.util.LogEmitterService;

import java.util.ArrayList;
import java.util.List;

public class PageTable {

    private final List<PageTableEntry> entries; // 页表项
    private final int pid;//进程号
    private final int memoryBlockSize;//实际分配的内存块数
    private int memoryBlockUsed;//内存已使用大小
    private int pointer;// 二次机会算法指针

    public PageTable(int pid, int pageTableSize, int[] diskAddressBlock) {// 用于初始化页表，传入进程号，进程大小，磁盘地址，在加载进程时调用
        this.pid = pid;
        entries = new ArrayList<>(pageTableSize);
        // 初始化页表项
        for (int i = 0; i < pageTableSize-1; i++) {
            entries.add(new PageTableEntry(diskAddressBlock[i]));
        }
        entries.add(new PageTableEntry(-1));//数据段页表项
        memoryBlockUsed = 0;
        memoryBlockSize = 4;

    }

    @Override
    public PageTable clone(){
        PageTable pageTable = new PageTable(this.pid, this.entries.size(), new int[this.entries.size()]);
        for (int i = 0; i < this.entries.size(); i++) {
            pageTable.entries.set(i, (PageTableEntry) this.entries.get(i).clone());
        }
        pageTable.memoryBlockUsed = this.memoryBlockUsed;
        pageTable.pointer = this.pointer;
        return pageTable;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PageTable pageTable = (PageTable) obj;
        return pid == pageTable.pid && memoryBlockUsed == pageTable.memoryBlockUsed && pointer == pageTable.pointer && entries.equals(pageTable.entries);
    }

    public boolean hasValidPage() {
        for (PageTableEntry entry : entries) {
            if (entry.isValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEmptyFrame() {
        return memoryBlockUsed < memoryBlockSize;
    }

    public void addMemoryUsed() {
        memoryBlockUsed++;
    }

    public int getReplacePage() {//使用改进的clock算法
        pointer = (pointer + 1) % entries.size();//指针指向下一个页表项

        for (int i = 0; i < entries.size(); i++) {
            // 第一次扫描，找到访问位为false,修改位为false的页
            if (entries.get(pointer).isValid()) {
                if (!entries.get(pointer).isAccessed() && !entries.get(pointer).isDirty()) {
                    return pointer;
                }
            }
            pointer = (pointer + 1) % entries.size();
        }
        for (int i = 0; i < entries.size(); i++) {
            // 第二次扫描，找到访问位为true,修改位为false的页,并将访问位设置为false
            if (entries.get(pointer).isValid()) {
                if (entries.get(pointer).isAccessed() && !entries.get(pointer).isDirty()) {
                    return pointer;
                }
                entries.get(pointer).setAccessed(false);
            }
            pointer = (pointer + 1) % entries.size();
        }
        for (int i = 0; i < entries.size(); i++) {
            // 第三次扫描，找到访问位为false,修改位为false的页(第二次扫描后所有页的访问位��为false)
            if (entries.get(pointer).isValid()) {
                if (!entries.get(pointer).isDirty()) {
                    return pointer;
                }
            }
            pointer = (pointer + 1) % entries.size();
        }
        for (int i = 0; i < entries.size(); i++) {
            // 第四次扫描，找到访问位为true,修改位为true的页,此时所有页的访问位都为false，修改位都为true
            if (entries.get(pointer).isValid()) {
                return pointer;
            }
            pointer = (pointer + 1) % entries.size();
        }
        LogEmitterService.getInstance().sendLog("进程未使用物理内存");
        return -1;
    }

    public int getPageTableSize() {
        return entries.size();
    }

    // 获取页表项
    public PageTableEntry getEntry(int pageNumber, boolean dirty) {
        if (pageNumber < 0 || pageNumber >= entries.size()) {
            LogEmitterService.getInstance().sendLog("页表项不存在: " + pageNumber);
            LogEmitterService.getInstance().sendLog("页表大小: " + entries.size());
            return null;
        }
        if (entries.get(pageNumber).isValid()) {
            entries.get(pageNumber).setAccessed(true);
            if (dirty) {
                entries.get(pageNumber).setDirty(true);
            }
        }
        return entries.get(pageNumber);
    }

    // 添加页表项
    public void addEntry() {
        PageTableEntry entry = new PageTableEntry(-1);
        entries.add(entry);
    }
    public void addEntries(int size) {
        for (int i = 0; i < size; i++) {
            addEntry();
        }
    }

    // 获取进程号
    public int getPid() {
        return pid;
    }
}
