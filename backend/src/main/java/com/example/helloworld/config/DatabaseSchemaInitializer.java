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
        createUsersTable();
        ensureUserActiveColumn();
        createRolesAndAccessTables();
        createLearningTables();
        ensureQuestionTimeLimitColumn();
        ensureAttemptCurrentQuestionStartedAtColumn();
        createAttemptAnswerOptionsTable();
        seedNomenclatures();
        jdbcTemplate.update("UPDATE users SET is_active = 1 WHERE is_active IS NULL");
    }

    private void createUsersTable() {
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
    }

    private void ensureUserActiveColumn() {
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
    }

    private void createRolesAndAccessTables() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS roles (
                code VARCHAR(30) NOT NULL,
                name VARCHAR(80) NOT NULL,
                description VARCHAR(255) NULL,
                is_active TINYINT(1) NOT NULL DEFAULT 1,
                PRIMARY KEY (code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS access_objects (
                code VARCHAR(60) NOT NULL,
                name VARCHAR(120) NOT NULL,
                description VARCHAR(255) NULL,
                PRIMARY KEY (code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS role_access (
                role_code VARCHAR(30) NOT NULL,
                access_object_code VARCHAR(60) NOT NULL,
                can_view TINYINT(1) NOT NULL DEFAULT 1,
                PRIMARY KEY (role_code, access_object_code),
                CONSTRAINT fk_role_access_role
                    FOREIGN KEY (role_code) REFERENCES roles(code) ON DELETE CASCADE,
                CONSTRAINT fk_role_access_object
                    FOREIGN KEY (access_object_code) REFERENCES access_objects(code) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );
    }

    private void createLearningTables() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS subjects (
                id BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(120) NOT NULL,
                description VARCHAR(255) NULL,
                teacher_id BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                CONSTRAINT fk_subjects_teacher
                    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS tests (
                id BIGINT NOT NULL AUTO_INCREMENT,
                title VARCHAR(160) NOT NULL,
                description TEXT NULL,
                subject_id BIGINT NOT NULL,
                teacher_id BIGINT NOT NULL,
                time_limit_minutes INT NOT NULL DEFAULT 30,
                is_active TINYINT(1) NOT NULL DEFAULT 1,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                CONSTRAINT fk_tests_subject
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
                CONSTRAINT fk_tests_teacher
                    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS questions (
                id BIGINT NOT NULL AUTO_INCREMENT,
                test_id BIGINT NOT NULL,
                question_text TEXT NOT NULL,
                source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
                points DECIMAL(8,2) NOT NULL DEFAULT 1.00,
                time_limit_seconds INT NOT NULL DEFAULT 60,
                position_index INT NOT NULL,
                PRIMARY KEY (id),
                UNIQUE KEY uk_questions_test_position (test_id, position_index),
                CONSTRAINT fk_questions_test
                    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS question_options (
                id BIGINT NOT NULL AUTO_INCREMENT,
                question_id BIGINT NOT NULL,
                option_text VARCHAR(500) NOT NULL,
                is_correct TINYINT(1) NOT NULL DEFAULT 0,
                position_index INT NOT NULL,
                PRIMARY KEY (id),
                UNIQUE KEY uk_question_option_position (question_id, position_index),
                CONSTRAINT fk_question_options_question
                    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS student_groups (
                id BIGINT NOT NULL AUTO_INCREMENT,
                group_name VARCHAR(120) NOT NULL,
                teacher_id BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_teacher_group_name (teacher_id, group_name),
                CONSTRAINT fk_groups_teacher
                    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS group_members (
                group_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (group_id, student_id),
                CONSTRAINT fk_group_members_group
                    FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE CASCADE,
                CONSTRAINT fk_group_members_student
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS test_assignments (
                id BIGINT NOT NULL AUTO_INCREMENT,
                test_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                group_id BIGINT NULL,
                assigned_by BIGINT NOT NULL,
                due_at TIMESTAMP NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_test_assignments_student (student_id),
                CONSTRAINT fk_assignments_test
                    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
                CONSTRAINT fk_assignments_student
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                CONSTRAINT fk_assignments_group
                    FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE SET NULL,
                CONSTRAINT fk_assignments_assigned_by
                    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS test_attempts (
                id BIGINT NOT NULL AUTO_INCREMENT,
                assignment_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
                current_position INT NOT NULL DEFAULT 1,
                current_question_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                end_time TIMESTAMP NULL,
                earned_points DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                total_points DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                score_percent DECIMAL(6,2) NOT NULL DEFAULT 0.00,
                violations_count INT NOT NULL DEFAULT 0,
                PRIMARY KEY (id),
                UNIQUE KEY uk_attempt_assignment_student (assignment_id, student_id),
                CONSTRAINT fk_attempts_assignment
                    FOREIGN KEY (assignment_id) REFERENCES test_assignments(id) ON DELETE CASCADE,
                CONSTRAINT fk_attempts_student
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS attempt_answers (
                id BIGINT NOT NULL AUTO_INCREMENT,
                attempt_id BIGINT NOT NULL,
                question_id BIGINT NOT NULL,
                selected_option_id BIGINT NULL,
                is_correct TINYINT(1) NOT NULL DEFAULT 0,
                earned_points DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                violation_reason VARCHAR(120) NULL,
                answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_attempt_question (attempt_id, question_id),
                CONSTRAINT fk_attempt_answers_attempt
                    FOREIGN KEY (attempt_id) REFERENCES test_attempts(id) ON DELETE CASCADE,
                CONSTRAINT fk_attempt_answers_question
                    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
                CONSTRAINT fk_attempt_answers_option
                    FOREIGN KEY (selected_option_id) REFERENCES question_options(id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        createAttemptAnswerOptionsTable();
    }

    private void ensureQuestionTimeLimitColumn() {
        Integer columnExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'questions'
                  AND column_name = 'time_limit_seconds'
                """,
                Integer.class
        );

        if (columnExists == null || columnExists == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE questions ADD COLUMN time_limit_seconds INT NOT NULL DEFAULT 60 AFTER points"
            );
        }

        jdbcTemplate.update("UPDATE questions SET time_limit_seconds = 60 WHERE time_limit_seconds IS NULL OR time_limit_seconds < 5");
    }

    private void ensureAttemptCurrentQuestionStartedAtColumn() {
        Integer columnExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'test_attempts'
                  AND column_name = 'current_question_started_at'
                """,
                Integer.class
        );

        if (columnExists == null || columnExists == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE test_attempts ADD COLUMN current_question_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER current_position"
            );
        }

        jdbcTemplate.update("UPDATE test_attempts SET current_question_started_at = CURRENT_TIMESTAMP WHERE current_question_started_at IS NULL");
    }

    private void createAttemptAnswerOptionsTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS attempt_answer_options (
                    attempt_answer_id BIGINT NOT NULL,
                    option_id BIGINT NOT NULL,
                    PRIMARY KEY (attempt_answer_id, option_id),
                    CONSTRAINT fk_attempt_answer_options_answer
                        FOREIGN KEY (attempt_answer_id) REFERENCES attempt_answers(id) ON DELETE CASCADE,
                    CONSTRAINT fk_attempt_answer_options_option
                        FOREIGN KEY (option_id) REFERENCES question_options(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """
        );
    }

    private void seedNomenclatures() {
        jdbcTemplate.update("""
            INSERT IGNORE INTO roles (code, name, description, is_active)
            VALUES
              ('ADMIN', 'Администратор', 'Управлява системата и потребителите', 1),
              ('TEACHER', 'Преподавател', 'Създава тестове и задава тестове', 1),
              ('STUDENT', 'Ученик', 'Решава зададени тестове', 1)
            """);

        jdbcTemplate.update("""
            INSERT IGNORE INTO access_objects (code, name, description)
            VALUES
              ('ADMIN_PANEL', 'Админ панел', 'Управление на потребители и роли'),
              ('TEACHER_PANEL', 'Teacher панел', 'Създаване и задаване на тестове'),
              ('STUDENT_PANEL', 'Student панел', 'Решаване на тестове'),
              ('REPORTS', 'Справки', 'Статистики и резултати')
            """);

        jdbcTemplate.update("""
            INSERT IGNORE INTO role_access (role_code, access_object_code, can_view)
            VALUES
              ('ADMIN', 'ADMIN_PANEL', 1),
              ('ADMIN', 'TEACHER_PANEL', 1),
              ('ADMIN', 'STUDENT_PANEL', 1),
              ('ADMIN', 'REPORTS', 1),
              ('TEACHER', 'TEACHER_PANEL', 1),
              ('TEACHER', 'REPORTS', 1),
              ('STUDENT', 'STUDENT_PANEL', 1)
            """);
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
                DB_URL=jdbc:mysql://127.0.0.1:3306/testdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
                DB_USERNAME=root
                DB_PASSWORD= (или root, ако в MAMP е с парола)
                """;
    }
}
