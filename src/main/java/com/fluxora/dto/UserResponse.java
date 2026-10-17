package com.fluxora.dto;

import com.fluxora.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDateTime creatAt
) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreateAt()
        );
    }
}
