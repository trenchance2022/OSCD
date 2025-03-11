package process_management;

import java.util.concurrent.BlockingQueue;

public class ClockInterrupt extends Thread {
    private BlockingQueue<Interrupt> interruptQueue;
    private volatile boolean running = true;
    private int clockInterval; // 时钟中断间隔（毫秒）
    private int cpuCount; // CPU数量

    public ClockInterrupt(BlockingQueue<Interrupt> interruptQueue, int clockInterval) {
        this.interruptQueue = interruptQueue;
        this.clockInterval = clockInterval;
        this.cpuCount = cpuCount;
    }

    @Override
    public void run() {
        try {
            while (running) {
                // 为每个CPU生成一个时钟中断
                for (int i = 0; i < cpuCount; i++) {
                    interruptQueue.put(new Interrupt(Interrupt.InterruptType.CLOCK));
                }
                Thread.sleep(clockInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
