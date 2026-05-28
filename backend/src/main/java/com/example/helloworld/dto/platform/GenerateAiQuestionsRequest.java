package com.example.helloworld.dto.platform;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateAiQuestionsRequest(
        @NotBlank(message = "Темата е задължителна.")
        @Size(max = 180, message = "Темата може да е до 180 символа.")
        String topic,

        @Size(max = 30, message = "Трудността може да е до 30 символа.")
        String difficulty,

        @Min(value = 1, message = "Броят въпроси трябва да е поне 1.")
        @Max(value = 20, message = "Броят въпроси може да е максимум 20.")
        Integer count,

        @Min(value = 5, message = "Времето за отговор трябва да е поне 5 секунди.")
        @Max(value = 3600, message = "Времето за отговор може да е максимум 3600 секунди.")
        Integer timeLimitSeconds
) {
}
