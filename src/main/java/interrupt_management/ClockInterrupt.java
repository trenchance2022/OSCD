package interrupt_management;

import main.Constants;

public class ClockInterrupt extends Thread {
    private InterruptRequestLine interruptRequestLine;
    private volatile boolean running = true;

    public ClockInterrupt(InterruptRequestLine interruptRequestLine, int clockInterval) {
        this.interruptRequestLine = interruptRequestLine;
    }

    @Override
    public void run() {
        try {
            while (running) {
                // 通过中断请求线向所有CPU广播时钟中断
                interruptRequestLine.broadcastInterrupt(new Interrupt(Interrupt.InterruptType.CLOCK));
                Thread.sleep(Constants.CLOCK_INTERRUPT_INTERVAL_MS);
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