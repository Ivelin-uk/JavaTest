package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Името на групата е задължително.")
        @Size(max = 120, message = "Името на групата може да е до 120 символа.")
        String name
) {
}
