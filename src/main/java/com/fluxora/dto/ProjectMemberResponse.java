package com.fluxora.dto;

import com.fluxora.entity.ProjectMember;
import com.fluxora.enums.ProjectRole;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        ProjectRole role,
        LocalDateTime joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member){
        return new ProjectMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
