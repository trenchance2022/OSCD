package org.example.oscdspring.main;

import org.example.oscdspring.device_management.DeviceManager;
import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.memory_management.MemoryManagement;
import org.example.oscdspring.util.LogEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Shell {
    private final FileSystemImpl fileSystem;
    private final MemoryManagement memoryManagement;
    private final DeviceManager deviceManager;
    private final LogEmitterService logEmitterService;

    @Autowired
    public Shell(FileSystemImpl fileSystem, MemoryManagement memoryManagement, DeviceManager deviceManager,
                 LogEmitterService logEmitterService) {
        this.fileSystem = fileSystem;
        this.memoryManagement = memoryManagement;
        this.deviceManager = deviceManager;
        this.logEmitterService = logEmitterService;
    }

    /**
     * 接收外部调用的命令，执行命令后返回状态信息。
     * 实际日志输出均通过 LogEmitterService 实时推送到前端，无需汇总返回。
     */
    public String processCommand(String command) {
        parseCommand(command);
        return "Command processed: " + command;
    }

    /**
     * 解析命令字符串，并调用各模块方法执行操作。
     * 所有需要提示的输出均直接调用 logEmitterService.sendLog() 进行实时推送。
     */
    private void parseCommand(String command) {
        String[] parts = command.trim().split(" ");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }
        switch (parts[0]) {
            case "mkdir":
                if (parts.length == 2) {
                    fileSystem.createDirectory(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: mkdir <directory>");
                }
                break;
            case "mkf":
                if (parts.length == 3) {
                    String fileName = parts[1];
                    try {
                        int size = Integer.parseInt(parts[2]);
                        fileSystem.createFile(fileName, size);
                    } catch (NumberFormatException e) {
                        logEmitterService.sendLog("Invalid size: must be an integer");
                    }
                } else {
                    logEmitterService.sendLog("Usage: mkf <filename> <size>");
                }
                break;
            case "cd":
                if (parts.length == 2) {
                    fileSystem.changeDirectory(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: cd <directory>");
                }
                break;
            case "cd..":
                fileSystem.goBack();
                break;
            case "cat":
                if (parts.length == 2) {
                    fileSystem.showFileData(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: cat <filename>");
                }
                break;
            case "ls":
                fileSystem.listDirectory();
                break;
            case "rm":
                if (parts.length == 2) {
                    fileSystem.removeFile(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: rm <filename>");
                }
                break;
            case "rmdir":
                if (parts.length == 2) {
                    fileSystem.removeDirectory(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: rmdir <directory>");
                }
                break;
            case "rmrdir":
                if (parts.length == 2) {
                    fileSystem.removeDirectoryRecursively(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: rmrdir <directory>");
                }
                break;
            case "shf":
                if (parts.length == 2) {
                    fileSystem.showFileBlock(parts[1]);
                } else {
                    logEmitterService.sendLog("Usage: shf <filename>");
                }
                break;
            case "vi":
                if (parts.length == 2) {
                    String fileName = parts[1];
                    if (!fileSystem.fileExists(fileName)) {
                        fileSystem.createFile(fileName, 1);
                        logEmitterService.sendLog("New file " + fileName + " created for editing.");
                        // 发送特殊指令 OPEN_VI_* 表明这是新建文件编辑
                        logEmitterService.sendLog("OPEN_VI_*:" + fileName);
                    } else {
                        // 文件已存在，发送普通的打开编辑指令
                        logEmitterService.sendLog("OPEN_VI:" + fileName);
                    }
                } else {
                    logEmitterService.sendLog("Usage: vi <filename>");
                }
                break;
            case "exec":
                if (parts.length >= 3 && ((parts.length - 1) % 2 == 0)) {
                    try {
                        for (int i = 1; i < parts.length; i += 2) {
                            String filename = parts[i];
                            int priority = Integer.parseInt(parts[i + 1]);
                            String fileContent = fileSystem.readFileData(filename);
                            if (!fileContent.equals("-1")) {
                                org.example.oscdspring.process_management.Scheduler.getInstance()
                                        .createProcess(filename, priority);
                            } else {
                                logEmitterService.sendLog("File " + filename + " does not exist or cannot be read");
                            }
                        }
                    } catch (NumberFormatException e) {
                        logEmitterService.sendLog("Priority must be an integer");
                    }
                } else {
                    logEmitterService.sendLog("Usage: exec <filename1> <priority1> [<filename2> <priority2> ...]");
                }
                break;
            case "info":
                if (parts.length == 2 && parts[1].equals("dir")) {
                    fileSystem.showDirectoryStructure();
                } else if (parts.length == 2 && parts[1].equals("disk")) {
                    fileSystem.displayDiskInfo();
                } else if (parts.length == 2 && parts[1].equals("memory")) {
                    memoryManagement.showPageUse(0, Constants.MEMORY_PAGE_SIZE);
                } else {
                    logEmitterService.sendLog("Unknown info command.");
                }
                break;
            case "addevc":
                if (parts.length == 3) {
                    String deviceName = parts[1];
                    try {
                        int deviceId = Integer.parseInt(parts[2]);
                        deviceManager.addDevice(deviceId, deviceName);
                    } catch (NumberFormatException e) {
                        logEmitterService.sendLog("Device ID must be an integer");
                    }
                } else {
                    logEmitterService.sendLog("Usage: addevc <deviceName> <deviceId>");
                }
                break;
            case "rmdevc":
                if (parts.length == 3) {
                    String deviceName = parts[1];
                    try {
                        int deviceId = Integer.parseInt(parts[2]);
                        deviceManager.removeDevice(deviceId, deviceName);
                    } catch (NumberFormatException e) {
                        logEmitterService.sendLog("Device ID must be an integer");
                    }
                } else {
                    logEmitterService.sendLog("Usage: rmdevc <deviceName> <deviceId>");
                }
                break;
            case "kill":
                if (parts.length == 2) {
                    try {
                        int pid = Integer.parseInt(parts[1]);
                        org.example.oscdspring.process_management.Scheduler.getInstance()
                                .terminateProcess(org.example.oscdspring.process_management.PCB.getPCB(pid));
                    } catch (NumberFormatException e) {
                        logEmitterService.sendLog("Process ID must be an integer");
                    }
                } else {
                    logEmitterService.sendLog("Usage: kill <pid>");
                }
                break;
            default:
                logEmitterService.sendLog("Unknown command.");
        }
    }

    // 原来的 start() 方法保留用于命令行测试，不用于 Web 交互
    public void start() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print(fileSystem.getCurrentPath() + "> ");
            String command = scanner.nextLine();
            parseCommand(command);
        }
    }
}
