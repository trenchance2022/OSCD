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
    private Runnable process;   // 进程代码
    private String executablePath;  //执行文件路径
    // 下面的属性会传给PTR
    private final int codeSize;//代码段大小
    private final int innerFragmentation;//内部碎片(代码段与数据段之间的碎片)
    private int pageTableSize;
    private final int pageTableAddress;


    public PCB(int codeSize, int[] diskAddressBlock, int priority, Runnable process) {
        this.pid = PIDBitmap.getInstance().allocatePID();
        this.priority = priority;
        this.process = process;
        this.state = ProcessState.NEW;
        this.timeUsed = 0;
        this.size = codeSize;
        this.codeSize = codeSize;
        this.pageTableSize = (codeSize - 1) / Constants.PAGE_SIZE_BYTES + 1;
        this.innerFragmentation = Constants.PAGE_SIZE_BYTES * pageTableSize - codeSize;
        this.pageTableAddress = PageTableArea.getInstance().addPageTable(pid, codeSize, diskAddressBlock);
        // 根据优先级设置时间片大小
        setTimeSliceByPriority();
    }

    private void setTimeSliceByPriority() {
        // 优先级越高，时间片越小
        switch (priority) {
            case 0: this.timeSlice = 2; break;  // 最高优先级
            case 1: this.timeSlice = 4; break;
            case 2: this.timeSlice = 8; break;
            default: this.timeSlice = 16; break; // 最低优先级，FCFS
        }
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        setTimeSliceByPriority();
    }

    public int getTimeSlice() {
        return timeSlice;
    }

    public int getTimeUsed() {
        return timeUsed;
    }

    public void resetTimeUsed() {
        this.timeUsed = 0;
    }

    public Runnable getProcess() {
        return process;
    }

    public void incrementTimeUsed() {
        this.timeUsed++;
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

    public void setExecutablePath(String path) {
        this.executablePath = path;
    }

    public String getExecutablePath() {
        return executablePath;
    }

    @Override
    public String toString() {
        return "PCB{" +
                "pid=" + pid +
                ", state=" + state +
                ", priority=" + priority +
                ", timeSlice=" + timeSlice +
                ", timeUsed=" + timeUsed +
                '}';
    }
}


