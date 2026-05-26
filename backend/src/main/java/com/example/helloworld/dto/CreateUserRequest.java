package com.example.helloworld.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Потребителското име е задължително.")
        @Size(min = 2, max = 60, message = "Потребителското име трябва да е между 2 и 60 символа.")
        String username,
        @NotBlank(message = "Името е задължително.")
        @Size(min = 2, max = 80, message = "Името трябва да е между 2 и 80 символа.")
        String name,
        @NotBlank(message = "Имейлът е задължителен.")
        @Email(message = "Невалиден имейл.")
        @Size(max = 120, message = "Имейлът е твърде дълъг.")
        String email,
        @NotBlank(message = "Паролата е задължителна.")
        @Size(min = 6, max = 120, message = "Паролата трябва да е между 6 и 120 символа.")
        String password,
        @NotBlank(message = "Ролята е задължителна.")
        @Size(min = 2, max = 30, message = "Ролята трябва да е 2-30 символа.")
        @Pattern(regexp = "^[A-Za-z_]+$", message = "Ролята може да съдържа само букви и _.")
        String role
) {
}
