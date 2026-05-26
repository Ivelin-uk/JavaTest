package com.example.helloworld.service.impl;

import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<String> getAllUserNames() {
        return userRepository.findAllUserNames();
    }
}
