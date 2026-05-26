package com.example.helloworld.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActivationRequest(
        @NotNull(message = "Стойността active е задължителна.")
        Boolean active
) {
}
