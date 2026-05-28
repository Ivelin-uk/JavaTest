package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank(message = "Името на предмета е задължително.")
        @Size(max = 120, message = "Името на предмета може да е до 120 символа.")
        String name,

        @Size(max = 255, message = "Описанието може да е до 255 символа.")
        String description
) {
}
