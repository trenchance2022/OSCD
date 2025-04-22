package org.example.oscdspring.controller;

import org.example.oscdspring.util.SnapshotEmitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SnapshotController {

    private final SnapshotEmitterService snapshotEmitterService;

    @Autowired
    public SnapshotController(SnapshotEmitterService snapshotEmitterService) {
        this.snapshotEmitterService = snapshotEmitterService;
    }

    @GetMapping("/api/snapshot")
    public SseEmitter streamSnapshot() {
        return snapshotEmitterService.addEmitter();
    }
}
