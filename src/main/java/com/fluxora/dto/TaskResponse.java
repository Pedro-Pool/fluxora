package com.fluxora.dto;

import com.fluxora.entity.Task;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    String title,
    String description,
    boolean completed,
    LocalDateTime creatAt
) {
    public static TaskResponse from(Task task){
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt()
        );
    }
}
