package process_management;

public class Interrupt {
    public enum InterruptType {
        CLOCK, IO
    }

    private InterruptType type;
    private int deviceId; // 对于IO中断，标识设备ID

    public Interrupt(InterruptType type) {
        this.type = type;
    }

    public Interrupt(InterruptType type, int deviceId) {
        this.type = type;
        this.deviceId = deviceId;
    }

    public InterruptType getType() {
        return type;
    }

    public int getDeviceId() {
        return deviceId;
    }
}
