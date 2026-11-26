package com.fluxora.service;

import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.Task;
import com.fluxora.entity.TaskComment;
import com.fluxora.entity.User;
import com.fluxora.enums.ProjectRole;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskCommentRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;


@Service
@Transactional(readOnly = true)
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskCommentService(
            TaskCommentRepository taskCommentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository) {

        this.taskCommentRepository = taskCommentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public TaskComment create(Long taskId, String content, String email){
        User user = findUserByEmail(email);

        Task task = findTaskForMember(taskId, user.getId());

        TaskComment comment = new TaskComment(task, user, content);

        return taskCommentRepository.save(comment);
    }

    public Page<TaskComment> findAll(Long taskId, String email, Pageable pageable){
        User user = findUserByEmail(email);

        Task task = findTaskForMember(taskId, user.getId());

        return taskCommentRepository.findByTaskId(task.getId(), pageable);
    }
    @Transactional
    public TaskComment update(
            Long commentId,
            Long taskId,
            String content,
            String email
    ) {
        User user = findUserByEmail(email);

        Task task = findTaskForMember(taskId, user.getId());

        TaskComment comment = taskCommentRepository.findByIdAndTaskId(commentId, task.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comment not found"
                ));

        boolean isAuthor = isAuthor(comment, user);

        if (!isAuthor) {
            requireAdminOrOwner(task, user);
        } else {
            LocalDateTime expirationTime = comment.getCreatedAt().plusMinutes(15);

            if (LocalDateTime.now().isAfter(expirationTime)){
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "The 15-minute editing period has expired"
                );
            }
        }

        comment.updateContent(content);

        return taskCommentRepository.save(comment);
    }
    @Transactional
    public void delete(Long commentId, Long taskId, String email){
        User user = findUserByEmail(email);

        Task task = findTaskForMember(taskId, user.getId());

        TaskComment comment = taskCommentRepository.findByIdAndTaskId(
                commentId,
                task.getId()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isAuthor = isAuthor(comment, user);

        if (!isAuthor) {
            requireAdminOrOwner(task, user);
        }

        taskCommentRepository.delete(comment);
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication not found"
                ));
    }
    private Task findTaskForMember(Long taskId, Long userId){
        return taskRepository.findByIdAndProjectMemberUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
    private boolean isAuthor(TaskComment comment, User user){
        return comment.getUser().getId().equals(user.getId());
    }
    private void requireAdminOrOwner(Task task, User user){
        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(
                        task.getProject().getId(),
                        user.getId()
                ).orElseThrow(() -> new TaskNotFoundException(task.getId()));

        boolean canEditOthers = member.getRole() == ProjectRole.OWNER
                || member.getRole() == ProjectRole.ADMIN;

        if (!canEditOthers) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to manage another user's comment"
            );
        }
    }
}
