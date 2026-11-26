package com.fluxora.service;

import com.fluxora.entity.ProjectMember;
import com.fluxora.enums.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.UserRepository;
import com.fluxora.validation.project.ProjectPermissionValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionValidator permissionValidator;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectPermissionValidator permissionValidator) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.permissionValidator = permissionValidator;
    }

    public Page<ProjectMember> findMembers(Long projectId, String email, Pageable pageable){
        permissionValidator.validateMembership(projectId, email);

        return projectMemberRepository.findByProjectId(projectId, pageable);
    }
    private User findInvitedUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invited user not found"
                ));
    }
    @Transactional
    public ProjectMember addMember(
            Long projectId,
            String memberEmail,
            ProjectRole role,
            String requesterEmail
    ){
        ProjectMember requesterMembership = permissionValidator.validateMembership(projectId, requesterEmail);

        permissionValidator.validateCanAddMember(requesterMembership, role);

        User invitedUser = findInvitedUserByEmail(memberEmail);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUser.getId())){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a member of this project"
            );
        }

        ProjectMember newMember = new ProjectMember(
                requesterMembership.getProject(),
                invitedUser,
                role
        );

        requesterMembership.getProject().addMember(newMember);

        return projectMemberRepository.save(newMember);
    }
    @Transactional
    public void removeMember(
            Long projectId,
            Long memberId,
            String requesterEmail
    ) {
        ProjectMember requesterMembership =
                permissionValidator.validateMembership(projectId, requesterEmail);

        permissionValidator.validateOwner(requesterMembership);

        ProjectMember targetMember =
                projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project member not found"
                        ));

        if (targetMember.getRole() == ProjectRole.OWNER){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The project owner cannot be removed"
            );
        }

        projectMemberRepository.delete(targetMember);
    }
    @Transactional
    public ProjectMember changeRole(
            Long projectId,
            Long memberId,
            ProjectRole role,
            String requesterEmail
    ){
         ProjectMember requesterMembership = permissionValidator.validateMembership(projectId, requesterEmail);

        permissionValidator.validateOwner(requesterMembership);

        if (role == null || role == ProjectRole.OWNER){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid member role"
            );
        }

        ProjectMember targetMember = projectMemberRepository
                .findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project member not found"
                ));

        if (targetMember.getRole() == ProjectRole.OWNER){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The project owner role cannot be changed"
            );
        }
        targetMember.changeRole(role);

        return targetMember;
    }
}
