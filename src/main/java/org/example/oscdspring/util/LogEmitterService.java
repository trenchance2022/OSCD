package org.example.oscdspring.util;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LogEmitterService {
    // 使用线程安全的列表保存所有 SSE 连接
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // 静态实例，使得非 Spring 管理的类也能访问
    private static LogEmitterService instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static LogEmitterService getInstance() {
        return instance;
    }

    /**
     * 添加一个新的 SSE 连接，并注册自动移除的回调。
     */
    public SseEmitter addEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * 向所有 SSE 连接发送日志消息，自动追加换行符。
     */
    public void sendLog(String message) {
        // 自动在消息后追加换行符
        String messageWithNewline = message + "\n";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(messageWithNewline));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(emitter);
            }
        }
    }
}
