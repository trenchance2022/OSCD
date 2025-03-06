package process_management;

import memory_management.MMU;

public class CPU {

    // 有多个CPU，每个CPU都有一个MMU，并且有一个PCB标识当前运行的进程
    private final MMU mmu = new MMU();
    private PCB currentPCB;

    public PCB getCurrentPCB() {
        return currentPCB;
    }

    public MMU getMMU() {
        return mmu;
    }

    public void changeProcess(PCB currentPCB) {
        mmu.update(currentPCB);
        this.currentPCB = currentPCB;
    }

}
