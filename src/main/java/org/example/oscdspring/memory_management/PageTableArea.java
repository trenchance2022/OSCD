package org.example.oscdspring.memory_management;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// 单例模式
// 这个类是用来模拟内存系统区中存储页表的区域，根据页表起始地址，我们可以在这个区域中找到对应的页表，然后根据页表中的页号找到对应的页框号
// 使用map来存储页表，key是页表的起始地址，value是页表对象，简化了内存系统的实现(数组实现有点难/(ㄒoㄒ)/~~)
public class PageTableArea {
    private static final PageTableArea INSTANCE = new PageTableArea();

    private PageTableArea() {
    }

    public static PageTableArea getInstance() {
        return INSTANCE;
    }

    Map<Integer, PageTable> pageTables = new HashMap<>();

    private int addressSpace = 1000;//页表区域的地址空间大小

    // 添加页表,返回页表的起始地址
    public int addPageTable(int pid, int pageTableSize, int[] diskAddressBlock) {
        // 添加页表,返回页表的起始地址
        int pageTableAddress = getAddress();
        // 添加页表
        pageTables.put(pageTableAddress, new PageTable(pid, pageTableSize, diskAddressBlock));
        return pageTableAddress;

    }

    // 获取页表
    public PageTable getPageTable(int pageTableAddress) {
        // 获取页表
        if (pageTables.containsKey(pageTableAddress)) {
            return pageTables.get(pageTableAddress);
        }
        return null;
    }


    // 移除页表
    public void removePageTable(int pageTableAddress) {
        pageTables.remove(pageTableAddress);
    }

    private int getAddress() {
        // 随机生成一个地址
        Random random = new Random();
        int pageTableAddress = random.nextInt(addressSpace);
        int tryTime = 0;
        while (pageTables.containsKey(pageTableAddress)) {
            tryTime++;
            if (tryTime > addressSpace / 10) {
                addressSpace *= 10;
            }
            pageTableAddress = random.nextInt(addressSpace);
        }
        return pageTableAddress;
    }

}



