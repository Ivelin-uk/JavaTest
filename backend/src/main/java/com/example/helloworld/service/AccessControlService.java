package com.example.helloworld.service;

import com.example.helloworld.repository.AccessControlRepository;
import com.example.helloworld.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccessControlService {

    private final AccessControlRepository accessControlRepository;
    private final UserRepository userRepository;

    public AccessControlService(AccessControlRepository accessControlRepository, UserRepository userRepository) {
        this.accessControlRepository = accessControlRepository;
        this.userRepository = userRepository;
    }

    public boolean hasAccess(String login, String accessObjectCode) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedAccessObjectCode = normalizeCode(accessObjectCode, "Кодът на достъп");

        String role = accessControlRepository.findRoleByLogin(normalizedLogin)
                .orElse(null);
        if (!StringUtils.hasText(role)) {
            return false;
        }

        String normalizedRole = normalizeCode(role, "Роля");
        if (!accessControlRepository.roleIsActive(normalizedRole)) {
            return false;
        }

        return accessControlRepository.hasRoleAccess(normalizedRole, normalizedAccessObjectCode);
    }

    public Map<String, Object> getAccessConfiguration() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roles", accessControlRepository.findRoles());
        payload.put("accessObjects", accessControlRepository.findAccessObjects());
        payload.put("roleAccess", accessControlRepository.findRoleAccess());
        return payload;
    }

    public List<Map<String, Object>> listActiveRoles() {
        return accessControlRepository.findActiveRoles();
    }

    public Map<String, Object> updateRoleAccess(String roleCode, String accessObjectCode, boolean canView) {
        String normalizedRoleCode = normalizeCode(roleCode, "Код на роля");
        String normalizedAccessObjectCode = normalizeCode(accessObjectCode, "Код на достъп");

        if (!accessControlRepository.roleExists(normalizedRoleCode)) {
            throw new IllegalArgumentException("Ролята не съществува.");
        }
        if (!accessControlRepository.accessObjectExists(normalizedAccessObjectCode)) {
            throw new IllegalArgumentException("Достъпният обект не съществува.");
        }

        accessControlRepository.upsertRoleAccess(normalizedRoleCode, normalizedAccessObjectCode, canView);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roleCode", normalizedRoleCode);
        response.put("accessObjectCode", normalizedAccessObjectCode);
        response.put("canView", canView);
        return response;
    }

    public Map<String, Object> updateRoleActivation(String roleCode, boolean active) {
        String normalizedRoleCode = normalizeCode(roleCode, "Код на роля");

        if (!accessControlRepository.roleExists(normalizedRoleCode)) {
            throw new IllegalArgumentException("Ролята не съществува.");
        }
        if ("ADMIN".equals(normalizedRoleCode) && !active) {
            throw new IllegalArgumentException("Ролята ADMIN не може да бъде деактивирана.");
        }
        if (!active && userRepository.countUsersByRole(normalizedRoleCode) > 0) {
            throw new IllegalArgumentException("Ролята има присвоени потребители и не може да бъде деактивирана.");
        }

        int updated = accessControlRepository.updateRoleActivation(normalizedRoleCode, active);
        if (updated == 0) {
            throw new IllegalArgumentException("Ролята не беше обновена.");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roleCode", normalizedRoleCode);
        response.put("active", active);
        return response;
    }

    public boolean isActiveRole(String roleCode) {
        String normalizedRoleCode = normalizeCode(roleCode, "Роля");
        return accessControlRepository.roleIsActive(normalizedRoleCode);
    }

    private String normalizeCode(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " е задължителен.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9_]{2,60}$")) {
            throw new IllegalArgumentException(fieldName + " е невалиден.");
        }
        return normalized;
    }

    private String normalizeLogin(String login) {
        if (!StringUtils.hasText(login)) {
            throw new IllegalArgumentException("Login е задължителен.");
        }
        return login.trim();
    }
}
