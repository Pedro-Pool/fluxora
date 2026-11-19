package com.fluxora.controller;

import com.fluxora.dto.ProjectRequest;
import com.fluxora.dto.ProjectResponse;
import com.fluxora.entity.Project;
import com.fluxora.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<ProjectResponse>> findAll(Authentication authentication, Pageable pageable){
        Page<ProjectResponse> projects = projectService
                .findAll(authentication.getName(), pageable)
                .map(ProjectResponse::from);

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
