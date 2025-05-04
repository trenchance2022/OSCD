package org.example.oscdspring;

import org.example.oscdspring.process_management.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.main.Library;

class ProcessManagementTest {

    @Test
    void testPIDAllocationAndReuse() {
        PIDBitmap pidMap = PIDBitmap.getInstance();
        int pid1 = pidMap.allocatePID();
        int pid2 = pidMap.allocatePID();
        assertNotEquals(-1, pid1, "First PID allocation failed");
        assertNotEquals(-1, pid2, "Second PID allocation failed");
        assertNotEquals(pid1, pid2, "Allocated PIDs should be unique");

        // Free one PID and ensure it can be reused
        pidMap.freePID(pid1);
        int pid3 = pidMap.allocatePID();
        assertEquals(pid1, pid3, "Freed PID should be reused for new allocation");
    }

    @Test
    void testSchedulerFCFSOrder() {
        Scheduler scheduler = Library.getScheduler();
        scheduler.configure(Scheduler.SchedulingPolicy.FCFS);
        // Create two processes with default priority (ignored in FCFS) and small code sizes
        PCB pcb1 = new PCB(PIDBitmap.getInstance().allocatePID(), 100, new int[]{0}, 0);
        PCB pcb2 = new PCB(PIDBitmap.getInstance().allocatePID(), 200, new int[]{0}, 0);
        pcb1.setState(ProcessState.READY);
        pcb2.setState(ProcessState.READY);
        // Add in order: pcb1 then pcb2
        scheduler.addReadyProcess(pcb1);
        scheduler.addReadyProcess(pcb2);

        PCB next1 = scheduler.getNextProcess();
        PCB next2 = scheduler.getNextProcess();
        PCB next3 = scheduler.getNextProcess();
        assertEquals(pcb1, next1, "FCFS should return first added process first");
        assertEquals(pcb2, next2, "FCFS should return second added process next");
        assertNull(next3, "No more processes should remain in queue");
    }

    @Test
    void testSchedulerPriorityScheduling() {
        Scheduler scheduler = Library.getScheduler();
        scheduler.configure(Scheduler.SchedulingPolicy.PRIORITY);
        // Create two processes with different priority values
        PCB highPri = new PCB(PIDBitmap.getInstance().allocatePID(), 50, new int[]{0}, 1);  // priority 1 (higher)
        PCB lowPri  = new PCB(PIDBitmap.getInstance().allocatePID(), 50, new int[]{0}, 5);  // priority 5 (lower)
        highPri.setState(ProcessState.READY);
        lowPri.setState(ProcessState.READY);
        // Add low priority first, then high priority
        scheduler.addReadyProcess(lowPri);
        scheduler.addReadyProcess(highPri);

        PCB first = scheduler.getNextProcess();
        PCB second = scheduler.getNextProcess();
        assertEquals(highPri, first, "Priority scheduling should pick process with higher priority (smaller number) first");
        assertEquals(lowPri, second, "Priority scheduling should pick lower priority process after higher ones are gone");
    }

    @Test
    void testSchedulerMLFQOrderAndTimeSlice() {
        Scheduler scheduler = Library.getScheduler();
        scheduler.configure(Scheduler.SchedulingPolicy.MLFQ);
        // Create two processes assigned to different queues (simulate initial priority levels for MLFQ)
        PCB procQ0 = new PCB(PIDBitmap.getInstance().allocatePID(), 100, new int[]{0}, 0);  // starts in queue 0
        PCB procQ2 = new PCB(PIDBitmap.getInstance().allocatePID(), 100, new int[]{0}, 2);  // starts in queue 2
        procQ0.setState(ProcessState.READY);
        procQ2.setState(ProcessState.READY);
        scheduler.addReadyProcess(procQ2);
        scheduler.addReadyProcess(procQ0);

        // In MLFQ, process in higher queue (0) should be scheduled before lower queue (2)
        PCB next = scheduler.getNextProcess();
        assertEquals(procQ0, next, "MLFQ should schedule process from higher-priority queue first");
        // The process from queue 2 should be next after queue 0 is empty
        PCB next2 = scheduler.getNextProcess();
        assertEquals(procQ2, next2, "MLFQ should schedule lower queue process after higher queues are empty");

        // Verify time slice assignments for MLFQ levels (queue 0 -> 500ms, queue 2 -> 2000ms as per Constants)
        assertEquals(500, procQ0.getTimeSlice(), "MLFQ queue 0 process should have time slice 500ms");
        assertEquals(2000, procQ2.getTimeSlice(), "MLFQ queue 2 process should have time slice 2000ms");
    }

    @Test
    void testSchedulerRoundRobinTimeSlice() {
        Scheduler scheduler = Library.getScheduler();
        scheduler.configure(Scheduler.SchedulingPolicy.RR);
        PCB pcb = new PCB(PIDBitmap.getInstance().allocatePID(), 300, new int[]{0}, 0);
        pcb.setState(ProcessState.READY);
        scheduler.addReadyProcess(pcb);
        // In RR policy, time slice for queue 0 should be set (100ms as configured)
        assertEquals(100, pcb.getTimeSlice(), "Round-Robin scheduling should assign 100ms time slice to process");
    }

    @Test
    void testCPUChangeProcessAndIsIdle() {
        Scheduler scheduler = Library.getScheduler();
        DeviceManager devMgr = new DeviceManager();
        CPU cpu = new CPU(0, scheduler, devMgr);
        // Initially, CPU has no process
        assertTrue(cpu.isIdle(), "CPU should be idle when no current process is set");

        // Create a process and simulate scheduling it to CPU
        PCB pcb = new PCB(PIDBitmap.getInstance().allocatePID(), 128, new int[]{0}, 3);
        pcb.setState(ProcessState.RUNNING);
        cpu.changeProcess(pcb);
        assertEquals(pcb, cpu.getCurrentPCB(), "CPU current process should be updated after changeProcess");
        assertFalse(cpu.isIdle(), "CPU should not be idle when a process is running");

        // Simulate process termination
        pcb.setState(ProcessState.TERMINATED);
        assertTrue(cpu.isIdle(), "CPU should be idle after the running process is terminated");
    }
}
