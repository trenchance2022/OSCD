package org.example.oscdspring.memory_management;

import org.example.oscdspring.main.Constants;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.util.LogEmitterService;

import static org.example.oscdspring.main.Library.getFileSystem;

// 每个CPU都有一个MMU，MMU负责地址转换，内含TLB缓冲区，页表寄存器
public class MMU {

    private final PTR ptr;//页表寄存器
    private final TLB tlb;//TLB缓冲区

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MMU mmu = (MMU) obj;
        return ptr.equals(mmu.ptr) && tlb.equals(mmu.tlb);
    }

    @Override
    public MMU clone() {
        return new MMU(this.ptr.clone(), this.tlb.clone());
    }

    public MMU() {
        this.ptr = new PTR();
        this.tlb = new TLB();
    }

    public MMU(PTR ptr, TLB tlb) {
        this.ptr = ptr;
        this.tlb = tlb;
    }

    // 切换进程时调用，更新页表寄存器和TLB
    public void update(PCB pcb) {
        ptr.update(pcb);
        tlb.refresh();
    }

    // 更新寄存器的页表大小，分配内存时会调用
    public void addPageSize(int pageSize) {
        ptr.addPageSize(pageSize);
    }

    public void setLastPageSize(int lastPageSize) {
        ptr.setLastPageSize(lastPageSize);
    }

    public int getLastPageSize() {
        return ptr.getLastPageSize();
    }

    public int getPageNumber(int logicalAddress) {
        if (logicalAddress < 0) {
            LogEmitterService.getInstance().sendLog("逻辑地址小于0 MMU");
            return -1;
        }

        // 更新逻辑地址:如果访问的是代码段之后数据段，逻辑地址加上内部碎片
        if (logicalAddress >= ptr.getCodeSize()) {
            logicalAddress += ptr.getInnerFragmentation();
        }

        return logicalAddress / Constants.PAGE_SIZE_BYTES;
    }

    // 转换逻辑地址为物理地址，如果缺页，会调用PageFaultHandler处理，然后重新运行函数
    public int addressTranslation(int logicalAddress, boolean dirty) {
        int logicalAddress1 = logicalAddress;

        if (logicalAddress < 0) {
            LogEmitterService.getInstance().sendLog("逻辑地址小于0 MMU");
            return -1;
        }

        // 更新逻辑地址:如果访问的是代码段之后数据段，逻辑地址加上内部碎片
        if (logicalAddress >= ptr.getCodeSize()) {
            logicalAddress += ptr.getInnerFragmentation();
        }

        int pageNumber = logicalAddress / Constants.PAGE_SIZE_BYTES;
        int offset = logicalAddress % Constants.PAGE_SIZE_BYTES;
        if (pageNumber >= ptr.getPageTableSize() || pageNumber == ptr.getPageTableSize() - 1 && offset >= ptr.getLastPageSize()) {
            LogEmitterService.getInstance().sendLog("逻辑地址超出范围");
            return -2;
        }

        // 先查TLB
        int frameNumber = tlb.getFrameNumber(pageNumber, dirty);

        if (frameNumber == -1) {// TLB未命中,查页表
            // 用tlb更新页表
            PageTable pageTable = PageTableArea.getInstance().getPageTable(ptr.getPageTableAddress());

            tlb.writeBackPageTable(pageTable);

            // 查找页表
            PageTableEntry entry = pageTable.getEntry(pageNumber, dirty);
            if (!entry.isValid()) {//页表项不存在,缺页中断
                if (!PageFaultHandler.handlePageFault(ptr.getPageTableAddress(), tlb, pageNumber)) {
                    LogEmitterService.getInstance().sendLog("缺页中断处理失败");
                    return -3;
                }
                return addressTranslation(logicalAddress1, dirty);//中断处理完毕后重新读取
            }

            // 页表项有效,更新TLB,如果写操作
            frameNumber = entry.getFrameNumber();
            tlb.addEntry(pageNumber, frameNumber);

        }
        return frameNumber * Constants.PAGE_SIZE_BYTES + offset;
    }

    public int getInnerFragmentation() {
        return ptr.getInnerFragmentation();
    }

    public int getPageTableAddress() {
        return ptr.getPageTableAddress();
    }

    public int getPageTableSize() {
        return ptr.getPageTableSize();
    }

    public int getCodeSize() {
        return ptr.getCodeSize();
    }

    public TLB getTLB() {
        return tlb;
    }

    public boolean isTLBEmpty() {
        return tlb.isEmpty();
    }

}
