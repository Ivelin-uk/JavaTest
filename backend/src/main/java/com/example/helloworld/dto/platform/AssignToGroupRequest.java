package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotNull;

public record AssignToGroupRequest(
        @NotNull(message = "testId е задължителен.")
        Long testId,

        @NotNull(message = "groupId е задължителен.")
        Long groupId,

        String dueAt
) {
}
