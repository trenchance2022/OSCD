package process_management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 中断请求线类，负责管理中断源和CPU之间的中断信号传递
 */
public class InterruptRequestLine {
    // 单例模式
    private static InterruptRequestLine instance;
    
    // 存储每个CPU的中断队列
    private Map<Integer, BlockingQueue<Interrupt>> cpuInterruptQueues;
    
    private InterruptRequestLine() {
        cpuInterruptQueues = new HashMap<>();
    }
    
    public static synchronized InterruptRequestLine getInstance() {
        if (instance == null) {
            instance = new InterruptRequestLine();
        }
        return instance;
    }
    
    /**
     * 注册CPU的中断队列
     * @param cpuId CPU的ID
     * @return 为该CPU创建的中断队列
     */
    public BlockingQueue<Interrupt> registerCPU(int cpuId) {
        BlockingQueue<Interrupt> queue = new LinkedBlockingQueue<>();
        cpuInterruptQueues.put(cpuId, queue);
        return queue;
    }
    
    /**
     * 获取CPU的中断队列
     * @param cpuId CPU的ID
     * @return CPU的中断队列
     */
    public BlockingQueue<Interrupt> getCPUInterruptQueue(int cpuId) {
        return cpuInterruptQueues.get(cpuId);
    }
    
    /**
     * 获取所有CPU的中断队列列表
     * @return 所有CPU的中断队列列表
     */
    public List<BlockingQueue<Interrupt>> getAllCPUInterruptQueues() {
        return new ArrayList<>(cpuInterruptQueues.values());
    }
    
    /**
     * 向指定CPU发送中断
     * @param cpuId CPU的ID
     * @param interrupt 中断信号
     * @return 是否成功发送
     */
    public boolean sendInterrupt(int cpuId, Interrupt interrupt) {
        BlockingQueue<Interrupt> queue = cpuInterruptQueues.get(cpuId);
        if (queue != null) {
            try {
                queue.put(interrupt);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    /**
     * 向所有CPU广播中断
     * @param interrupt 中断信号
     */
    public void broadcastInterrupt(Interrupt interrupt) {
        for (BlockingQueue<Interrupt> queue : cpuInterruptQueues.values()) {
            try {
                queue.put(interrupt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}