package com.example.helloworld.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db.startup.max-retries:15}")
    private int maxRetries;

    @Value("${app.db.startup.retry-delay-ms:1000}")
    private long retryDelayMs;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int attempts = Math.max(1, maxRetries);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                ensureSchema();
                return;
            } catch (DataAccessException ex) {
                if (isAuthenticationError(ex)) {
                    throw new IllegalStateException(buildAuthenticationErrorMessage(), ex);
                }

                if (attempt == attempts) {
                    throw new IllegalStateException(
                            "Неуспешна връзка с MySQL. Провери дали MySQL работи и дали DB_URL/DB_USERNAME/DB_PASSWORD са правилни.",
                            ex
                    );
                }

                sleepBeforeRetry();
            }
        }
    }

    private void ensureSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(80) NOT NULL,
                    email VARCHAR(120) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(30) NOT NULL,
                    username VARCHAR(60) NOT NULL,
                    is_active TINYINT(1) NOT NULL DEFAULT 1,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_users_email (email),
                    UNIQUE KEY uk_users_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """
        );

        Integer columnExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND column_name = 'is_active'
                """,
                Integer.class
        );

        if (columnExists == null || columnExists == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1"
            );
        }

        jdbcTemplate.update("UPDATE users SET is_active = 1 WHERE is_active IS NULL");
    }

    private void sleepBeforeRetry() {
        if (retryDelayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isAuthenticationError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("access denied for user")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildAuthenticationErrorMessage() {
        return """
                Неуспешна връзка с MySQL: Access denied.
                Провери DB_USERNAME/DB_PASSWORD и използвай 127.0.0.1 вместо localhost.
                Пример за MAMP:
                DB_URL=jdbc:mysql://127.0.0.1:8889/testdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
                DB_USERNAME=root
                DB_PASSWORD=root (или празно, ако в MAMP е без парола)
                """;
    }
}
