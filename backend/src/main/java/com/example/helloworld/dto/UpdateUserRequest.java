package com.example.helloworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Потребителското име е задължително.")
        @Size(min = 2, max = 60, message = "Потребителското име трябва да е между 2 и 60 символа.")
        String username
) {
}
