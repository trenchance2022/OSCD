package org.example.oscdspring;

import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.main.Constants;
import org.example.oscdspring.memory_management.*;
import org.example.oscdspring.process_management.CPU;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.PIDBitmap;
import org.example.oscdspring.process_management.Scheduler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Nested
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoryManagementTest {
    @Nested
    class MemoryManagementImplTest {
        /*
         * 对内存管理的接口测试
         * 1.获取物理内存使用情况
         * 2.分配内存：页表扩大
         * 3.读写内存：写入和读出的一样
         * 4.释放进程：删除页表，释放内存
         * */

        // 测试获取物理内存使用情况
        @Test
        void testGetPageUseStatus() {
            FileSystemImpl fs = new FileSystemImpl();
            MemoryManagement memMan = new MemoryManagementImpl(fs);
            Map<String, Object> usage = memMan.getPageUse();
            assertEquals(Constants.MEMORY_PAGE_SIZE, usage.get("totalFrames"), "Total frames count should match memory size");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> frameInfo = (java.util.List<Map<String, Object>>) usage.get("frameInfo");
            // Initially, frameInfo should list system frames (PID=0) and none for empty frames
            long systemFramesCount = frameInfo.stream().filter(f -> ((int) f.get("pid")) == PIDBitmap.SYSTEM_PID).count();
            assertEquals(Constants.SYSTEM_MEMORY_PAGE_SIZE, systemFramesCount, "System frames should be accounted for in usage");
            // Allocate a new block to a process and verify it appears in frameInfo
            int block = Memory.getInstance().findEmptyBlock();
            Memory.getInstance().writeBlock(block, new byte[Constants.PAGE_SIZE_BYTES], 5, 0);
            usage = memMan.getPageUse();
            frameInfo = (java.util.List<Map<String, Object>>) usage.get("frameInfo");
            boolean found = frameInfo.stream().anyMatch(f -> ((int) f.get("pid")) == 5 && ((int) f.get("frameId")) == block);
            assertTrue(found, "Newly allocated frame should appear in page use info with correct PID and frame ID");
        }

        // 测试分配释放内存
        @Test
        void testAllocateMemory() {
            FileSystemImpl fs = new FileSystemImpl();
            MemoryManagement memMan = new MemoryManagementImpl(fs);
            CPU cpu = new CPU(1, Scheduler.getInstance(), new DeviceManager());

            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            // 创建一个进程,大小为10000内的随机数
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            cpu.changeProcess(pcb);
            int initSize = pcb.getPageTableSize();
            int addSize = r.nextInt(10000);
            memMan.Allocate(cpu, addSize);
            // 分配内存后，页表大小增加大小为addSize，增加的页数应该为addSize/PAGE_SIZE
            int expectedPageTableSize = addSize / Constants.PAGE_SIZE_BYTES;
            assertEquals(expectedPageTableSize, pcb.getPageTableSize() - initSize, "Page table size should increase after allocation");

        }

        // 测试读写内存：写入和读出的一样
        @Test
        void testReadWriteMemory() {
            FileSystemImpl fs = new FileSystemImpl();
            MemoryManagement memMan = new MemoryManagementImpl(fs);
            CPU cpu = new CPU(1, Scheduler.getInstance(), new DeviceManager());

            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            cpu.changeProcess(pcb);
            int addSize = r.nextInt(10000);
            memMan.Allocate(cpu, addSize);
            // 分配内存后，在分配的部分进行随机读写
            int wrSize = r.nextInt(addSize);
            byte[] wrContent = new byte[wrSize];
            r.nextBytes(wrContent);

            // 写
            memMan.Write(cpu, codeSize, wrContent, wrSize);
            // 读
            byte[] readContent = new byte[wrSize];
            memMan.Read(cpu, codeSize, readContent, wrSize);

            assertArrayEquals(wrContent, readContent, "Read content should equal the write content");
        }

        // 测试释放内存
        @Test
        void testFreeMemory() {
            FileSystemImpl fs = new FileSystemImpl();
            MemoryManagement memMan = new MemoryManagementImpl(fs);
            Map<String, Object> usage = memMan.getPageUse();

            CPU cpu = new CPU(1, Scheduler.getInstance(), new DeviceManager());

            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            cpu.changeProcess(pcb);
            int addSize = r.nextInt(10000);
            memMan.Allocate(cpu, addSize);
            // 分配内存后，在分配的部分进行随机读写
            int wrSize = r.nextInt(addSize);
            byte[] wrContent = new byte[wrSize];
            r.nextBytes(wrContent);

            memMan.Write(cpu, codeSize, wrContent, wrSize);
            byte[] readContent = new byte[wrSize];
            memMan.Read(cpu, codeSize, readContent, wrSize);

            Map<String, Object> usage2 = memMan.getPageUse();
            assertNotEquals(usage, usage2, "Memory usage should change after allocation");
            // 释放内存
            memMan.releaseProcess(pcb);

            // 释放后，物理内存使用情况应该与之前相同
            Map<String, Object> usage3 = memMan.getPageUse();
            assertEquals(usage, usage3, "Memory usage should be the same after process release");
        }
    }

    @Nested
    class MemoryTest {
        /*
         * 对内存的测试
         * 1.正确查找空闲物理内存块
         * 2.物理内存的读写
         * */

        // 查找空闲物理内存块
        @Test
        void testFindEmptyBlockAndReuse() {
            Memory memory = Memory.getInstance();
            int block = memory.findEmptyBlock();
            assertTrue(block >= Constants.SYSTEM_MEMORY_PAGE_SIZE, "Found block should be in user space (>= system reserved pages)");
            // Write to the found block to mark it used
            byte[] data = new byte[Constants.PAGE_SIZE_BYTES];
            memory.writeBlock(block, data, 2, 0);  // simulate PID=2 occupying this page
            // Next empty block should be a higher index
            int block2 = memory.findEmptyBlock();
            assertNotEquals(block, block2, "Subsequent findEmptyBlock should skip the recently allocated block");
            // Free the first block and ensure it becomes available again
            memory.freeBlock(block);
            int block3 = memory.findEmptyBlock();
            assertEquals(block, block3, "Freed block should be found again as empty");
        }

        // 测试物理内存读写
        @Test
        void testMemoryReadWriteBlockData() {
            Memory memory = Memory.getInstance();
            int userBlock = memory.findEmptyBlock();
            byte[] testData = new byte[Constants.PAGE_SIZE_BYTES];
            for (int i = 0; i < testData.length; i++) {
                testData[i] = (byte) (i % 256);
            }
            memory.writeBlock(userBlock, testData, 3, 1);
            byte[] readBack = memory.readBlock(userBlock);
            assertNotNull(readBack, "Reading a user memory block should return data");
            assertArrayEquals(testData, readBack, "Data read from memory block should match written data");
            // Reading a system-reserved block should return null (protected region)
            assertNull(memory.readBlock(0), "Reading from system memory area should return null");
        }

        // 测试物理内存读写（跨页）
        @Test
        void testMemoryReadWriteAcrossPages() {
            Memory memory = Memory.getInstance();
            // Choose an address near the end of one page to force spanning into the next page
            int startAddress = Constants.PAGE_SIZE_BYTES * (Constants.SYSTEM_MEMORY_PAGE_SIZE) + 900;
            int length = 300;  // this spans 900-1023 of one page and 0-176 of next page
            // Prepare data of specified length
            byte[] writeData = new byte[length];
            for (int i = 0; i < length; i++) {
                writeData[i] = (byte) (65 + (i % 26));  // ASCII letters sequence
            }
            memory.write(startAddress, length, writeData);
            byte[] readData = memory.read(startAddress, length);
            assertArrayEquals(writeData, readData, "Data read across page boundary should match data written");
        }

    }

    @Nested
    class MMUTest {
        /*
         * 地址转换器的测试
         * 1. 切换进程时更新页表，快表等数据
         * 2. 地址转换，
         * 3. 地址保护，对越界情况的处理
         * 4. 快表替换策略
         * 5. 页面置换算法
         */

        // 测试进程切换时更新页表，快表等数据
        @Test
        void testTranProcess() {
            MMU mmu = new MMU();
            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            // 创建两个进程,大小为10000内的随机数
            PCB pcb1 = new PCB(1, codeSize, addressBlock, 1);
            PCB pcb2 = new PCB(2, codeSize, addressBlock, 1);
            mmu.update(pcb1);
            // 检查mmu的ptr是否更新
            assertEquals(mmu.getLastPageSize(), pcb1.getLastPageSize(), "MMU last page size should match PCB");
            assertEquals(mmu.getPageTableAddress(), pcb1.getPageTableAddress(), "MMU page table address should match PCB");
            assertEquals(mmu.getPageTableSize(), pcb1.getPageTableSize(), "MMU page table size should match PCB");
            assertEquals(mmu.getCodeSize(), pcb1.getCodeSize(), "MMU code size should match PCB");
            assertEquals(mmu.getInnerFragmentation(), pcb1.getInnerFragmentation(), "MMU inner fragmentation should match PCB");
            // 检查mmu的tlb是否刷新
            assertTrue(mmu.isTLBEmpty(), "TLB should be empty after process switch");

            // 切换到第二个进程
            mmu.update(pcb2);
            // 检查mmu的ptr是否更新
            assertEquals(mmu.getLastPageSize(), pcb2.getLastPageSize(), "MMU last page size should match PCB");
            assertEquals(mmu.getPageTableAddress(), pcb2.getPageTableAddress(), "MMU page table address should match PCB");
            assertEquals(mmu.getPageTableSize(), pcb2.getPageTableSize(), "MMU page table size should match PCB");
            assertEquals(mmu.getCodeSize(), pcb2.getCodeSize(), "MMU code size should match PCB");
            assertEquals(mmu.getInnerFragmentation(), pcb2.getInnerFragmentation(), "MMU inner fragmentation should match PCB");
            // 检查mmu的tlb是否刷新
            assertTrue(mmu.isTLBEmpty(), "TLB should be empty after process switch");

        }

        // 测试地址转换
        @Test
        void testAddressTran() {
            MMU mmu = new MMU();
            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            mmu.update(pcb);

            // 检查地址转换
            for (int i = 0; i < codeSize; i++) {
                int logicalAddress = i;
                int mmutrans = mmu.addressTranslation(logicalAddress, false);

                if (logicalAddress >= mmu.getCodeSize()) {
                    logicalAddress += mmu.getInnerFragmentation();
                }

                int pageNumber = logicalAddress / Constants.PAGE_SIZE_BYTES;
                int offset = logicalAddress % Constants.PAGE_SIZE_BYTES;
                PageTable pageTable = PageTableArea.getInstance().getPageTable(mmu.getPageTableAddress());

                // 查找页表
                PageTableEntry entry = pageTable.getEntry(pageNumber, false);

                assertEquals(entry.getFrameNumber() * Constants.PAGE_SIZE_BYTES + offset, mmutrans, "Address translation should match the expected physical address");
            }
        }

        // 测试地址保护，对越界情况的处理
        @Test
        void testAddressProtect() {
            MMU mmu = new MMU();
            CPU cpu = new CPU(1, Scheduler.getInstance(), new DeviceManager());

            Random r = new Random();
            int codeSize = r.nextInt(10000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            cpu.changeProcess(pcb);
            // 分配内存后，进行随机读写，并且越界
            int wrSize = r.nextInt(codeSize + 1024, codeSize * 10 + 1024);
            assertEquals(mmu.addressTranslation(wrSize, false), -2, "Write should fail due to out of bounds");
        }

        // 测试快表替换
        @Test
        void testTLBClock() {
            MMU mmu = new MMU();
            Random r = new Random();
            int codeSize = r.nextInt(10000, 50000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            mmu.update(pcb);

            MMU mmuc = mmu.clone();

            // 测试tlb的替换策略
            for (int i = 0; i < codeSize; i += 1050) {
                int logicalAddress = i;
                mmu.addressTranslation(logicalAddress, false);
                assertNotEquals(mmuc.getTLB(), mmu.getTLB(), "TLB should be updated after address translation");
            }

            // 同一页进行多次地址转换，检查tlb是否一致
            for (int i = 0; i < 1024; i++) {
                int logicalAddress = i;
                mmu.addressTranslation(logicalAddress, false);
                if (i == 0) {
                    mmuc = mmu.clone();
                    continue;
                }
                assertEquals(mmuc.getTLB(), mmu.getTLB(), "TLB should be same after address translation");
            }
        }

        // 测试页面置换
        @Test
        void testPageReplace() {
            MMU mmu = new MMU();
            Random r = new Random();
            int codeSize = r.nextInt(10000, 50000);
            int blocks = (codeSize - 1) / Constants.BLOCK_SIZE_BYTES + 1;
            int[] addressBlock = new int[blocks];
            for (int i = 0; i < blocks; i++) {
                addressBlock[i] = new Random().nextInt(Constants.DISK_SIZE);
            }
            PCB pcb = new PCB(1, codeSize, addressBlock, 1);
            mmu.update(pcb);

            int pageTableAddress = mmu.getPageTableAddress();
            PageTable pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress).clone();
            // 测试tlb的替换策略
            for (int i = 0; i < codeSize; i += 1050) {
                int logicalAddress = i;
                mmu.addressTranslation(logicalAddress, false);
                assertNotEquals(pageTable, PageTableArea.getInstance().getPageTable(pageTableAddress).clone(), "TLB should be updated after address translation");
            }

            // 同一页进行多次地址转换，检查tlb是否一致
            for (int i = 0; i < 1024; i++) {
                int logicalAddress = i;
                mmu.addressTranslation(logicalAddress, false);
                if (i == 0) {
                    pageTable = PageTableArea.getInstance().getPageTable(pageTableAddress).clone();
                    continue;
                }
                assertEquals(pageTable, PageTableArea.getInstance().getPageTable(pageTableAddress).clone(), "TLB should be same after address translation");
            }
        }
    }

}
