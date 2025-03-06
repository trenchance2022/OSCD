import java.util.Arrays;

public class PIDBitmap {
    public static final int MAX_PID = 10240;
    public static final int SYSTEM_PID = 0;
    public static final int EMPTY_PID = -1;

    private final boolean[] bitmap = new boolean[MAX_PID];

    private static final PIDBitmap pidBitmap = new PIDBitmap();

    public static PIDBitmap getInstance() {
        return pidBitmap;
    }

    private PIDBitmap() {
        Arrays.fill(bitmap, false);
        bitmap[SYSTEM_PID] = true;
    }

    public int allocatePID() {
        for (int i = 1; i < MAX_PID; i++) {
            if (!bitmap[i]) {
                bitmap[i] = true;
                return i;
            }
        }
        return -1;
    }

    public void freePID(int pid) {
        if (pid > 0 && pid < MAX_PID) {
            bitmap[pid] = false;
        }
    }

}
