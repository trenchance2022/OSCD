import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Directory {
    String name;
    Directory parent;  // 父目录
    List<Inode> files;  // 存储目录下的文件
    List<Directory> subdirectories;  // 存储子目录

    public Directory(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.files = new ArrayList<>();
        this.subdirectories = new ArrayList<>();
    }

    // 创建文件
    public void createFile(Disk disk, String fileName, int fileSize, int inodeNumber) {
        if (fileSize > disk.getFreeSpace()) {
            System.out.println("Not enough space to create file " + fileName);
            return;
        }

        Inode inode = new Inode(inodeNumber, fileName, fileSize);
        int totalBlocksNeeded = (int) Math.ceil((double) fileSize / 1024);  // 计算所需的块数

        int remainingSize = fileSize;  // 记录还需要写入的字节数

        for (int i = 0; i < totalBlocksNeeded; i++) {
            int blockIndex = disk.allocateBlock();
            if (blockIndex != -1) {
                inode.blockIndexes[i] = blockIndex;

                // 计算本次应该写入的字节数（可能小于 1024）
                int bytesToWrite = Math.min(remainingSize, 1024);
                disk.writeRandomDataToDisk(blockIndex * 1024, bytesToWrite);

                remainingSize -= bytesToWrite;
            } else {
                System.out.println("Not enough space to create file " + fileName);
                return;
            }
        }

        disk.setOccupiedBlocks(disk.getOccupiedBlocks() + totalBlocksNeeded);
        files.add(inode);
        System.out.println("File " + fileName + " created with size " + fileSize + "B.");
    }


    // 创建子目录
    public void createDirectory(String directoryName) {
        Directory dir = new Directory(directoryName, this);
        subdirectories.add(dir);
        System.out.println("Directory " + directoryName + " created.");
    }

    // 删除文件
    public void removeFile(Disk disk, String fileName) {
        for (int i = 0; i < files.size(); i++) {
            Inode inode = files.get(i);
            if (inode.fileName.equals(fileName)) {
                // 释放磁盘块
                disk.setOccupiedBlocks(disk.getOccupiedBlocks() - inode.blockIndexes.length);
                for (int blockIndex : inode.blockIndexes) {
                    disk.freeBlock(blockIndex);
                }
                files.remove(i);
                System.out.println("File " + fileName + " deleted.");
                return;
            }
        }
        System.out.println("File " + fileName + " not found.");
    }

    // 删除空目录
    public void removeDirectory(String directoryName) {
        for (int i = 0; i < subdirectories.size(); i++) {
            Directory dir = subdirectories.get(i);
            if (dir.name.equals(directoryName)) {
                if (dir.files.isEmpty() && dir.subdirectories.isEmpty()) { // 确保目录为空
                    subdirectories.remove(i);
                    System.out.println("Directory " + directoryName + " deleted.");
                    return ;
                } else {
                    System.out.println("Directory " + directoryName + " is not empty. Use rmrdir to remove non-empty directories.");
                    return ;
                }
            }
        }
        System.out.println("Directory " + directoryName + " not found.");
    }

    // 递归删除目录及其所有内容
    public void removeDirectoryRecursively(Disk disk, String directoryName) {
        // 使用栈来迭代处理目录
        Stack<Directory> stack = new Stack<>();
        Directory targetDir = null;

        // 查找目标目录
        for (Directory dir : subdirectories) {
            if (dir.name.equals(directoryName)) {
                targetDir = dir;
                break;
            }
        }

        if (targetDir == null) {
            return;
        }

        // 将目标目录加入栈中
        stack.push(targetDir);

        // 迭代删除文件和子目录
        while (!stack.isEmpty()) {
            Directory currentDir = stack.pop();

            // 先删除当前目录中的所有文件
            for (Inode inode : currentDir.files) {
                disk.setOccupiedBlocks(disk.getOccupiedBlocks() - inode.blockIndexes.length);
                for (int blockIndex : inode.blockIndexes) {
                    disk.freeBlock(blockIndex);
                }
            }

            // 将当前目录中的子目录加入栈中
            for (Directory subDir : currentDir.subdirectories) {
                stack.push(subDir);
            }

            // 删除当前目录
            subdirectories.remove(currentDir);
        }
    }

    // 查看目录内容
    public void listDirectory() {
        for (Inode inode : files) {
            System.out.println(inode.fileName + " ");
        }
        for (Directory dir : subdirectories) {
            System.out.println(dir.name + "/");
        }
    }

    // 切换到子目录
    public Directory changeDirectory(String directoryName) {
        for (Directory dir : subdirectories) {
            if (dir.name.equals(directoryName)) {
                return dir;
            }
        }
        System.out.println("Directory " + directoryName + " not found.");
        return null;
    }

    // 返回上级目录
    public Directory goBack() {
        return parent;
    }

    // 展示文件占用的磁盘块号
    public void showFileBlock(String fileName) {
        for (Inode inode : files) {
            if (inode.fileName.equals(fileName)) {
                System.out.println("File " + fileName + " occupies blocks: ");
                for (int blockIndex : inode.blockIndexes) {
                    System.out.print(blockIndex + " ");
                }
                System.out.println();
                return;
            }
        }
        System.out.println("File " + fileName + " not found.");
    }

    // 递归地展示目录结构
    public void showDirectoryStructure(String prefix) {
        System.out.println(prefix + name + "/");
        for (Inode inode : files) {
            System.out.println(prefix + "  " + inode.fileName);
        }
        for (Directory dir : subdirectories) {
            dir.showDirectoryStructure(prefix + "  ");
        }
    }
}
