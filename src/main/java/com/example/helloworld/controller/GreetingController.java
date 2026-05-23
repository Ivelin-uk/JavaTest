package com.example.helloworld.controller;

import com.example.helloworld.dto.GreetingResponse;
import com.example.helloworld.service.GreetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/hello")
    public GreetingResponse hello() {
        return greetingService.getHelloGreeting();
    }
}
