package com.fluxora.service;

import com.fluxora.dto.TaskStatisticsResponse;
import com.fluxora.entity.*;
import com.fluxora.enums.TaskHistoryAction;
import com.fluxora.enums.TaskPriority;
import com.fluxora.enums.TaskStatus;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskHistoryRepository;
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
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       TaskHistoryRepository taskHistoryRepository,
                       ProjectMemberRepository projectMemberRepository){

        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.taskHistoryRepository = taskHistoryRepository;
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

        Task savedTask = taskRepository.save(task);

        TaskHistory history = new TaskHistory(
                savedTask,
                user,
                TaskHistoryAction.CREATED,
                null,
                null,
                null
        );

        taskHistoryRepository.save(history);

        return savedTask;
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

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();

        TaskPriority oldPriority = task.getPriority();
        LocalDate oldDueDate = task.getDueDate();

        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        TaskStatus oldStatus = task.getStatus();

        task.assignTo(assignee);
        task.updateDetails(title, description);

        if (!oldTitle.equals(task.getTitle())){
            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    TaskHistoryAction.UPDATED,
                    "title",
                    oldTitle,
                    task.getTitle()
            ));
        }

        if (!Objects.equals(oldDescription, task.getDescription())){
            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    TaskHistoryAction.UPDATED,
                    "description",
                    oldDescription,
                    task.getDescription()
            ));
        }

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

        TaskStatus newStatus = task.getStatus();

        if (!Objects.equals(oldStatus, newStatus)){
            TaskHistoryAction action;

            if (newStatus == TaskStatus.DONE){
                action = TaskHistoryAction.COMPLETED;
            } else if (newStatus == TaskStatus.CANCELED){
                action = TaskHistoryAction.CANCELED;
            } else {
                action = TaskHistoryAction.REOPENED;
            }

            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    action,
                    "status",
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus != null ? newStatus.name() : null
            ));
        }

        if (!Objects.equals(oldPriority, task.getPriority())){
            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    TaskHistoryAction.UPDATED,
                    "priority",
                    oldPriority != null ? oldPriority.name() : null,
                    task.getPriority() != null ? task.getPriority().name() : null
            ));
        }
        if (!Objects.equals(oldDueDate, task.getDueDate())){
            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    TaskHistoryAction.UPDATED,
                    "dueDate",
                    oldDueDate != null ? oldDueDate.toString() : null,
                    task.getDueDate() != null ? task.getDueDate().toString() : null
            ));
        }

        Long newAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        if (!Objects.equals(oldAssigneeId, newAssigneeId)){
            taskHistoryRepository.save(new TaskHistory(
                    task,
                    user,
                    TaskHistoryAction.ASSIGNED,
                    "assignee",
                    oldAssigneeId != null ? oldAssigneeId.toString() : null,
                    newAssigneeId != null ? newAssigneeId.toString() : null
            ));
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
