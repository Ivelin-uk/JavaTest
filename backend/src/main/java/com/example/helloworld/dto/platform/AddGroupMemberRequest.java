package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(
        @NotNull(message = "studentId е задължителен.")
        Long studentId
) {
}
