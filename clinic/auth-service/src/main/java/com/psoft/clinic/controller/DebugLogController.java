package com.psoft.clinic.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class DebugLogController {

    private static final Logger log = LoggerFactory.getLogger(DebugLogController.class);

    @GetMapping("/debug/log")
    public ResponseEntity<String> logTest() {
        log.info("debug-log endpoint called");
        return ResponseEntity.ok("ok");
    }
}
