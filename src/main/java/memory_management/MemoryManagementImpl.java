package memory_management;

import process_management.CPU;
import process_management.PIDBitmap;
import file_disk_management.FileDiskManagement;
import main.Constants;

import java.util.Arrays;

public class MemoryManagementImpl implements MemoryManagement {

    private final FileDiskManagement fileDiskManagement;

    public MemoryManagementImpl(FileDiskManagement fileDiskManagement) {
        this.fileDiskManagement = fileDiskManagement;
    }

    @Override
    public boolean Allocate(CPU cpu, int size) {
        // 分配内存，需要更新页表(如果需要新增页表项)，更新MMU(pageSize,lastPageSize)，更新PCB（size，pageSize,lastPageSize）

        int pageTableAddress = cpu.getCurrentPCB().getPageTableAddress();
        //获取页表
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);

        // 分配后的最后一页大小
        int lastPageSize = cpu.getMMU().getLastPageSize()+size;
        int addPageSize=lastPageSize/Constants.PAGE_SIZE_BYTES;
        lastPageSize=lastPageSize%Constants.PAGE_SIZE_BYTES;

        //更新页表
        pageTable.addEntries(addPageSize);

        //更新PCB
        cpu.getCurrentPCB().addSize(size);
        cpu.getCurrentPCB().addPage(addPageSize);
        cpu.getCurrentPCB().setLastPageSize(lastPageSize);

        //更新MMU
        cpu.getMMU().addPageSize(addPageSize);
        cpu.getMMU().setLastPageSize(lastPageSize);

        return true;
    }

    @Override
    public boolean FreeProcess(CPU cpu) {
        int pageTableAddress = cpu.getCurrentPCB().getPageTableAddress();
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress);
        for (int i = 0; i < pageTable.getPageTableSize(); i++) {
            PageTableEntry entry = pageTable.getEntry(i, false);
            if (entry.isValid()) {
                Memory.getInstance().freeBlock(entry.getFrameNumber());
            } else {
                if(entry.getDiskAddress()!=-1)
                    fileDiskManagement.freeBlock(entry.getDiskAddress());
            }
        }
        PageTableArea.getInstance().removePageTable(pageTableAddress);
        PIDBitmap.getInstance().freePID(cpu.getCurrentPCB().getPid());
        return true;
    }


    @Override
    public boolean Read(CPU cpu, int logicAddress, byte[] data, int length) {
        MMU mmu = cpu.getMMU();

        //第一个页面读取的数据长度
        int firstPageReadSize = Math.min(Constants.PAGE_SIZE_BYTES - logicAddress % Constants.PAGE_SIZE_BYTES, length);
        //读取
        int physicalAddress = mmu.addressTranslation(logicAddress, false);
        if (physicalAddress < 0) {
            return false;
        }
        System.arraycopy(Memory.getInstance().read(physicalAddress, firstPageReadSize), 0, data, 0, firstPageReadSize);
        // 读取中间的整块页面
        int remainBlock = (length - firstPageReadSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            physicalAddress = mmu.addressTranslation(logicAddress + firstPageReadSize + i * Constants.PAGE_SIZE_BYTES, false);
            if (physicalAddress < 0) {
                return false;
            }
            System.arraycopy(Memory.getInstance().read(physicalAddress, Constants.PAGE_SIZE_BYTES), 0, data, firstPageReadSize + i * Constants.PAGE_SIZE_BYTES, Constants.PAGE_SIZE_BYTES);
        }
        // 读取最后一个页面的数据
        int lastPageReadSize = length - firstPageReadSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        if (lastPageReadSize > 0) {
            physicalAddress = mmu.addressTranslation(logicAddress + firstPageReadSize + remainBlock * Constants.PAGE_SIZE_BYTES, false);
            if (physicalAddress < 0) {
                return false;
            }
            System.arraycopy(Memory.getInstance().read(physicalAddress, lastPageReadSize), 0, data, firstPageReadSize + remainBlock * Constants.PAGE_SIZE_BYTES, lastPageReadSize);
        }
        return true;
    }


    @Override
    public boolean Write(CPU cpu, int logicAddress, byte[] data, int length) {
        MMU mmu = cpu.getMMU();

        // 第一个页面写入的数据长度
        int firstPageWriteSize = Math.min(Constants.PAGE_SIZE_BYTES - logicAddress % Constants.PAGE_SIZE_BYTES, length);
        // 写入第一个页面的数据
        int physicalAddress = mmu.addressTranslation(logicAddress, true);
        if (physicalAddress < 0) {
            return false;
        }
        Memory.getInstance().write(physicalAddress, firstPageWriteSize, Arrays.copyOfRange(data, 0, firstPageWriteSize));

        // 写入中间的整块页面
        int remainBlock = (length - firstPageWriteSize) / Constants.PAGE_SIZE_BYTES;
        for (int i = 0; i < remainBlock; i++) {
            physicalAddress = mmu.addressTranslation(logicAddress + firstPageWriteSize + i * Constants.PAGE_SIZE_BYTES, true);
            if (physicalAddress < 0) {
                return false;
            }
            Memory.getInstance().write(physicalAddress, Constants.PAGE_SIZE_BYTES, Arrays.copyOfRange(data, firstPageWriteSize + i * Constants.PAGE_SIZE_BYTES, firstPageWriteSize + (i + 1) * Constants.PAGE_SIZE_BYTES));
        }

        // 写入最后一个页面的数据
        int lastPageWriteSize = length - firstPageWriteSize - remainBlock * Constants.PAGE_SIZE_BYTES;
        if (lastPageWriteSize > 0) {
            physicalAddress = mmu.addressTranslation(logicAddress + firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES, true);
            if (physicalAddress < 0) {
                return false;
            }
            Memory.getInstance().write(physicalAddress, lastPageWriteSize, Arrays.copyOfRange(data, firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES, firstPageWriteSize + remainBlock * Constants.PAGE_SIZE_BYTES + lastPageWriteSize));
        }

        return true;
    }


    @Override
    public void showPageUse(int start, int end) {
        Memory.getInstance().showPageUse(start, end);
    }

}