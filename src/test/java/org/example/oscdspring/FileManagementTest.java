package org.example.oscdspring;

import org.example.oscdspring.file_disk_management.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileManagementTest {

    @Test
    void testCreateFileAndRemoveFile() {
        // 使用文件系统接口
        FileSystemImpl fs = new FileSystemImpl();  // 初始化磁盘，根目录
        int originalSize = fs.getOccupiedBlockIndices().size();  // 记录原始磁盘块数
        String fileName = "testFile.txt";
        int fileSize = 2048;  // 2KB 文件（应占用 2 块磁盘空间）

        // 创建文件
        fs.createFile(fileName, fileSize);

        // 创建后，文件应出现在当前目录并占用磁盘块
        int[] blocks = fs.getFileDiskBlock(fileName);
        assertNotNull(blocks, "File blocks should be allocated after file creation");
        assertEquals(Math.ceil(fileSize / 1024.0), blocks.length, "Allocated block count should match file size");

        // 检查磁盘占用块数是否增加
        List<Integer> occupiedBlockIndices = fs.getOccupiedBlockIndices();
        int expectedOccupiedBlocks = blocks.length + originalSize;
        assertEquals(expectedOccupiedBlocks, occupiedBlockIndices.size(), "Disk occupied blocks count should increase after file creation");

        // 删除文件后，文件应从目录中移除
        fs.removeFile(fileName);
        int[] blocksAfter = fs.getFileDiskBlock(fileName);
        assertNull(blocksAfter, "File should be removed from directory listing");

        occupiedBlockIndices = fs.getOccupiedBlockIndices();
        // 删除文件后，磁盘占用块数应减少
        assertEquals(originalSize, occupiedBlockIndices.size(), "Disk blocks should be freed after file removal");
    }

    @Test
    void testDirectoryNavigationAndRemoval() {
        FileSystemImpl fs = new FileSystemImpl();
        int originalSize = fs.getOccupiedBlockIndices().size();  // 记录原始磁盘块数

        // 创建目录并进入
        fs.createDirectory("dir1");
        fs.changeDirectory("dir1");
        String pathInDir1 = fs.getCurrentPath();
        assertTrue(pathInDir1.endsWith("/dir1"), "Current path should reflect navigation into dir1");

        // 在 dir1 内创建子目录和文件
        fs.createDirectory("subdir");
        fs.createFile("innerFile.txt", 512);

        // 返回根目录
        fs.goBack();
        String pathRoot = fs.getCurrentPath();
        assertTrue(pathRoot.equals("/") || pathRoot.endsWith("/root"), "Current path should be root after going back");

        // 删除非空目录（应无法删除，因为目录内还有文件）
        fs.removeDirectory("dir1");
        fs.changeDirectory("dir1");
        assertTrue(fs.getCurrentPath().endsWith("/dir1"), "Non-empty directory removal should not remove contents");

        // 删除目录并回到根目录
        fs.goBack();
        fs.removeDirectoryRecursively("dir1");
        fs.changeDirectory("dir1");
        assertFalse(fs.getCurrentPath().endsWith("/dir1"), "Directory should be removed after recursive deletion");

        // 目录内的文件应被删除，磁盘块应被释放
        List<Integer> occupiedBlockIndices = fs.getOccupiedBlockIndices();
        assertEquals(originalSize, occupiedBlockIndices.size(), "Disk blocks from files in recursively deleted directory should be freed");
    }

    @Test
    void testFileLockManagerReadWriteLocks() {
        FileLockManager lockManager = FileLockManager.getInstance();

        // 进程A和进程B同时获取读锁
        int pidA = 101;
        int pidB = 102;
        assertTrue(lockManager.acquireReadLock("file1", pidA), "First read lock should be acquired");
        assertTrue(lockManager.acquireReadLock("file1", pidB), "Second read lock should be acquired concurrently");

        // 进程C试图获取写锁，应该等待
        int pidC = 103;
        assertFalse(lockManager.acquireWriteLock("file1", pidC), "Write lock should not be acquired while read locks are active");

        // 释放一个读锁，进程C仍然不能获取写锁，直到所有读锁都释放
        lockManager.releaseReadLock("file1", pidA);
        assertFalse(lockManager.acquireWriteLock("file1", pidC), "Write lock should still wait until all reads are released");

        // 释放第二个读锁，写锁可以获取
        lockManager.releaseReadLock("file1", pidB);
        assertTrue(lockManager.acquireWriteLock("file1", pidC), "Write lock should succeed after all read locks are released");

        // 在写锁被持有时，读锁获取失败
        int pidD = 104;
        assertFalse(lockManager.acquireReadLock("file1", pidD), "Read lock should wait while write lock is held");

        // 释放写锁，读锁可以被获取
        lockManager.releaseWriteLock("file1", pidC);
        assertTrue(lockManager.acquireReadLock("file1", pidD), "Read lock should succeed after write lock is released");
    }
}
