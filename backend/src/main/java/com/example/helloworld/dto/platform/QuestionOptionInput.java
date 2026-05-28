package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionOptionInput(
        @NotBlank(message = "Текстът на отговора е задължителен.")
        @Size(max = 500, message = "Отговорът може да е до 500 символа.")
        String text,

        @NotNull(message = "Маркерът за правилен отговор е задължителен.")
        Boolean correct
) {
}
