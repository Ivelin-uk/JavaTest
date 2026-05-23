package com.example.helloworld.controller;

import com.example.helloworld.dto.GreetingResponse;
import com.example.helloworld.service.GreetingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final GreetingService greetingService;

    public PageController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        GreetingResponse greeting = greetingService.getHelloGreeting();
        model.addAttribute("message", greeting.message());
        return "index";
    }
}
