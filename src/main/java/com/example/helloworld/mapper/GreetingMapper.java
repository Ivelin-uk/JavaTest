package com.example.helloworld.mapper;

import com.example.helloworld.dto.GreetingResponse;
import com.example.helloworld.model.Greeting;
import org.springframework.stereotype.Component;

@Component
public class GreetingMapper {

    public GreetingResponse toResponse(Greeting greeting) {
        return new GreetingResponse(greeting.getId(), greeting.getMessage());
    }
}
