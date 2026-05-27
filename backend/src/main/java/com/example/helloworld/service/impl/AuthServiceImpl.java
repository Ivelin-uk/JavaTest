package com.example.helloworld.service.impl;

import com.example.helloworld.dto.AuthResponse;
import com.example.helloworld.dto.LoginRequest;
import com.example.helloworld.dto.RegisterRequest;
import com.example.helloworld.exception.UserNotFoundException;
import com.example.helloworld.model.User;
import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.service.AuthService;
import com.example.helloworld.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        String role = normalizeRegistrationRole(request.role());
        return userService.createUser(
                request.username(),
                request.name(),
                request.email(),
                request.password(),
                role
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String login = normalizeLogin(request.login());
        String password = normalizePassword(request.password());

        UserRepository.UserAuthData authData = userRepository.findAuthByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Невалиден потребител или парола."));

        if (!authData.active()) {
            throw new IllegalArgumentException("Потребителят е деактивиран.");
        }

        if (!StringUtils.hasText(authData.passwordHash()) || !passwordEncoder.matches(password, authData.passwordHash())) {
            throw new IllegalArgumentException("Невалиден потребител или парола.");
        }

        User user = userRepository.findByUsername(authData.username())
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не може да бъде зареден."));

        String token = buildBasicToken(authData.username(), password);
        return new AuthResponse("Успешен вход.", token, user);
    }

    @Override
    public User getCurrentUser(String login) {
        String normalizedLogin = normalizeLogin(login);
        return userRepository.findByLogin(normalizedLogin)
                .orElseThrow(() -> new UserNotFoundException("Потребителят не е намерен."));
    }

    private String buildBasicToken(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String normalizeLogin(String login) {
        if (!StringUtils.hasText(login)) {
            throw new IllegalArgumentException("Потребителско име или имейл е задължително.");
        }
        return login.trim();
    }

    private String normalizePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Паролата е задължителна.");
        }

        String normalized = password.trim();
        if (normalized.length() < 6 || normalized.length() > 120) {
            throw new IllegalArgumentException("Паролата трябва да е между 6 и 120 символа.");
        }
        return normalized;
    }

    private String normalizeRegistrationRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("Ролята е задължителна.");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!"TEACHER".equals(normalized) && !"STUDENT".equals(normalized)) {
            throw new IllegalArgumentException("Ролята трябва да е TEACHER или STUDENT.");
        }
        return normalized;
    }
}
