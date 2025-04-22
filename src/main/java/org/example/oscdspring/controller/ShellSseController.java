package org.example.oscdspring.controller;

import org.example.oscdspring.util.LogEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ShellSseController {

    private final LogEmitterService logEmitterService;

    @Autowired
    public ShellSseController(LogEmitterService logEmitterService) {
        this.logEmitterService = logEmitterService;
    }

    /**
     * 前端访问该接口后，建立 SSE 通道以接收后端实时日志
     */
    @GetMapping("/api/shell/stream")
    public SseEmitter stream() {
        return logEmitterService.addEmitter();
    }
}
