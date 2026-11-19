package com.fluxora.dto;

import com.fluxora.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequest(

        @NotBlank(message = "Member email is mandatory")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Member role is mandatory")
        ProjectRole role
) {
}
