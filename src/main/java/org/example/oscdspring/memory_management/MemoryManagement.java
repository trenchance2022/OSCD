package org.example.oscdspring.memory_management;

import org.example.oscdspring.process_management.CPU;

//内存管理模块对外接口
public interface MemoryManagement {

    /**
     * 给进程分配再分配指定大小的内存空间，并返回是否成功分配 进程刚创建时，已经分配了一个页表，并且给进程本身分配了内存（在PCD的构造函数中）
     * 如果进程运行时需要更多内存，就需要再分配内存，此时调用该函数
     *
     * @param cpu CPU对象,用于获取当前运行的进程,以及进程的快表，页表等信息
     * @param size 分配的内存大小，byte(实际为虚拟页内存大小)
     * @return 是否成功分配内存空间
     */
    public boolean Allocate(CPU cpu, int size);

    /**
     * 释放指定进程的内存空间。
     *
     * @param cpu 待释放的进程的CPU对象
     * @return 是否成功释放内存空间
     */
    public boolean FreeProcess(CPU cpu);

    /**
     * 从指定地址的内存中读取数据。
     *
     * @param cpu CPU对象,用于获取当前运行的进程,以及进程的快表，页表等信息
     * @param logicAddress 进程逻辑地址
     * @param data 实际返回的数据
     * @param length 读取的数据长度
     * @return 是否成功读取内存空间
     */
    public boolean Read(CPU cpu, int logicAddress, byte[] data, int length);

    /**
     * 向指定地址的内存中写入数据。 注意：根据返回值的不同，data有两种含义
     *
     * @param cpu CPU对象,用于获取当前运行的进程,以及进程的快表，页表等信息
     * @param logicAddress 进程逻辑地址
     * @param data 要写入的数据
     * @param length 写入的数据长度
     * @return 写入是否成功
     */
    public boolean Write(CPU cpu, int logicAddress, byte[] data, int length);

    /**
     * 获取内存的页面使用情况位图。 这里直接打印出来,是由Memory实现的
     *
     * @param start 起始页号
     * @param end 结束页号(不包括)
     */
    public void showPageUse(int start, int end);

}
