package org.example.oscdspring;

import org.example.oscdspring.device_management.*;
import org.example.oscdspring.process_management.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.example.oscdspring.main.Library;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DeviceManagementTest {

    @Test
    void testAddAndRemoveDevice() {
        DeviceManager manager = new DeviceManager();
        int devId = 1;
        String devName = "Printer";
        manager.addDevice(devId, devName);
        // Verify the device is registered
        assertTrue(manager.deviceExists(devId, devName), "Device should exist after being added");
        // Removing the device
        manager.removeDevice(devId, devName);
        assertFalse(manager.deviceExists(devId, devName), "Device should be removed from manager");
    }

    @Test
    void testIORequestQueuingAndProcessWaiting() {
        DeviceManager manager = new DeviceManager();
        // Manually add a device without starting thread to control queue
        IODevice device = new IODevice(2, "Disk");
        DeviceManager.getDevices().put("Disk_2", device);
        // Create two dummy processes and send I/O requests
        PCB pcb1 = new PCB(PIDBitmap.getInstance().allocatePID(), 128, new int[]{0}, 1);
        PCB pcb2 = new PCB(PIDBitmap.getInstance().allocatePID(), 128, new int[]{0}, 1);
        pcb1.setState(ProcessState.READY);
        pcb2.setState(ProcessState.READY);
        manager.requestIO(pcb1, 300, "Disk", 2);
        manager.requestIO(pcb2, 100, "Disk", 2);

        // After requesting I/O, processes should be in WAITING state
        assertEquals(ProcessState.WAITING, pcb1.getState(), "Process should be WAITING after I/O request");
        assertEquals(ProcessState.WAITING, pcb2.getState(), "Process should be WAITING after I/O request");
        // The IODevice queue should contain both requests in FIFO order
        IORequest firstRequest = device.getRequests().poll();
        IORequest secondRequest = device.getRequests().poll();
        assertNotNull(firstRequest, "First I/O request should be queued");
        assertNotNull(secondRequest, "Second I/O request should be queued");
        assertEquals(pcb1, firstRequest.getPcb(), "First queued request should correspond to first process");
        assertEquals(pcb2, secondRequest.getPcb(), "Second queued request should correspond to second process");
        // After processing (simulated by removing from queue), processes should be removed from waiting queue
        Library.getScheduler().removeWaitingProcess(pcb1);
        Library.getScheduler().removeWaitingProcess(pcb2);
        // (In a full integration test, InterruptHandler would move these back to READY after I/O completion)
    }
}
