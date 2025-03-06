public class Main {
    public static void main(String[] args) {
        FileDiskManagement fileDiskManagement = new FileSystemImpl();
        MemoryManagement memoryManagement = new MemoryManagementImpl();
        Shell shell = new Shell(fileDiskManagement, memoryManagement);
        shell.start();
    }
}
