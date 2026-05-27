package com.example.helloworld.service;

import com.example.helloworld.dto.AuthResponse;
import com.example.helloworld.dto.LoginRequest;
import com.example.helloworld.dto.RegisterRequest;
import com.example.helloworld.model.User;

public interface AuthService {

    User register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User getCurrentUser(String login);
}
