package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotNull;

public record AssignToStudentRequest(
        @NotNull(message = "testId е задължителен.")
        Long testId,

        @NotNull(message = "studentId е задължителен.")
        Long studentId,

        String dueAt
) {
}
