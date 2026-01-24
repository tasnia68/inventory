package com.inventory.system.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @PostMapping("/echo")
    public String echo(@RequestBody String body) {
        log.info("Test echo received: {}", body);
        return "Echo: " + body;
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("Test hello called");
        return "Hello from Test Controller!";
    }
}
