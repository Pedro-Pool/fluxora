package com.fluxora.dto;

import com.fluxora.entity.TaskComment;

import java.time.LocalDateTime;
import java.util.Objects;

public record TaskCommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean edited
) {
    public static TaskCommentResponse from(TaskComment comment){
        return new TaskCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                !Objects.equals(comment.getCreatedAt(), comment.getUpdatedAt())
        );
    }
}
