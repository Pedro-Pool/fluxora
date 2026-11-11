package com.fluxora.service;

import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectPermissionValidatorImpl implements ProjectPermissionValidator{

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectPermissionValidatorImpl(
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProjectMember validateMembership(Long projectId, String requesterEmail){
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        return projectMemberRepository.findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"
                ));
    }
    @Override
    public void validateOwner(ProjectMember membership){
        if (membership.getRole() != ProjectRole.OWNER){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the project owner can perform this action"
            );
        }
    }
    @Override
    public void validateCanAddMember(ProjectMember requesterMembership, ProjectRole role){
        ProjectRole requesterRole = requesterMembership.getRole();

        if (requesterRole != ProjectRole.OWNER && requesterRole != ProjectRole.ADMIN){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to manage project members"
            );
        }

        if (role == null || role == ProjectRole.OWNER){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid member role"
            );
        }

        if (requesterRole == ProjectRole.ADMIN && role != ProjectRole.MEMBER){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrators can only add members with MEMBER role"
            );
        }
    }
}
