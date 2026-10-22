package com.fluxora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "The project name is mandatory")
        @Size(max = 100, message = "The project name must have a maximum of 100 characters")
        String name,

        @Size(max = 1000, message = "The description must have a maximum of 1000 characters")
        String description
) {
}
