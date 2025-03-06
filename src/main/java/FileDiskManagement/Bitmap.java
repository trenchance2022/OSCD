class Bitmap {
    private int[] bitmap;   // 位图，1表示已分配，0表示空闲
    private int size;       // 位图的大小（即磁盘块的总数）

    public Bitmap(int size) {
        this.size = size;
        bitmap = new int[size];  // 每个块的状态（0或1）
    }

    // 获取一个空闲块
    public int allocateBlock() {
        for (int i = 0; i < size; i++) {
            if (bitmap[i] == 0) {   // 找到一个空闲块
                bitmap[i] = 1;      // 标记为已分配
                return i;           // 返回块的索引
            }
        }
        return -1;  // 没有空闲块
    }

    // 释放一个块
    public void freeBlock(int blockIndex) {
        bitmap[blockIndex] = 0;  // 将块标记为空闲
    }

    // 获取磁盘的状态（显示位图）
    public void displayBitmap() {
        for (int i = 0; i < size; i++) {
            System.out.print(bitmap[i] + " ");
        }
        System.out.println();
    }
}
