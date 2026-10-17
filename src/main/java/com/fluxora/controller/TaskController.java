package com.fluxora.controller;

import com.fluxora.dto.TaskRequest;
import com.fluxora.dto.TaskResponse;
import com.fluxora.entity.Task;
import com.fluxora.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request){
        Task task = taskService.create(request.title(), request.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }
    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll(){
        List<TaskResponse> tasks = taskService.findAll()
                .stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(tasks);
    }
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id){
        Task task = taskService.findById(id);

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskRequest request){
        Task task = taskService.update(id, request.title(), request.description());

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> complete(@PathVariable Long id){
        Task task = taskService.complete(id);

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TaskResponse> reopen(@PathVariable Long id){
        Task task = taskService.reopen(id);

        return ResponseEntity.ok(TaskResponse.from(task));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        taskService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
