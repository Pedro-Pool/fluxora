package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ProjectRepository projectRepository){

        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }
    public List<Task> findAll(Long projectId, String email){
        User user = findUserByEmail(email);

        findProjectByIdAndUser(projectId, user.getId());

        return taskRepository.findAllByProjectIdAndProjectUserId(projectId, user.getId());
    }
    public Task findById(Long id, String email){
        User user = findUserByEmail(email);

        return findTaskByIdAndUser(id, user.getId());
    }
    @Transactional
    public Task create(Long projectId, String title, String description, String email){
        User user = findUserByEmail(email);
        Project project = findProjectByIdAndUser(projectId, user.getId());

        Task task = new Task(title, description);
        task.assignTo(project);

        return taskRepository.save(task);
    }
    @Transactional
    public Task update(Long id, String title, String description, String email){
        User user = findUserByEmail(email);

        Task task = findTaskByIdAndUser(id, user.getId());
        task.updateDetails(title, description);

        return task;
    }
    @Transactional
    public Task complete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        task.completed();
        return task;
    }
    @Transactional
    public Task reopen(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        task.reopen();
        return task;
    }
    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        taskRepository.delete(task);
    }
    private User findUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authenticated user not found"
        ));
    }
    private Project findProjectByIdAndUser(Long projectId, Long userId){
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));
    }
    private Task findTaskByIdAndUser(Long id, Long userId){
        return taskRepository.findByIdAndProjectUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

}
