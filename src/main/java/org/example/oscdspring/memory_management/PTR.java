package org.example.oscdspring.memory_management;

import org.example.oscdspring.process_management.PCB;

public class PTR {
    private int pageTableAddress;//页表地址
    private int pageTableSize;//页表大小
    private int codeSize;//代码段大小
    private int innerFragmentation;//内部碎片(代码段与数据段之间的碎片)
    private int lastPageSize;//数据段最后一页的大小

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PTR ptr = (PTR) obj;
        return pageTableAddress == ptr.getPageTableAddress()
                && pageTableSize == ptr.getPageTableSize()
                && codeSize == ptr.getCodeSize()
                && innerFragmentation == ptr.getInnerFragmentation()
                && lastPageSize == ptr.getLastPageSize();
    }

    @Override
    public PTR clone() {
        PTR ptr = new PTR();
        ptr.pageTableAddress = pageTableAddress;
        ptr.pageTableSize = pageTableSize;
        ptr.codeSize = codeSize;
        ptr.innerFragmentation = innerFragmentation;
        ptr.lastPageSize = lastPageSize;
        return ptr;
    }

    public void update(PCB pcb) {
        pageTableAddress = pcb.getPageTableAddress();
        pageTableSize = pcb.getPageTableSize();
        codeSize = pcb.getCodeSize();
        innerFragmentation = pcb.getInnerFragmentation();
        lastPageSize = pcb.getLastPageSize();
    }

    public int getPageTableAddress() {
        return pageTableAddress;
    }

    public int getPageTableSize() {
        return pageTableSize;
    }

    public int getCodeSize() {
        return codeSize;
    }

    public int getInnerFragmentation() {
        return innerFragmentation;
    }

    public void addPageSize(int pageSize) {
        pageTableSize += pageSize;
    }

    public int getLastPageSize() {
        return lastPageSize;
    }

    public void setLastPageSize(int lastPageSize) {
        this.lastPageSize = lastPageSize;
    }
}

