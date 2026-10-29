package com.fluxora.controller;

import com.fluxora.dto.TaskRequest;
import com.fluxora.dto.TaskResponse;
import com.fluxora.entity.Task;
import com.fluxora.entity.TaskPriority;
import com.fluxora.entity.TaskStatus;
import com.fluxora.service.CategoryService;
import com.fluxora.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService taskService;
    private final CategoryService categoryService;

    public TaskController(TaskService taskService, CategoryService categoryService) {
        this.taskService = taskService;
        this.categoryService = categoryService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication authentication){
        Task task = taskService.create(
                projectId,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate(),
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<Page<TaskResponse>> findAll(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ){
        if(page < 0 || size < 1){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be greater than zero");
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<TaskResponse> tasks = taskService.findAll(
                projectId,
                authentication.getName(),
                status,
                priority,
                dueDateFrom,
                dueDateTo,
                pageable
                )
                .map(TaskResponse::from);
        return ResponseEntity.ok(tasks);
    }
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id, Authentication authentication){
        Task task = taskService.findById(id, authentication.getName());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication authentication){
        Task task = taskService.update(
                id,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate(),
                authentication.getName());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PatchMapping("/tasks/{id}/complete")
    public ResponseEntity<TaskResponse> complete(@PathVariable Long id, Authentication authentication){
        Task task = taskService.complete(id, authentication.getName());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PatchMapping("/tasks/{id}/reopen")
    public ResponseEntity<TaskResponse> reopen(@PathVariable Long id, Authentication authentication){
        Task task = taskService.reopen(id, authentication.getName());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PatchMapping("/tasks/{id}/cancel")
    public ResponseEntity<TaskResponse> cancel(@PathVariable Long id, Authentication authentication){
        Task task = taskService.cancel(id, authentication.getName());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication){
        taskService.delete(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
    @PutMapping("/tasks/{tasksId}/categories/{categoryId}")
    public ResponseEntity<Void> addCategory(
            @PathVariable Long tasksId,
            @PathVariable Long categoryId,
            Authentication authentication){
        categoryService.addToTask(tasksId, categoryId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/tasks/{tasksId}/categories/{categoryId}")
    public ResponseEntity<Void> removeCategory(
            @PathVariable Long tasksId,
            @PathVariable Long categoryId,
            Authentication authentication){
        categoryService.removeFromTask(tasksId, categoryId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
