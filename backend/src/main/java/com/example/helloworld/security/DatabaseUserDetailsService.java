package com.example.helloworld.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.helloworld.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserRepository.UserAuthData authData = userRepository.findAuthByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Невалидни потребителски данни."));

        if (!StringUtils.hasText(authData.passwordHash())) {
            throw new UsernameNotFoundException("Потребителят няма зададена парола.");
        }

        String role = normalizeRole(authData.role());
        return User.builder()
                .username(authData.username())
                .password(authData.passwordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .disabled(!authData.active())
                .build();
    }

    private String normalizeRole(String role) {
        String normalized = StringUtils.hasText(role) ? role.trim().toUpperCase() : "USER";
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring(5);
        }
        return normalized;
    }
}
