package com.example.helloworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Потребителско име или имейл е задължително.")
        @Size(min = 2, max = 120, message = "Логинът трябва да е между 2 и 120 символа.")
        String login,
        @NotBlank(message = "Паролата е задължителна.")
        @Size(min = 6, max = 120, message = "Паролата трябва да е между 6 и 120 символа.")
        String password
) {
}
