package com.example.helloworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleAccessRequest(
        @NotBlank(message = "Кодът на ролята е задължителен.")
        String roleCode,

        @NotBlank(message = "Кодът на обекта за достъп е задължителен.")
        String accessObjectCode,

        @NotNull(message = "Полето canView е задължително.")
        Boolean canView
) {
}
