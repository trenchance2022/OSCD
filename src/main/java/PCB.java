
public class PCB {
    // 别的属性


    private final int pid;

    // 进程大小，单位为B
    private int size;// 进程大小(代码段+数据段)
    // 下面的属性会传给PTR
    private final int codeSize;//代码段大小
    private final int innerFragmentation;//内部碎片(代码段与数据段之间的碎片)
    private int pageTableSize;
    private final int pageTableAddress;


    public PCB(int codeSize, int[] diskAddressBlock) {
        pid = PIDBitmap.getInstance().allocatePID();
        size = codeSize;
        this.codeSize = codeSize;
        pageTableSize = (codeSize - 1) / Constants.PAGE_SIZE_BYTES + 1;
        innerFragmentation = Constants.PAGE_SIZE_BYTES * pageTableSize - codeSize;
        pageTableAddress = PageTableArea.getInstance().addPageTable(pid, codeSize, diskAddressBlock);
    }

    public int getCodeSize() {
        return codeSize;
    }

    public int getInnerFragmentation() {
        return innerFragmentation;
    }


    public int getSize() {
        return size;
    }

    public void addSize(int addSize) {
        this.size += addSize;
    }

    public void addPage(int addSize) {
        this.pageTableSize += addSize;
    }


    public int getPageTableSize() {
        return pageTableSize;
    }


    public int getPageTableAddress() {
        return pageTableAddress;
    }


    public int getPID() {
        return pid;
    }
}


