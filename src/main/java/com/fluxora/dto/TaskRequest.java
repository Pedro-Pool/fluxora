package com.fluxora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
    @NotBlank(message = "The title is mandatory.")
    @Size(max = 150, message = "The title must have a maximum of 150 characters.")
    String title,

    @Size(max = 100, message = "The description must be a maximum of 1,000 characters.")
    String description
){}
