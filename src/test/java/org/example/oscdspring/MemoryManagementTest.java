package org.example.oscdspring;

import org.example.oscdspring.memory_management.*;
import org.example.oscdspring.process_management.PIDBitmap;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.example.oscdspring.main.Constants;

import java.util.Map;

class MemoryManagementTest {

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

    @Test
    void testGetPageUseStatus() {
        FileSystemImpl fs = new FileSystemImpl();
        MemoryManagement memMan = new MemoryManagementImpl(fs);
        Map<String, Object> usage = memMan.getPageUse();
        assertEquals(Constants.MEMORY_PAGE_SIZE, usage.get("totalFrames"), "Total frames count should match memory size");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> frameInfo = (java.util.List<Map<String, Object>>) usage.get("frameInfo");
        // Initially, frameInfo should list system frames (PID=0) and none for empty frames
        long systemFramesCount = frameInfo.stream().filter(f -> ((int)f.get("pid")) == PIDBitmap.SYSTEM_PID).count();
        assertEquals(Constants.SYSTEM_MEMORY_PAGE_SIZE, systemFramesCount, "System frames should be accounted for in usage");
        // Allocate a new block to a process and verify it appears in frameInfo
        int block = Memory.getInstance().findEmptyBlock();
        Memory.getInstance().writeBlock(block, new byte[Constants.PAGE_SIZE_BYTES], 5, 0);
        usage = memMan.getPageUse();
        frameInfo = (java.util.List<Map<String, Object>>) usage.get("frameInfo");
        boolean found = frameInfo.stream().anyMatch(f -> ((int)f.get("pid")) == 5 && ((int)f.get("frameId")) == block);
        assertTrue(found, "Newly allocated frame should appear in page use info with correct PID and frame ID");
    }
}
