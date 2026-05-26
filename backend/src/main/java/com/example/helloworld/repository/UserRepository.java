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

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username")
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, username FROM users ORDER BY id",
                userRowMapper
        );
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username FROM users WHERE id = ?",
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
                "SELECT id, username FROM users WHERE username = ?",
                userRowMapper,
                username
        );
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public User save(String username) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, username);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на потребител.");
        }

        return findById(key.longValue())
                .orElseThrow(() -> new IllegalStateException("Потребителят е създаден, но не може да бъде намерен."));
    }

    public Optional<User> update(Long id, String username) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET username = ? WHERE id = ?",
                username,
                id
        );
        if (rows == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }
}
