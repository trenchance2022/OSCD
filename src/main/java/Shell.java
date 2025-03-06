import java.util.Scanner;

class Shell {
    FileDiskManagement fileDiskManagement;
    MemoryManagement memoryManagement;

    public Shell(FileDiskManagement fileDiskManagement, MemoryManagement memoryManagement) {
        this.fileDiskManagement = fileDiskManagement;
        this.memoryManagement = memoryManagement;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(fileDiskManagement.getCurrentPath() + "> ");  // 显示当前路径
            String command = scanner.nextLine();
            parseCommand(command);
        }
    }

    public void parseCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0]) {
            case "mkdir":
                if (parts.length == 2) {
                    fileDiskManagement.createDirectory(parts[1]);
                } else {
                    System.out.println("Usage: mkdir <directory>");
                }
                break;
            case "mkf":
                if (parts.length == 3) {
                    String fileName = parts[1];
                    int size = Integer.parseInt(parts[2]);
                    fileDiskManagement.createFile(fileName, size);
                } else {
                    System.out.println("Usage: mkf <filename> <size>");
                }
                break;
            case "cd":
                if (parts.length == 2) {
                    fileDiskManagement.changeDirectory(parts[1]);
                } else {
                    System.out.println("Usage: cd <directory>");
                }
                break;
            case "cd..":
                fileDiskManagement.goBack();
                break;
            case "cat":
                if (parts.length == 2) {
                    fileDiskManagement.showFileData(parts[1]);
                } else {
                    System.out.println("Usage: cat <filename>");
                }
                break;
            case "ls":
                fileDiskManagement.listDirectory();
                break;
            case "rm":
                if (parts.length == 2) {
                    fileDiskManagement.removeFile(parts[1]);
                } else {
                    System.out.println("Usage: rm <filename>");
                }
                break;
            case "rmdir":
                if (parts.length == 2) {
                    fileDiskManagement.removeDirectory(parts[1]);
                } else {
                    System.out.println("Usage: rmdir <directory>");
                }
                break;
            case "rmrdir":
                if (parts.length == 2) {
                    fileDiskManagement.removeDirectoryRecursively(parts[1]);
                } else {
                    System.out.println("Usage: rmrdir <directory>");
                }
                break;
            case "shf":
                if (parts.length == 2) {
                    fileDiskManagement.showFileBlock(parts[1]);
                } else {
                    System.out.println("Usage: shf <filename>");
                }
                break;
            case "vi":
                if (parts.length == 2) {
                    fileDiskManagement.editFile(parts[1]);
                } else {
                    System.out.println("Usage: vi <filename>");
                }
                break;
            case "info":
                if (parts.length == 2 && parts[1].equals("dir")) {
                    fileDiskManagement.showDirectoryStructure();
                } else if (parts.length == 2 && parts[1].equals("disk")) {
                    fileDiskManagement.displayDiskInfo();
                } else {
                    System.out.println("Unknown info command.");
                }
                break;
            default:
                System.out.println("Unknown command.");
        }
    }
}
