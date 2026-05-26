package com.example.helloworld.repository;

import com.example.helloworld.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String TABLE_NAME = "users";
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
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
}
