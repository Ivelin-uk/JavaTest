package com.example.helloworld.service.impl;

import com.example.helloworld.exception.DuplicateUserException;
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
    public User createUser(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new DuplicateUserException("Потребител с име '" + normalizedUsername + "' вече съществува.");
        }
        return userRepository.save(normalizedUsername);
    }

    @Override
    public User updateUser(Long id, String username) {
        User existingUser = getUserById(id);
        String normalizedUsername = normalizeUsername(username);
        userRepository.findByUsername(normalizedUsername)
                .ifPresent(user -> {
                    if (!user.getId().equals(existingUser.getId())) {
                        throw new DuplicateUserException(
                                "Потребител с име '" + normalizedUsername + "' вече съществува."
                        );
                    }
                });

        return userRepository.update(id, normalizedUsername)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    @Override
    public void deleteUser(Long id) {
        getUserById(id);
        userRepository.deleteById(id);
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Потребителското име е задължително.");
        }

        String normalized = username.trim();
        if (normalized.length() < 2 || normalized.length() > 60) {
            throw new IllegalArgumentException("Потребителското име трябва да е между 2 и 60 символа.");
        }

        return normalized;
    }
}
