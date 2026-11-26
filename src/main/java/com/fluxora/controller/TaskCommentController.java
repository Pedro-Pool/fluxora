package com.fluxora.controller;


import com.fluxora.dto.TaskCommentRequest;
import com.fluxora.dto.TaskCommentResponse;
import com.fluxora.entity.TaskComment;
import com.fluxora.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    public TaskCommentController(TaskCommentService taskCommentService) {
        this.taskCommentService = taskCommentService;
    }

    @GetMapping
    public ResponseEntity<Page<TaskCommentResponse>> findAll(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        if (page < 0 || size < 1){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be greater tha zero"
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort direction: " + direction
            );
        }

        Set<String> allowedSortFields = Set.of(
                "createdAt",
                "updatedAt",
                "content"
        );

        if (!allowedSortFields.contains(sortBy)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort field: " + sortBy
            );
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortDirection, sortBy)
        );

        Page<TaskCommentResponse> comments = taskCommentService.findAll(
                taskId,
                authentication.getName(),
                pageable
        ).map(TaskCommentResponse::from);

        return ResponseEntity.ok(comments);
    }
    @PostMapping
    public ResponseEntity<TaskCommentResponse> create(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCommentRequest request,
            Authentication authentication
            ) {
        TaskCommentResponse response = TaskCommentResponse.from(
                taskCommentService.create(
                        taskId,
                        request.content(),
                        authentication.getName()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PutMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> update(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody TaskCommentRequest request,
            Authentication authentication
    ) {
        TaskCommentResponse response = TaskCommentResponse.from(
                taskCommentService.update(
                        commentId,
                        taskId,
                        request.content(),
                        authentication.getName()
                )
        );

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        taskCommentService.delete(
                commentId,
                taskId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
