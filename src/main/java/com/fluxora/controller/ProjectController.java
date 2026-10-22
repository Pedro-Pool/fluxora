package com.fluxora.controller;

import com.fluxora.dto.ProjectRequest;
import com.fluxora.dto.ProjectResponse;
import com.fluxora.entity.Project;
import com.fluxora.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication){
        Project project = projectService.create(request.name(), request.description(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> findAll(Authentication authentication){
        List<ProjectResponse> projects = projectService
                .findAll(authentication.getName())
                .stream()
                .map(ProjectResponse::from)
                .toList();

        return ResponseEntity.ok(projects);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id, Authentication authentication){
        Project project = projectService.findById(id, authentication.getName());

        return ResponseEntity.ok(ProjectResponse.from(project));
    }
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication){

        Project project = projectService.update(id, request.name(), request.description(), authentication.getName());

        return ResponseEntity.ok(ProjectResponse.from(project));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication){
        projectService.delete(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
