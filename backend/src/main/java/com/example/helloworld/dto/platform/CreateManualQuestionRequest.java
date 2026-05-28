package com.example.helloworld.dto.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateManualQuestionRequest(
        @NotBlank(message = "Текстът на въпроса е задължителен.")
        String questionText,

        @NotNull(message = "Точките са задължителни.")
        @DecimalMin(value = "0.1", message = "Точките трябва да са над 0.")
        @DecimalMax(value = "100.0", message = "Точките могат да са максимум 100.")
        Double points,

        @NotEmpty(message = "Трябва да има поне 2 отговора.")
        @Valid
        List<QuestionOptionInput> options
) {
}
