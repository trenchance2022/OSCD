package org.example.oscdspring;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.example.oscdspring.main.Library;
import org.example.oscdspring.memory_management.MemoryManagement;
import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.process_management.CPU;
import org.example.oscdspring.process_management.PCB;
import org.example.oscdspring.process_management.PIDBitmap;
import org.example.oscdspring.process_management.Scheduler;
import org.example.oscdspring.util.LogEmitterService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

public class CPUTest {

    private CPU cpu;
    private MemoryManagement mockMem;
    private Scheduler mockScheduler;
    private DeviceManager mockDeviceManager;
    private MockedStatic<Library> libStatic;
    private MockedStatic<LogEmitterService> logStatic;
    private LogEmitterService mockLogger;


    @BeforeEach
    public void setUp() {
        // Mock MemoryManagement
        mockMem = mock(MemoryManagement.class);
        libStatic = mockStatic(Library.class);
        libStatic.when(Library::getMemoryManagement)
                .thenReturn(mockMem);

        // Mock LogEmitterService singleton
        mockLogger = mock(LogEmitterService.class);
        logStatic = mockStatic(LogEmitterService.class);
        logStatic.when(LogEmitterService::getInstance)
                .thenReturn(mockLogger);


        when(mockMem.toString()).thenReturn("SafeMemoryManagement");
        // 其它 mock 和 CPU 实例化
        mockScheduler = mock(Scheduler.class);
        mockDeviceManager = mock(DeviceManager.class);
        cpu = new CPU(0, mockScheduler, mockDeviceManager);
        cpu.setMemoryManagement(mockMem);

    }

    @AfterEach
    public void tearDown() {
        libStatic.close();
        logStatic.close();
    }

    @Test
    public void testInstructionFetch_readsUntilHash() {



        PCB pcb = new PCB(1, 0, new int[]{0}, 0);
        pcb.setPc(50);
        pcb.setRemainInstruction("");
        cpu.setCurrentPCB(pcb);
        byte[] buf = new byte[1];

//        when(mockMem.Read(
//                eq(cpu),                    // CPU 对象
//                eq(50),                     // 逻辑地址
//                eq(buf),          // 匹配任意 byte[]，而不是特定的 buf 引用 :contentReference[oaicite:0]{index=0}
//                eq(1)                       // 长度
//        ))
//                .thenAnswer(invocation -> {
//                    byte[] b = invocation.getArgument(2);
//                    b[0] = 'X';
//                    return true;
//                })
//                .thenAnswer(invocation -> {
//                    byte[] b = invocation.getArgument(2);
//                    b[0] = 'Y';
//                    return true;
//                })
//                .thenAnswer(invocation -> {
//                    byte[] b = invocation.getArgument(2);
//                    b[0] = '#';
//                    return true;
//                });
        doAnswer(invocation -> {
            byte[] b = invocation.getArgument(2);
            b[0] = 'X';
            return true;
        }).when(mockMem).Read(
                eq(cpu),
                eq(50),
                any(byte[].class),
                eq(1)
        );
        doAnswer(invocation -> {
            byte[] b = invocation.getArgument(2);
            b[0] = 'Y';
            return true;
        }).when(mockMem).Read(
                eq(cpu),
                eq(51),
                any(byte[].class),
                eq(1)
        );
        doAnswer(invocation -> {
            byte[] b = invocation.getArgument(2);
            b[0] = '#';
            return true;
        }).when(mockMem).Read(
                eq(cpu),
                eq(52),
                any(byte[].class),
                eq(1)
        );

        // 4. 执行并断言
        String inst = cpu.InstructionFetchForTest();
        assertEquals("XY", inst);
        // 50 + 3 次读取后，PC 应该是 53
        assertEquals(53, pcb.getPc());

        // 验证 Read 被调用了 3 次
        verify(mockMem, times(3))
                .Read(eq(cpu), anyInt(), any(byte[].class), eq(1));
    }


    @Test
    public void testInstructionFetch_withRemainInstruction_skipsMemoryRead() {
        // 准备 PCB，预先设置 remainInstruction
        PCB pcb = new PCB(1, 0, new int[]{0}, 0);
        pcb.setPc(0);
        pcb.setRemainInstruction("AB");
        cpu.setCurrentPCB(pcb);

        // 调用 InstructionFetch
        String inst = cpu.InstructionFetchForTest();

        // 应当直接返回 remainInstruction，且不触发任何内存读取
        assertEquals("AB", inst);

        // 验证 Read 从未被调用
        verify(mockMem, never()).Read(any(), anyInt(), any(), eq(1));   // :contentReference[oaicite:0]{index=0}
    }


    @Test
    public void testExecuteInstruction_computeUpdatesTimeUsed() throws InterruptedException {
        // Prepare PCB and scheduler policy for non-preemptive (e.g. FCFS)
        PCB pcb = new PCB(2, 0, new int[]{10}, 0);
        pcb.setPc(0);
        pcb.setTimeSlice(1000);
        gpuSetPolicy(Scheduler.SchedulingPolicy.FCFS);
        cpu.setCurrentPCB(pcb);

        // Execute compute instruction "C 200"
        cpu.executeInstructionForTest("C 200");

        // After execution, total time used should equal 200
        assertEquals(200, pcb.getTimeUsed());
        // RemainInstruction should be cleared
        assertEquals("", pcb.getRemainInstruction());
    }

    // Helper to mock scheduler policy
    private void gpuSetPolicy(Scheduler.SchedulingPolicy policy) {
        when(mockScheduler.getCurrentPolicy()).thenReturn(policy);
        when(mockScheduler.getNextProcess()).thenReturn(null);
    }
}