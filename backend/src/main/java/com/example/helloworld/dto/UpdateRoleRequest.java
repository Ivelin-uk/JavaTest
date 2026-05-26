package com.example.helloworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank(message = "Ролята е задължителна.")
        @Size(min = 2, max = 30, message = "Ролята трябва да е 2-30 символа.")
        @Pattern(regexp = "^[A-Za-z_]+$", message = "Ролята може да съдържа само букви и _.")
        String role
) {
}
