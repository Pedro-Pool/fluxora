package com.fluxora.controller;

import com.fluxora.dto.ProjectMemberRequest;
import com.fluxora.dto.ProjectMemberResponse;
import com.fluxora.entity.ProjectMember;
import com.fluxora.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService service;

    public ProjectMemberController(ProjectMemberService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberRequest request,
            Authentication authentication
            ) {
        ProjectMember member = service.addMember(
                projectId,
                request.email(),
                request.role(),
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMemberResponse.from(member));
    }
    @GetMapping
    public ResponseEntity<Page<ProjectMemberResponse>> findMembers(
            @PathVariable Long projectId,
            Authentication authentication,
            Pageable pageable
    ) {
        Page<ProjectMemberResponse> members = service
                .findMembers(projectId, authentication.getName(), pageable)
                .map(ProjectMemberResponse::from);

        return ResponseEntity.ok(members);
    }
    @PatchMapping("/{memberId}")
    public ResponseEntity<ProjectMemberResponse> changeRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody ProjectMemberRequest request,
            Authentication authentication
    ) {
        ProjectMember member = service.changeRole(
                projectId,
                memberId,
                request.role(),
                authentication.getName()
        );

        return ResponseEntity.ok(ProjectMemberResponse.from(member));
    }
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        service.removeMember(
                projectId,
                memberId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
