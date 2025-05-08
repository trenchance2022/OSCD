package org.example.oscdspring.controller;

import org.example.oscdspring.file_disk_management.FileSystemImpl;
import org.example.oscdspring.main.Shell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shell")
public class ShellController {

    private final Shell shell;
    private final FileSystemImpl fileSystem;

    @Autowired
    public ShellController(Shell shell, FileSystemImpl fileSystem) {
        this.shell = shell;
        this.fileSystem = fileSystem;
    }


    /**
     * 接收前端提交的命令字符串，并返回命令执行结果。
     */
    @PostMapping("/command")
    public String executeCommand(@RequestBody String command) {
        return shell.processCommand(command);
    }

    /**
     * 保存 vi 模式下编辑的文件内容
     * 假设前端传来的数据格式为 JSON { "fileName": "...", "content": "..." }
     */
    @PostMapping("/vi")
    public String saveFileContent(@RequestBody ViSaveRequest request) {
        fileSystem.editFile(request.getFileName(), request.getContent());
        return "File " + request.getFileName() + " has been updated.";
    }
}

class ViSaveRequest {
    private String fileName;
    private String content;

    // getter 和 setter
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}