package com.fluxora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskCommentRequest(

        @NotBlank
        @Size(max = 5000)
        String content
) {
}
