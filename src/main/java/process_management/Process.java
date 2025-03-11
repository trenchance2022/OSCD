package process_management;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Process implements Runnable {
    private String executablePath;
    private List<String> instructions;
    private int currentInstructionIndex;
    private int pid;

    public Process(String executablePath) {
        this.executablePath = executablePath;
        this.instructions = new ArrayList<>();
        this.currentInstructionIndex = 0;
        loadInstructions();
    }

    private void loadInstructions() {
        try (BufferedReader reader = new BufferedReader(new FileReader(executablePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    instructions.add(line.trim());
                }
            }
            System.out.println("从文件 " + executablePath + " 加载了 " + instructions.size() + " 条指令");
        } catch (IOException e) {
            System.err.println("无法读取可执行文件: " + executablePath);
            e.printStackTrace();
        }
    }

    public void setPid(int pid) {
        if (pid != PIDBitmap.EMPTY_PID) {
            this.pid = pid;
        } else {
            throw new IllegalArgumentException("无效的PID: " + pid);
        }
    }

    public int getPid() {
        return pid;
    }

    public boolean hasNextInstruction() {
        return currentInstructionIndex < instructions.size();
    }

    public String getNextInstruction() {
        if (hasNextInstruction()) {
            return instructions.get(currentInstructionIndex++);
        }
        return null;
    }

    public void executeInstruction(String instruction) {
        System.out.println("进程 " + pid + " 执行指令: " + instruction);
        
        // 解析指令类型
        String[] parts = instruction.split("\\s+");
        String command = parts[0].toLowerCase();
        
        try {
            switch (command) {
                case "compute":
                    // 模拟计算操作
                    int computeTime = parts.length > 1 ? Integer.parseInt(parts[1]) : 1000;
                    System.out.println("进程 " + pid + " 执行计算操作，耗时 " + computeTime + "ms");
                    Thread.sleep(computeTime);
                    break;
                    
                case "io":
                    // 模拟IO操作，返回设备ID
                    int deviceId = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    System.out.println("进程 " + pid + " 请求IO设备 " + deviceId);
                    // IO操作由CPU处理，这里只返回设备ID
                    throw new IORequestException(deviceId);
                    
                case "sleep":
                    // 模拟进程休眠
                    int sleepTime = parts.length > 1 ? Integer.parseInt(parts[1]) : 1000;
                    System.out.println("进程 " + pid + " 休眠 " + sleepTime + "ms");
                    Thread.sleep(sleepTime);
                    break;
                    
                default:
                    // 未知指令
                    System.out.println("进程 " + pid + " 执行未知指令: " + instruction);
                    Thread.sleep(500); // 默认执行时间
            }
        } catch (InterruptedException e) {
            System.out.println("进程 " + pid + " 执行被中断");
            Thread.currentThread().interrupt();
        } catch (IORequestException e) {
            // 重新抛出IO请求异常，由CPU处理
            throw e;
        }
    }

    @Override
    public void run() {
        System.out.println("进程 " + pid + " 开始执行文件: " + executablePath);
        
        try {
            while (hasNextInstruction()) {
                String instruction = getNextInstruction();
                executeInstruction(instruction);
            }
            
            System.out.println("进程 " + pid + " 执行完成所有指令");
        } catch (IORequestException e) {
            // 这个异常会被CPU捕获并处理
            throw e;
        } catch (Exception e) {
            System.out.println("进程 " + pid + " 执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 自定义异常，用于IO请求
    public static class IORequestException extends RuntimeException {
        private int deviceId;
        
        public IORequestException(int deviceId) {
            this.deviceId = deviceId;
        }
        
        public int getDeviceId() {
            return deviceId;
        }
    }
    
    // 重置指令索引，用于进程被中断后重新执行
    public void resetInstructionIndex() {
        currentInstructionIndex = 0;
    }
    
    // 获取当前指令索引
    public int getCurrentInstructionIndex() {
        return currentInstructionIndex;
    }
    
    // 获取指令总数
    public int getInstructionCount() {
        return instructions.size();
    }
    
    public String getExecutablePath() {
        return executablePath;
    }
}
