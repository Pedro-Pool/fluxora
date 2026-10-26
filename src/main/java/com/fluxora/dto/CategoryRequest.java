package com.fluxora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "The category name is mandatory")
        @Size(max = 50, message = "The category name must have a maximum of 50 characters")
        String name
) {
}
