package com.example.helloworld.service.impl;

import com.example.helloworld.exception.UserNotFoundException;
import com.example.helloworld.model.User;
import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    @Override
    public User updateUserRole(Long id, String role) {
        getUserById(id);
        String normalizedRole = normalizeRole(role);
        return userRepository.updateRole(id, normalizedRole)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("Ролята е задължителна.");
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.matches("^[A-Z_]{2,30}$")) {
            throw new IllegalArgumentException("Ролята трябва да е 2-30 символа, само главни букви и _.");
        }
        return normalized;
    }
}
