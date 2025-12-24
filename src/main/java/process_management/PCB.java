package process_management;

import memory_management.PageTableArea;
import main.Constants;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PCB {

    // 存储所有活跃PCB的线程安全列表
    private static List<PCB> activePCBs = Collections.synchronizedList(new ArrayList<>());

    private int currentQueue; // 当前所在队列（MLFQ使用）
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
    private String originalInstruction; // 当前正在执行的指令
    private String remainInstruction;   // 被抢占后剩余的执行时间的执行指令

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
        this.originalInstruction = "";
        this.remainInstruction = "";
        
        
    }

    // 获取所有活跃PCB的列表
    public static List<PCB> getActivePCBs() {
        return Collections.unmodifiableList(activePCBs);
    }

    // 从活跃PCB列表中移除PCB
    public void removePCB() {
        activePCBs.remove(this);
    }

    // 将PCB添加到活跃PCB列表中
    public void addPCB() {
        activePCBs.add(this);
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

    public void setTimeSlice(int timeSlice) {
        this.timeSlice = timeSlice;
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

    public String getOriginalInstruction() {
        return originalInstruction;
    }

    public void setOriginalInstruction(String originalInstruction) {
        this.originalInstruction = originalInstruction;
    }

    public String getRemainInstruction() {
        return remainInstruction;
    }

    public void setRemainInstruction(String remainInstruction) {
        this.remainInstruction = remainInstruction;
    }
    public int getCurrentQueue() {
        return currentQueue;
    }

    public void setCurrentQueue(int queue) {
        this.currentQueue = Math.min(queue, 3);
    }

    public static PCB getPCB(int pid){
        for(PCB pcb:PCB.activePCBs) {
            if (pid == pcb.pid) {
                return pcb;
            }
        }
        return null;
    }
}
