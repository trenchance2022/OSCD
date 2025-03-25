package main;

// 常量类
public final class Constants {

    // 私有构造函数，防止实例化
    private Constants() {
        throw new UnsupportedOperationException("main.Constants class cannot be instantiated");
    }

    // 常数
    public static final int PAGE_SIZE_BYTES = 1024; // 页大小1024B
    public static final int DISK_SIZE = 1024; // 磁盘大小1024块
    public static final int MEMORY_PAGE_SIZE = 128; // 内存大小128页
    public static final int TLB_SIZE = 8; // TLB大小
    public static final int SYSTEM_MEMORY_PAGE_SIZE = 16; // 系统内存页大小
    public static final int BLOCK_SIZE_BYTES = PAGE_SIZE_BYTES; // 块大小
    public static final int CLOCK_INTERRUPT_INTERVAL_MS = 100; // 时钟中断间隔(毫秒)

    
}