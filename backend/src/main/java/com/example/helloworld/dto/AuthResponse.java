package com.example.helloworld.dto;

import com.example.helloworld.model.User;

public record AuthResponse(
        String message,
        String token,
        User user
) {
}
