package com.example.helloworld.repository;

import com.example.helloworld.model.User;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String TABLE_NAME = "users";
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("role")
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id,
                       COALESCE(NULLIF(username, ''), NULLIF(name, ''), NULLIF(email, ''), CONCAT('user-', id)) AS username,
                       name,
                       email,
                       role
                FROM users
                ORDER BY id
                """,
                userRowMapper
        );
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT id,
                       COALESCE(NULLIF(username, ''), NULLIF(name, ''), NULLIF(email, ''), CONCAT('user-', id)) AS username,
                       name,
                       email,
                       role
                FROM users
                WHERE id = ?
                """,
                userRowMapper,
                id
        );
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT id,
                       COALESCE(NULLIF(username, ''), NULLIF(name, ''), NULLIF(email, ''), CONCAT('user-', id)) AS username,
                       name,
                       email,
                       role
                FROM users
                WHERE username = ?
                LIMIT 1
                """,
                userRowMapper,
                username
        );
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT id,
                       COALESCE(NULLIF(username, ''), NULLIF(name, ''), NULLIF(email, ''), CONCAT('user-', id)) AS username,
                       name,
                       email,
                       role
                FROM users
                WHERE email = ?
                LIMIT 1
                """,
                userRowMapper,
                email
        );
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public User save(String username, String name, String email, String passwordHash, String role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, role, username) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            ps.setString(5, username);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на потребител.");
        }

        return findById(key.longValue())
                .orElseThrow(() -> new IllegalStateException("Потребителят е създаден, но не може да бъде намерен."));
    }

    public Optional<User> updateRole(Long id, String role) {
        int rows = jdbcTemplate.update(
                "UPDATE " + TABLE_NAME + " SET role = ? WHERE id = ?",
                role,
                id
        );
        if (rows == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    public Optional<User> updatePasswordHash(Long id, String passwordHash) {
        int rows = jdbcTemplate.update(
                "UPDATE " + TABLE_NAME + " SET password_hash = ? WHERE id = ?",
                passwordHash,
                id
        );
        if (rows == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
    }
}
