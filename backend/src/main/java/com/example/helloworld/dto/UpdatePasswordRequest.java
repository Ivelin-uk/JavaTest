package com.example.helloworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "Паролата е задължителна.")
        @Size(min = 6, max = 120, message = "Паролата трябва да е между 6 и 120 символа.")
        String password
) {
}
