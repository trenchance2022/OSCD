package memory_management;

import process_management.PCB;
import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import main.Constants;

import static java.lang.System.exit;
import static main.Main.fileSystem;

// 每个CPU都有一个MMU，MMU负责地址转换，内含TLB缓冲区，页表寄存器
public class MMU {

    private final PTR ptr = new PTR();//页表寄存器
    private final TLB tlb = new TLB();//TLB缓冲区

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

    // 转换逻辑地址为物理地址，如果缺页，会调用PageFaultHandler处理，然后重新运行函数
    public int addressTranslation(int logicalAddress, boolean dirty) {
        int logicalAddress1 = logicalAddress;

        if (logicalAddress < 0) {
            System.out.println("逻辑地址小于0 MMU");
            return -1;
        }

        // 更新逻辑地址:如果访问的是代码段之后数据段，逻辑地址加上内部碎片
        if (logicalAddress >= ptr.getCodeSize()) {
            logicalAddress += ptr.getInnerFragmentation();
        }

        int pageNumber = logicalAddress / Constants.PAGE_SIZE_BYTES;
        int offset = logicalAddress % Constants.PAGE_SIZE_BYTES;
        if (pageNumber >= ptr.getPageTableSize()||pageNumber== ptr.getPageTableSize()-1&&offset>=ptr.getLastPageSize()) {
            System.out.println("逻辑地址超出范围 MMU");
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
                    System.out.println("缺页中断处理失败");
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
}

class PTR {
    private int pageTableAddress;//页表地址
    private int pageTableSize;//页表大小
    private int codeSize;//代码段大小
    private int innerFragmentation;//内部碎片(代码段与数据段之间的碎片)
    private int lastPageSize;//数据段最后一页的大小

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

// 这里假定使用Clock算法来替换TLB中的映射关系
class TLB {
    private final TLBEntry[] TLB = new TLBEntry[Constants.TLB_SIZE];
    private int clockHand = 0;//指针，指向下一个要替换的TLB项

    public TLB() {
        for (int i = 0; i < Constants.TLB_SIZE; i++) {
            TLB[i] = new TLBEntry();
        }
    }

    // 添加映射关系，如果TLB已满，用Clock算法替换
    public void addEntry(int pageNumber, int frameNumber) {
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


class PageFaultHandler {
    // 处理缺页中断，参数：页表地址，tlb，页号

    // 1. 如果物理内存已满，且该进程没有被调入物理内存的页面，无法局部替换，返回-1;
    // 2. 如果物理内存有空，且该进程还有未使完分配的物理内存，直接分配，无需调出
    // 3. 如果物理内存已满，或该进程已经使用完分配的物理内存，需要调出页面
    //      只有被修改的页面才需要写回磁盘，否则直接替换
    //      如果被修改的页面此前没有对应的磁盘块（第一次被调出），需要分配空闲磁盘块
    // 将磁盘块读入内存
    // 更新页表项，更新TLB
    static public boolean handlePageFault(int pageTableAddress, TLB tlb, int pageNumber) {
        // 读取页表
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);
        // 读取出错的页表项
        PageTableEntry faultPageTableEntry = pageTable.getEntry(pageNumber, false);

        boolean PageHasEmptyFrame = pageTable.hasEmptyFrame();
        boolean MemoryHasEmptyFrame = Memory.getInstance().findEmptyBlock() != -1;
        boolean HasValidPage = pageTable.hasValidPage();

        if ((!HasValidPage) && (!MemoryHasEmptyFrame)) {
            System.out.println("物理内存已满，且未给本进程分配物理内存，无法替换页面");
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
                    int diskAddress = fileSystem.allocateBlock();
                    replacePageTableEntry.setDiskAddress(diskAddress);
                    replacePageTableEntry.AllocatedDisk();

                }
                fileSystem.writeBlock(replacePageTableEntry.getDiskAddress(), Memory.getInstance().readBlock(replacePageTableEntry.getFrameNumber()));
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
        if (faultPageTableEntry.getDiskAddress() != -1)
            Memory.getInstance().writeBlock(freeFrame, fileSystem.readBlock(faultPageTableEntry.getDiskAddress()), pageTable.getPid(), pageNumber);

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



