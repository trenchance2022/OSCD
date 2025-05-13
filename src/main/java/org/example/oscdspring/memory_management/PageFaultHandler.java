package org.example.oscdspring.memory_management;

import org.example.oscdspring.util.LogEmitterService;

import static org.example.oscdspring.main.Library.getFileSystem;

public class PageFaultHandler {

    // 互斥处理
    private static final Object lock = new Object();

    // 处理缺页中断，参数：页表地址，tlb，页号

    // 1. 如果物理内存已满，且该进程没有被调入物理内存的页面，无法局部替换，返回-1;
    // 2. 如果物理内存有空，且该进程还有未使完分配的物理内存，直接分配，无需调出
    // 3. 如果物理内存已满，或该进程已经使用完分配的物理内存，需要调出页面
    //      只有被修改的页面才需要写回磁盘，否则直接替换
    //      如果被修改的页面此前没有对应的磁盘块（第一次被调出），需要分配空闲磁盘块
    // 将磁盘块读入内存
    // 更新页表项，更新TLB
    static public boolean handlePageFault(int pageTableAddress, TLB tlb, int pageNumber) {
        synchronized (lock) {
            // 读取页表
            PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);
            // 读取出错的页表项
            PageTableEntry faultPageTableEntry = pageTable.getEntry(pageNumber, false);

            boolean PageHasEmptyFrame = pageTable.hasEmptyFrame();
            boolean MemoryHasEmptyFrame = Memory.getInstance().findEmptyBlock() != -1;
            boolean HasValidPage = pageTable.hasValidPage();

            if ((!HasValidPage) && (!MemoryHasEmptyFrame)) {
                LogEmitterService.getInstance().sendLog("物理内存已满，且未给本进程分配物理内存，无法替换页面");
                return false;
            }

            // 获取调入页面的页号
            int freeFrame;
            // 物理内存已满或者页表内存用完，需要调出页面
            if ((!MemoryHasEmptyFrame) || (!PageHasEmptyFrame)) {
                int replacePage = pageTable.getReplacePage();
                PageTableEntry replacePageTableEntry = pageTable.getEntry(replacePage, false);
                if (replacePageTableEntry == null) {
                    System.err.println("按理说这里不可能false， 如果出现了说明有BUG，反馈给我，我会修改");
                    return false;
                }
                freeFrame = replacePageTableEntry.getFrameNumber();
                // 只有被修改的页面才需要写回磁盘，否则直接替换
                if (replacePageTableEntry.isDirty()) {
                    // 如果被修改的页面此前没有对应，需要分配磁盘块
                    if (!replacePageTableEntry.isAllocatedDisk()) {
                        int diskAddress;
                        diskAddress = getFileSystem().allocateBlock();
                        replacePageTableEntry.setDiskAddress(diskAddress);
                        replacePageTableEntry.AllocatedDisk();

                    }
                    getFileSystem().writeBlock(replacePageTableEntry.getDiskAddress(), Memory.getInstance().readBlock(replacePageTableEntry.getFrameNumber()));
                }
                // 更新内存
                Memory.getInstance().freeBlock(freeFrame);
                // 更新页表项
                replacePageTableEntry.setValid(false);
                // 更新TLB
                tlb.deleteEntry(replacePage);
            }
            // 物理内存有空闲块，直接分配
            else {
                freeFrame = Memory.getInstance().findEmptyBlock();
                pageTable.addMemoryUsed();
            }

            // 将磁盘块读入内存
            if (faultPageTableEntry.getDiskAddress() != -1) {
                byte[] block;
                block = getFileSystem().readBlock(faultPageTableEntry.getDiskAddress());
                Memory.getInstance().writeBlock(freeFrame, block, pageTable.getPid(), pageNumber);
            } else {// 更新内存块状态
                Memory.getInstance().updateBlock(freeFrame, pageTable.getPid(), pageNumber);
            }

            // 更新页表项，
            faultPageTableEntry.setFrameNumber(freeFrame);
            faultPageTableEntry.setValid(true);
            faultPageTableEntry.setAccessed(true);
            faultPageTableEntry.setDirty(false);

            // 更新TLB
            tlb.addEntry(pageNumber, freeFrame);
            return true;
        }
    }
}


