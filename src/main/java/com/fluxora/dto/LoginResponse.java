package com.fluxora.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
