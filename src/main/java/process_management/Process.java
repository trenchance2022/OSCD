package process_management;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import memory_management.MemoryManagement;
import memory_management.MemoryManagementImpl;

public class Process implements Runnable {
    private String executablePath;
    private List<String> instructions;
    private int currentInstructionIndex;
    private int pid;
    private MemoryManagement memoryManager;

    public Process(String executablePath) {
        this.executablePath = executablePath;
        this.instructions = new ArrayList<>();
        this.currentInstructionIndex = 0;
        this.memoryManager = new MemoryManagementImpl();
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
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
        String command = parts.length > 0 ? parts[0].toLowerCase() : "";

        switch (command) {
            case "":        
            default:
        }
    }

    @Override
    public void run() {
        System.out.println("进程 " + pid + " 开始执行文件: " + executablePath);
        
        while (hasNextInstruction()) {
            String instruction = getNextInstruction();
            executeInstruction(instruction);
        }
        
        System.out.println("进程 " + pid + " 执行完成所有指令");
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
