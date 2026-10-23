package com.fluxora.dto;

import com.fluxora.entity.Task;
import com.fluxora.entity.TaskPriority;
import com.fluxora.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    LocalDateTime createdAt,
    LocalDateTime updateAt,
    LocalDateTime completedAt
) {
    public static TaskResponse from(Task task){
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdateAt(),
                task.getCompletedAt()
        );
    }
}
