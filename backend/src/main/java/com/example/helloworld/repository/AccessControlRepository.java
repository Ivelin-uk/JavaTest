package com.example.helloworld.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccessControlRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccessControlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findRoles() {
        return jdbcTemplate.queryForList(
                """
                SELECT code,
                       name,
                       description,
                       COALESCE(is_active, 1) AS active
                FROM roles
                ORDER BY code
                """
        );
    }

    public List<Map<String, Object>> findActiveRoles() {
        return jdbcTemplate.queryForList(
                """
                SELECT code,
                       name,
                       description,
                       COALESCE(is_active, 1) AS active
                FROM roles
                WHERE COALESCE(is_active, 1) = 1
                ORDER BY code
                """
        );
    }

    public boolean roleExists(String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE code = ?",
                Integer.class,
                roleCode
        );
        return count != null && count > 0;
    }

    public boolean roleIsActive(String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE code = ? AND COALESCE(is_active, 1) = 1",
                Integer.class,
                roleCode
        );
        return count != null && count > 0;
    }

    public int updateRoleActivation(String roleCode, boolean active) {
        return jdbcTemplate.update(
                "UPDATE roles SET is_active = ? WHERE code = ?",
                active,
                roleCode
        );
    }

    public List<Map<String, Object>> findAccessObjects() {
        return jdbcTemplate.queryForList(
                """
                SELECT code,
                       name,
                       description
                FROM access_objects
                ORDER BY code
                """
        );
    }

    public boolean accessObjectExists(String accessObjectCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM access_objects WHERE code = ?",
                Integer.class,
                accessObjectCode
        );
        return count != null && count > 0;
    }

    public List<Map<String, Object>> findRoleAccess() {
        return jdbcTemplate.queryForList(
                """
                SELECT role_code AS roleCode,
                       access_object_code AS accessObjectCode,
                       COALESCE(can_view, 0) AS canView
                FROM role_access
                ORDER BY role_code, access_object_code
                """
        );
    }

    public void upsertRoleAccess(String roleCode, String accessObjectCode, boolean canView) {
        jdbcTemplate.update(
                """
                INSERT INTO role_access (role_code, access_object_code, can_view)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE can_view = VALUES(can_view)
                """,
                roleCode,
                accessObjectCode,
                canView
        );
    }

    public Optional<String> findRoleByLogin(String login) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT role
                FROM users
                WHERE username = ? OR email = ?
                LIMIT 1
                """,
                login,
                login
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object value = rows.get(0).get("role");
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value));
    }

    public boolean hasRoleAccess(String roleCode, String accessObjectCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_access
                WHERE role_code = ?
                  AND access_object_code = ?
                  AND COALESCE(can_view, 0) = 1
                """,
                Integer.class,
                roleCode,
                accessObjectCode
        );
        return count != null && count > 0;
    }
}
