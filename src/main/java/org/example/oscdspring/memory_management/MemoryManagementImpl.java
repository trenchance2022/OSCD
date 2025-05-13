package org.example.oscdspring.memory_management;

import org.example.oscdspring.file_disk_management.FileDiskManagement;
import org.example.oscdspring.main.Constants;
import org.example.oscdspring.process_management.CPU;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.PIDBitmap;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
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
        int lastPageSize = cpu.getMMU().getLastPageSize() + size;
        int addPageSize = lastPageSize / Constants.PAGE_SIZE_BYTES;
        lastPageSize = lastPageSize % Constants.PAGE_SIZE_BYTES;

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
                if (entry.getDiskAddress() != -1 && entry.isAllocatedDisk())
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
        // 逐字读写
        for (int i = 0; i < length; i++) {
            int physicalAddress = mmu.addressTranslation(logicAddress + i, false);
            if (physicalAddress < 0) {
                return false;
            }
            data[i] = Memory.getInstance().read(physicalAddress, 1)[0];
        }

        return true;
    }


    @Override
    public boolean Write(CPU cpu, int logicAddress, byte[] data, int length) {
        MMU mmu = cpu.getMMU();

        for (int i = 0; i < length; i++) {
            int physicalAddress = mmu.addressTranslation(logicAddress + i, true);
            if (physicalAddress < 0) {
                return false;
            }
            Memory.getInstance().write(physicalAddress, 1, new byte[]{data[i]});
        }

        return true;
    }


    @Override
    public Map<String, Object> getPageUse() {
        return Memory.getInstance().getPageUse();
    }

    @Override
    public void showPageUse(int start, int end) {
        Memory.getInstance().showPageUse(start, end);
    }

    @Override
    public void releaseProcess(PCB pcb) {
        PageTable pageTable = PageTableArea.getInstance().getPageTable(pcb.getPageTableAddress());
        if (pageTable != null) {
            for (int i = 0; i < pageTable.getPageTableSize(); i++) {
                PageTableEntry entry = pageTable.getEntry(i, false);
                if (entry.isValid()) {
                    Memory.getInstance().freeBlock(entry.getFrameNumber());
                } else {
                    if (entry.getDiskAddress() != -1 && entry.isAllocatedDisk())
                        fileDiskManagement.freeBlock(entry.getDiskAddress());
                }
            }
        }
        PageTableArea.getInstance().removePageTable(pcb.getPageTableAddress());

    }

}
