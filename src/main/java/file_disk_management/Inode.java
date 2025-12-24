package file_disk_management;

class Inode {
    int inodeNumber;  // inode编号
    String fileName;  // 文件名
    int size;         // 文件大小，单位字节
    int[] blockIndexes;  // 存储文件的非连续块索引

    public Inode(int inodeNumber, String fileName, int size) {
        this.inodeNumber = inodeNumber;
        this.fileName = fileName;
        this.size = size;
        // 计算所需的块数
        this.blockIndexes = new int[(int)Math.ceil((double)size / 1024)];
    }
}
