package com.fluxora.service;

import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    public List<ProjectMember> findMembers(Long projectId, String email){
        permissionValidator.validateMembership(projectId, email);

        return projectMemberRepository.findByProjectId(projectId);
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

        return projectMemberRepository.save(newMember);
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
