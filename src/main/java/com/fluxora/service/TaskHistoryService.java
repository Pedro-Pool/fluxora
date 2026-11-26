package com.fluxora.service;

import com.fluxora.entity.Task;
import com.fluxora.entity.TaskHistory;
import com.fluxora.entity.User;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskHistoryRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public TaskHistoryService(
            TaskHistoryRepository taskHistoryRepository,
            TaskRepository taskRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository) {

        this.taskHistoryRepository = taskHistoryRepository;
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public Page<TaskHistory> findAll(Long taskId, String email, Pageable pageable){
        User user = finUserByEmail(email);

        Task task = taskRepository.findByIdAndProjectMemberUserId(
                taskId,
                user.getId()
        ).orElseThrow(() -> new TaskNotFoundException(taskId));

        return taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                task.getId(),
                task.getProject().getId(),
                pageable
        );
    }

    private User finUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"
                ));
    }
}
