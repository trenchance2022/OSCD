package interrupt_management;

public class Interrupt {
    public enum InterruptType {
        CLOCK,
        IO,
        DEVICE
    }

    private InterruptType type;

    public Interrupt(InterruptType type) {
        this.type = type;
    }

    public InterruptType getType() {
        return type;
    }

}
