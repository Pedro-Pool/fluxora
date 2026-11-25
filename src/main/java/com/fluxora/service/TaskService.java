package com.fluxora.service;

import com.fluxora.dto.TaskStatisticsResponse;
import com.fluxora.entity.*;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository){

        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }
    public Page<Task> findAll(
            Long projectId,
            String email,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Long categoryId,
            Pageable pageable){
        User user = findUserByEmail(email);

        projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"
                ));

        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The start date must be before or equals to the end date");
        }

        return taskRepository.findAllWithFiltersByProjectMember(
                projectId,
                user.getId(),
                status,
                priority,
                dueDateFrom,
                dueDateTo,
                categoryId,
                pageable);
    }
    public Task findById(Long id, String email){
        User user = findUserByEmail(email);

        return findTaskByIdAndMember(id, user.getId());
    }
    public Page<Task> findOverdueTasks(String email, Pageable pageable){
        User user = findUserByEmail(email);

        return taskRepository.findOverdueTasksByProjectMember(
                user.getId(),
                LocalDate.now(),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    public Page<Task> findTasksDueDate(String email, Pageable pageable){
        User user = findUserByEmail(email);

        return taskRepository.findTasksDueTodayByProjectMember(
                user.getId(),
                LocalDate.now(),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    public Page<Task> findUpComingTask(String email, int days, Pageable pageable){
        if(days <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Days must be greater than zero");
        }

        User user = findUserByEmail(email);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        return taskRepository.findUpcomingTasksByProjectMember(
                user.getId(),
                today,
                endDate,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    public TaskStatisticsResponse findTaskStatistics(String email){
        User user = findUserByEmail(email);

        return taskRepository.findTaskStatistics(user.getId());
    }

    @Transactional
    public Task create(Long projectId,
                       String title,
                       String description,
                       TaskStatus status,
                       TaskPriority priority,
                       LocalDate dueDate,
                       Long assigneeId,
                       String email){

        User user = findUserByEmail(email);
        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        Project project = membership.getProject();

        User assignee = findAssigneeForProject(assigneeId, projectId);

        Task task = new Task(title, description);
        task.assignTo(project);
        task.setCreatedBy(user);
        task.assignTo(assignee);
        task.updatePlanning(status, priority, dueDate);

        if (status == TaskStatus.DONE){
            task.completed();
        }else if (status == TaskStatus.CANCELED){
            task.cancel();
        }

        return taskRepository.save(task);
    }
    @Transactional
    public Task update(Long id,
                       String title,
                       String description,
                       TaskStatus status,
                       TaskPriority priority,
                       LocalDate dueDate,
                       Long assigneeId,
                       String email){

        User user = findUserByEmail(email);
        Task task = findTaskByIdAndMember(id, user.getId());
        User assignee = findAssigneeForProject(assigneeId, task.getProject().getId());

        task.assignTo(assignee);
        task.updateDetails(title, description);
        task.updatePlanning(status, priority, dueDate);

        if (status == TaskStatus.DONE){
            task.completed();
        }else if (status == TaskStatus.CANCELED){
            task.cancel();
        } else if (status == TaskStatus.TODO || status == TaskStatus.IN_PROGRESS){
            task.reopen();
            if (status == TaskStatus.IN_PROGRESS){
                task.updatePlanning(status, priority, dueDate);
            }
        }
        return task;
    }
    @Transactional
    public Task complete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndMember(id, user.getId());
        task.completed();
        return task;
    }
    @Transactional
    public Task reopen(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndMember(id, user.getId());
        task.reopen();
        return task;
    }
    @Transactional
    public Task cancel(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndMember(id, user.getId());
        task.cancel();
        return task;
    }

    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndMember(id, user.getId());
        taskRepository.delete(task);
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authenticated user not found"
        ));
    }
    private Task findTaskByIdAndMember(Long id, Long userId){
        return taskRepository.findByIdAndProjectMemberUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
    private User findAssigneeForProject(Long assigneeId, Long projectId) {
        if (assigneeId == null){
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assignee not found"
                ));

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignee must be a member of the project"
            );
        }

        return assignee;
    }

}
