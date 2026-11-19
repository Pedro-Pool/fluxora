package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    private final ProjectMemberRepository projectMemberRepository;

    private final ProjectPermissionValidator permissionValidator;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectPermissionValidator permissionValidator) {

        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.permissionValidator = permissionValidator;
    }
    public Page<Project> findAll(String email, Pageable pageable){
        User user = findUserByEmail(email);
        return projectRepository.findDistinctByMembers_User_Id(user.getId(), pageable);
    }
    public Project findById(Long id, String email){
        User user = findUserByEmail(email);
        return findProjectByIdAndUser(id, user.getId());
    }
    @Transactional
    public Project create(String name, String description, String email){
        User user = findUserByEmail(email);

        Project project = new Project(name, description);
        project.assignTo(user);

        Project savedProject = projectRepository.save(project);

        ProjectMember owner = new ProjectMember(
                savedProject,
                user,
                ProjectRole.OWNER
        );
        savedProject.addMember(owner);

        projectMemberRepository.save(owner);

        return savedProject;
    }
    @Transactional
    public Project update(Long id, String name, String description, String email){
        User user = findUserByEmail(email);
        Project project = findProjectByIdAndUser(id, user.getId());

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(id, user.getId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found"
                        ));

        permissionValidator.validateCanUpdateProject(membership);

        project.updateDetails(name, description);

        return project;
    }
    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Project project = findProjectByIdAndUser(id, user.getId());

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"
                ));

        permissionValidator.validateCanDeleteProject(membership);

        projectMemberRepository.deleteAll(
                projectMemberRepository.findByProjectId(id)
        );

        projectRepository.delete(project);
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.UNAUTHORIZED, "Authenticate user not found"));
    }
    private Project findProjectByIdAndUser(Long id, Long userId){
        return projectRepository.findByIdAndMembers_User_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}
