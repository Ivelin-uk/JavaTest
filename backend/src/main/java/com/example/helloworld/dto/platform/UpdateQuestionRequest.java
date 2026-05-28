package com.example.helloworld.dto.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateQuestionRequest(
        @NotBlank(message = "Текстът на въпроса е задължителен.")
        String questionText,

        @NotNull(message = "Точките са задължителни.")
        @DecimalMin(value = "0.1", message = "Точките трябва да са над 0.")
        @DecimalMax(value = "100.0", message = "Точките могат да са максимум 100.")
        Double points,

        @NotNull(message = "Времето за отговор е задължително.")
        @Min(value = 5, message = "Времето за отговор трябва да е поне 5 секунди.")
        @Max(value = 3600, message = "Времето за отговор може да е максимум 3600 секунди.")
        Integer timeLimitSeconds,

        @NotEmpty(message = "Трябва да има поне 2 отговора.")
        @Valid
        List<QuestionOptionInput> options
) {
}
