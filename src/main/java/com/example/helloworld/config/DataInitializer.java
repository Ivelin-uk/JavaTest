package com.example.helloworld.config;

import com.example.helloworld.model.Greeting;
import com.example.helloworld.repository.GreetingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final GreetingRepository greetingRepository;

    public DataInitializer(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    @Override
    public void run(String... args) {
        if (greetingRepository.count() == 0) {
            greetingRepository.save(new Greeting("Hello World"));
        }
    }
}
