package com.fluxora.dto;

import com.fluxora.entity.TaskHistory;
import com.fluxora.enums.TaskHistoryAction;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        Long id,
        Long taskId,
        Long userId,
        String userName,
        String userEmail,
        TaskHistoryAction action,
        String fieldName,
        String oldValue,
        String newValue,
        LocalDateTime createdAt
) {
    public static TaskHistoryResponse from(TaskHistory history){
        return new TaskHistoryResponse(
                history.getId(),
                history.getTask() != null ? history.getId() : null,
                history.getUser().getId(),
                history.getUser().getName(),
                history.getUser().getEmail(),
                history.getAction(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getCreatedAt()
        );
    }
}
