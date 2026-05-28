package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTestRequest(
        @NotBlank(message = "Заглавието на теста е задължително.")
        @Size(max = 160, message = "Заглавието може да е до 160 символа.")
        String title,

        String description,

        @NotNull(message = "Subject ID е задължителен.")
        Long subjectId,

        @Min(value = 1, message = "Времето трябва да е поне 1 минута.")
        @Max(value = 300, message = "Времето може да е максимум 300 минути.")
        Integer timeLimitMinutes,

        @NotNull(message = "active е задължително поле.")
        Boolean active
) {
}
