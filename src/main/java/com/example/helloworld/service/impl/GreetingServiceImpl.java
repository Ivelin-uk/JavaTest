package com.example.helloworld.service.impl;

import com.example.helloworld.dto.GreetingResponse;
import com.example.helloworld.mapper.GreetingMapper;
import com.example.helloworld.repository.GreetingRepository;
import com.example.helloworld.service.GreetingService;
import org.springframework.stereotype.Service;

@Service
public class GreetingServiceImpl implements GreetingService {

    private final GreetingRepository greetingRepository;
    private final GreetingMapper greetingMapper;

    public GreetingServiceImpl(GreetingRepository greetingRepository, GreetingMapper greetingMapper) {
        this.greetingRepository = greetingRepository;
        this.greetingMapper = greetingMapper;
    }

    @Override
    public GreetingResponse getHelloGreeting() {
        return greetingRepository
                .findFirstByOrderByIdAsc()
                .map(greetingMapper::toResponse)
                .orElse(new GreetingResponse(null, "Hello World"));
    }
}
