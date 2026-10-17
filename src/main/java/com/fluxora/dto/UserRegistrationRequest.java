package com.fluxora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank(message = "The name is mandatory.")
        @Size(max = 100, message = "The name must have a maximum of 100 characters.")
        String name,

        @NotBlank(message = "The email is mandatory")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "The email must have a maximum of 254 characters")
        String email,

        @NotBlank(message = "The password is mandatory")
        @Size(min = 8, max = 72, message = "The password must be between 8 and 72 characters long.")
        String password
) { }
