package com.fluxora.dto;

import com.fluxora.entity.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDateTime creatAt
) {
    public static ProjectResponse from(Project project){
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }
}
