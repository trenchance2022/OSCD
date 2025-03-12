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
    private Object process;   // 进程代码
    private String executablePath;  //执行文件路径
    // 下面的属性会传给PTR
    private int codeSize = 0;//代码段大小
    private int innerFragmentation = 0;//内部碎片(代码段与数据段之间的碎片)
    private int pageTableSize = 0;
    private int pageTableAddress = -1;

    // 用于创建进程对象的构造函数
    public PCB(int pid, int priority, Object process) {
        this.pid = pid;
        this.priority = priority;
        this.process = process;
        this.state = ProcessState.NEW;
        this.timeUsed = 0;
        // 根据优先级设置时间片大小
        setTimeSliceByPriority();
        // this.size = codeSize;
        // this.codeSize = codeSize;
        // this.pageTableSize = (codeSize - 1) / Constants.PAGE_SIZE_BYTES + 1;
        // this.innerFragmentation = Constants.PAGE_SIZE_BYTES * pageTableSize - codeSize;
        // this.pageTableAddress = PageTableArea.getInstance().addPageTable(pid, codeSize, diskAddressBlock);
    }

    // 用于创建带内存管理的进程对象的构造函数
    public PCB(int pid, int codeSize, int[] diskAddressBlock, int priority, Object process) {
        this.pid = pid;
        this.priority = priority;
        this.process = process;
        this.state = ProcessState.NEW;
        this.timeUsed = 0;
        this.codeSize = codeSize;
        this.size = codeSize;
        this.pageTableSize = (codeSize - 1) / Constants.PAGE_SIZE_BYTES + 1;
        this.innerFragmentation = Constants.PAGE_SIZE_BYTES * pageTableSize - codeSize;
        
        // 只有在提供了磁盘地址块时才创建页表
        if (diskAddressBlock != null && diskAddressBlock.length > 0) {
            this.pageTableAddress = PageTableArea.getInstance().addPageTable(pid, codeSize, diskAddressBlock);
        }
        
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

    public void incrementTimeUsed() {
        this.timeUsed++;
    }

    public void resetTimeUsed() {
        this.timeUsed = 0;
    }

    public Object getProcess() {
        return process;
    }

    public void setExecutablePath(String path) {
        this.executablePath = path;
    }

    public String getExecutablePath() {
        return executablePath;
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


