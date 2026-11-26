package com.fluxora.controller;

import com.fluxora.dto.TaskHistoryResponse;
import com.fluxora.service.TaskHistoryService;
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
@RequestMapping("/api/projects/{projectId}/history")
public class ProjectTaskHistoryController {

    private final TaskHistoryService taskHistoryService;

    public ProjectTaskHistoryController(TaskHistoryService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping
    public ResponseEntity<Page<TaskHistoryResponse>> findAll(
            @PathVariable Long projectId,
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
                "action",
                "fieldName"
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

        Page<TaskHistoryResponse> history = taskHistoryService.findAllByProject(
                projectId,
                authentication.getName(),
                pageable
        ).map(TaskHistoryResponse::from);

        return ResponseEntity.ok(history);
    }
}
