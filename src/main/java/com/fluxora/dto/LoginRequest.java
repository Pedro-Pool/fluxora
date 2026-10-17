package com.fluxora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "The email address is mandatory")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "The password is required")
        String password
) {
}
