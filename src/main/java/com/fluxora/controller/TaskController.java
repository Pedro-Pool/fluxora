package com.fluxora.controller;

import com.fluxora.dto.TaskRequest;
import com.fluxora.dto.TaskResponse;
import com.fluxora.entity.Task;
import com.fluxora.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication authentication){
        Task task = taskService.create(projectId, request.title(), request.description(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> findAll(@PathVariable Long projectId, Authentication authentication){
        List<TaskResponse> tasks = taskService.findAll(projectId, authentication.getName())
                .stream()
                .map(TaskResponse::from)
                .toList();
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
        Task task = taskService.update(id, request.title(), request.description(), authentication.getName());

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
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication){
        taskService.delete(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
