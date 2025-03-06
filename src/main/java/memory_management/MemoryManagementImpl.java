package memory_management;

import process_management.CPU;
import process_management.PIDBitmap;
import file_disk_management.FileDiskManagement;
import file_disk_management.FileSystemImpl;
import main.Constants;

import java.util.Arrays;

public class MemoryManagementImpl implements MemoryManagement {

    @Override
    public boolean Allocate(CPU cpu, int size) {
        // 分配内存，需要更新页表，更新MMU，更新PCB

        int pageTableAddress = cpu.getCurrentPCB().getPageTableAddress();
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);

        int lastPageRemainSize = Constants.PAGE_SIZE_BYTES - pageTable.getLastPageSize();
        int addPageSize = (size - lastPageRemainSize) / Constants.PAGE_SIZE_BYTES;
        int lastPageSize = (size - lastPageRemainSize) % Constants.PAGE_SIZE_BYTES;
        if (lastPageSize != 0) {
            addPageSize++;
        } else {
            lastPageSize = Constants.PAGE_SIZE_BYTES;
        }

        //更新页表
        pageTable.setLastPageSize(lastPageSize);
        for (int i = 0; i < addPageSize; i++) {
            pageTable.addEntry();
        }

        //更新PCB
        cpu.getCurrentPCB().addSize(size);
        cpu.getCurrentPCB().addPage(addPageSize);

        //更新MMU
        cpu.getMMU().addPageSize(addPageSize);

        return true;
    }

    @Override
    // 释放进程，需要释放物理内存，释放虚拟内存，释放页表，释放PID

    public boolean FreeProcess(CPU cpu) {
        FileDiskManagement fileDiskManagement = new FileSystemImpl();
        int pageTableAddress = cpu.getCurrentPCB().getPageTableAddress();
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);
        // 释放物理内存和虚拟内存
        for (int i = 0; i < pageTable.getPageTableSize(); i++) {
            PageTableEntry entry = pageTable.getEntry(i, false);
            if (entry.isValid()) {// 页表项有效，需要释放物理内存
                Memory.getInstance().freeBlock(entry.getFrameNumber());
            } else {// 页表项无效，需要释放虚拟内存(磁盘)
                fileDiskManagement.freeBlock(entry.getDiskAddress());
            }
        }
        // 释放页表
        PageTableArea.getInstance().removePageTable(pageTableAddress);
        // 释放PID
        PIDBitmap.getInstance().freePID(cpu.getCurrentPCB().getPID());
        return true;
    }


    @Override
    public boolean Read(CPU cpu, int logicAddress, byte[] data, int length) {
        MMU mmu = cpu.getMMU();
//        // 读取数据,简单粗暴的方法，一个一个读取
//        for(int i = 0; i < length; i++){
//            int physicalAddress = mmu.addressTranslation(logicAddress + i,false);
//            data[i] = Memory.getInstance().read(physicalAddress, 1)[0];
//        }
//
        //优化方法，一次读取一个页面，然后再从页面中读取数据
        //第一个页面读取的数据长度
        int firstPageReadSize = Math.min(Constants.PAGE_SIZE_BYTES - logicAddress % Constants.PAGE_SIZE_BYTES, length);
        //读取
        System.arraycopy(Memory.getInstance().read(mmu.addressTranslation(logicAddress, false), firstPageReadSize), 0, data, 0, firstPageReadSize);
        // 读取中间的整块页面
        int remainBlock = (length - firstPageReadSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            System.arraycopy(Memory.getInstance().read(mmu.addressTranslation(logicAddress + firstPageReadSize + i * Constants.PAGE_SIZE_BYTES, false), Constants.PAGE_SIZE_BYTES)
                    , 0, data, firstPageReadSize + i * Constants.PAGE_SIZE_BYTES, Constants.PAGE_SIZE_BYTES);
        }
        // 读取最后一个页面的数据
        int lastPageReadSize = length - firstPageReadSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        System.arraycopy(Memory.getInstance().read(mmu.addressTranslation(logicAddress + firstPageReadSize + remainBlock * Constants.PAGE_SIZE_BYTES, false), lastPageReadSize),
                0, data, firstPageReadSize + remainBlock * Constants.PAGE_SIZE_BYTES, lastPageReadSize);

        return true;
    }


    @Override
    public boolean Write(CPU cpu, int logicAddress, byte[] data, int length) {
        MMU mmu = cpu.getMMU();
//        // 写入数据,简单粗暴的方法，一个一个写入
//        for(int i = 0; i < length; i++){
//            int physicalAddress = mmu.addressTranslation(logicAddress + i,true);
//            Memory.getInstance().write(physicalAddress,1, new byte[]{data[i]});
//        }

        // 优化方法，一次写入一个页面，然后再从页面中写入数据
        // 第一个页面写入的数据长度
        int firstPageWriteSize = Math.min(Constants.PAGE_SIZE_BYTES - logicAddress % Constants.PAGE_SIZE_BYTES, length);
        //写入
        Memory.getInstance().write(mmu.addressTranslation(logicAddress, true),
                firstPageWriteSize,
                Arrays.copyOfRange(data, 0, firstPageWriteSize));
        // 写入剩余的数据
        int remainBlock = (length - firstPageWriteSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            Memory.getInstance().write(mmu.addressTranslation(logicAddress + firstPageWriteSize + i * Constants.PAGE_SIZE_BYTES, true),
                    Constants.PAGE_SIZE_BYTES,
                    Arrays.copyOfRange(data, firstPageWriteSize + i * Constants.PAGE_SIZE_BYTES, firstPageWriteSize + (i + 1) * Constants.PAGE_SIZE_BYTES));
        }
        // 写入最后一个页面的数据
        int lastPageWriteSize = length - firstPageWriteSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        Memory.getInstance().write(mmu.addressTranslation(logicAddress + firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES, true),
                lastPageWriteSize,
                Arrays.copyOfRange(data, firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES, firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES + lastPageWriteSize));

        return true;

    }


    @Override
    public void showPageUse(int start, int end) {
        Memory.getInstance().showPageUse(start, end);
    }

}