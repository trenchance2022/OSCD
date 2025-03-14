package process_management;

import memory_management.PageTableArea;
import main.Constants;

public class PCB {
    private final int pid;  // 进程id
    private int size;       // 进程大小(代码段+数据段)(单位为B)
    private int priority;   // 进程优先级
    private ProcessState state; // 进程状态
    private int timeSlice;  // 分配的时间片
    private int timeUsed;   // 已使用的时间片
    private String executedFile;  //执行文件
    private int pc;         //程序计数器
    // 下面的属性会传给PTR
    private int codeSize;//代码段大小
    private int innerFragmentation;//内部碎片(代码段与数据段之间的碎片)
    private int pageTableSize;
    private int pageTableAddress;

    public PCB(int pid, int codeSize, int[] diskAddressBlock, int priority) {
        this.pid = pid;
        this.priority = priority;
        this.state = ProcessState.NEW;
        this.timeUsed = 0;
        this.codeSize = codeSize;
        this.size = codeSize;
        this.pageTableSize = (codeSize - 1) / Constants.PAGE_SIZE_BYTES + 1;
        this.innerFragmentation = Constants.PAGE_SIZE_BYTES * pageTableSize - codeSize;
        this.pageTableAddress = PageTableArea.getInstance().addPageTable(pid, codeSize, diskAddressBlock);
        setTimeSliceByPriority();
    }

    private void setTimeSliceByPriority() {
        // 优先级越高，时间片越小
        // 确保时间片是时钟中断间隔的整倍数
        switch (priority) {
            case 0: this.timeSlice = 2 * Constants.CLOCK_INTERRUPT_INTERVAL_MS; break;  // 最高优先级
            case 1: this.timeSlice = 4 * Constants.CLOCK_INTERRUPT_INTERVAL_MS; break;
            case 2: this.timeSlice = 8 * Constants.CLOCK_INTERRUPT_INTERVAL_MS; break;
            default: this.timeSlice = 16 * Constants.CLOCK_INTERRUPT_INTERVAL_MS; break; // 最低优先级，FCFS
        }
    }

    public int getPid() {
        return pid;
    }

    public int getSize() {
        return size;
    }

    public void addSize(int addSize) {
        this.size += addSize;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        setTimeSliceByPriority();
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getTimeSlice() {
        return timeSlice;
    }

    public int getTimeUsed() {
        return timeUsed;
    }

    public void incrementTimeUsed(int time) {
        // 每次时钟中断增加的时间等于时钟中断间隔
        this.timeUsed += time;
    }

    public void resetTimeUsed() {
        this.timeUsed = 0;
    }

    public void setExecutedFile(String path) {
        this.executedFile = path;
    }

    public String getExecutedFile() {
        return executedFile;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getCodeSize() {
        return codeSize;
    }

    public int getInnerFragmentation() {
        return innerFragmentation;
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

    public void setPageTableAddress(int pageTableAddress) {
        this.pageTableAddress = pageTableAddress;
    }
}


