package com.example.helloworld.service.impl;

import com.example.helloworld.exception.DuplicateUserException;
import com.example.helloworld.exception.UserNotFoundException;
import com.example.helloworld.model.User;
import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.service.UserService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
    public User createUser(String username, String name, String email, String plainPassword, String role) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedName = normalizeName(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(role);
        String normalizedPassword = normalizePassword(plainPassword);

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new DuplicateUserException("Потребителското име вече съществува.");
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateUserException("Имейлът вече съществува.");
        }

        String passwordHash = passwordEncoder.encode(normalizedPassword);
        return userRepository.save(normalizedUsername, normalizedName, normalizedEmail, passwordHash, normalizedRole);
    }

    @Override
    public User updateUserRole(Long id, String role) {
        User existing = getUserById(id);
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(existing.getRole())
                && existing.isActive()
                && !"ADMIN".equals(normalizedRole)
                && userRepository.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("Трябва да има поне един активен администратор.");
        }

        return userRepository.updateRole(id, normalizedRole)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    @Override
    public User updateUserPassword(Long id, String plainPassword) {
        getUserById(id);
        String normalizedPassword = normalizePassword(plainPassword);
        String passwordHash = passwordEncoder.encode(normalizedPassword);
        return userRepository.updatePasswordHash(id, passwordHash)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    @Override
    public User updateUserActivation(Long id, boolean active) {
        User existing = getUserById(id);
        if ("ADMIN".equals(existing.getRole())
                && existing.isActive()
                && !active
                && userRepository.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("Не можеш да деактивираш последния активен администратор.");
        }

        return userRepository.updateActiveStatus(id, active)
                .orElseThrow(() -> new UserNotFoundException("Потребител с id " + id + " не беше намерен."));
    }

    @Override
    public void deleteUser(Long id) {
        User existing = getUserById(id);
        if ("ADMIN".equals(existing.getRole())
                && existing.isActive()
                && userRepository.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("Не можеш да изтриеш последния активен администратор.");
        }
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

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Името е задължително.");
        }

        String normalized = name.trim();
        if (normalized.length() < 2 || normalized.length() > 80) {
            throw new IllegalArgumentException("Името трябва да е между 2 и 80 символа.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Имейлът е задължителен.");
        }

        String normalized = email.trim().toLowerCase();
        if (!normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Невалиден имейл.");
        }
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Имейлът е твърде дълъг.");
        }
        return normalized;
    }

    private String normalizePassword(String plainPassword) {
        if (!StringUtils.hasText(plainPassword)) {
            throw new IllegalArgumentException("Паролата е задължителна.");
        }

        String normalized = plainPassword.trim();
        if (normalized.length() < 6 || normalized.length() > 120) {
            throw new IllegalArgumentException("Паролата трябва да е между 6 и 120 символа.");
        }
        return normalized;
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
