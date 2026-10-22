package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }
    public List<Project> findAll(String email){
        User user = findUserByEmail(email);
        return projectRepository.findAllByUserId(user.getId());
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

        return projectRepository.save(project);
    }
    @Transactional
    public Project update(Long id, String name, String description, String email){
        User user = findUserByEmail(email);

        Project project = findProjectByIdAndUser(id, user.getId());

        project.updateDetails(name, description);

        return project;
    }
    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Project project = findProjectByIdAndUser(id, user.getId());

        projectRepository.delete(project);
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.UNAUTHORIZED, "Authenticate user not found"));
    }
    private Project findProjectByIdAndUser(Long id, Long userId){
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}
