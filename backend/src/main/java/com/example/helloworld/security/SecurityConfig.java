package com.example.helloworld.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.example.helloworld.service.AccessControlService;

@Configuration
public class SecurityConfig {

    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    public SecurityConfig(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/users/**").access(accessByRoleAndObject("ADMIN_PANEL", "ADMIN"))
                        .requestMatchers("/api/teacher/reports/**").access(accessByRoleAndObject("REPORTS", "TEACHER", "ADMIN"))
                        .requestMatchers("/api/teacher/**").access(accessByRoleAndObject("TEACHER_PANEL", "TEACHER", "ADMIN"))
                        .requestMatchers("/api/student/**").access(accessByRoleAndObject("STUDENT_PANEL", "STUDENT"))
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new SecurityErrorResponse(
                                            "Неоторизиран достъп.",
                                            HttpStatus.UNAUTHORIZED.value(),
                                            Instant.now().toString()
                                    )
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new SecurityErrorResponse(
                                            "Нямаш права за тази операция.",
                                            HttpStatus.FORBIDDEN.value(),
                                            Instant.now().toString()
                                    )
                            ));
                        })
                );

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> accessByRoleAndObject(
            String accessObjectCode,
            String... allowedRoles
    ) {
        return (authenticationSupplier, context) -> {
            Authentication authentication = authenticationSupplier.get();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }

            boolean hasAllowedRole = Arrays.stream(allowedRoles)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .anyMatch(authority -> authentication.getAuthorities().contains(authority));

            if (!hasAllowedRole) {
                return new AuthorizationDecision(false);
            }

            boolean hasAccess = accessControlService.hasAccess(authentication.getName(), accessObjectCode);
            return new AuthorizationDecision(hasAccess);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> originPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private record SecurityErrorResponse(
            String message,
            int status,
            String timestamp
    ) {
    }
}
