package com.fluxora.service;

import com.fluxora.entity.*;
import com.fluxora.enums.ProjectRole;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskCommentRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private TaskCommentService taskCommentService;

    private User user;
    private Task task;
    private User anotherUser;
    private Project project;
    private TaskComment comment;

    private static final String EMAIL = "member@example.com";
    private static final String CONTENT = "This is a task comment.";
    private static final String UPDATED_CONTENT = "Updated task comment.";

    @BeforeEach
    void setUp() {
        user = new User("Member", EMAIL, "hashedPassword");

        anotherUser = new User(
                "Another User",
                "another@example.com",
                "hashedPassword"
        );

        project = new Project("Fluxora Project", "Project description");

        task = new Task("Study Java", "Practice tests");

        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(anotherUser, "id", 2L);
        ReflectionTestUtils.setField(project, "id", 3L);
        ReflectionTestUtils.setField(task, "id", 1L);

        task.assignTo(project);
    }

    private TaskComment createComment(
            User author,
            LocalDateTime createdAt
    ) {
        TaskComment comment = new TaskComment(task, author, CONTENT);

        ReflectionTestUtils.setField(comment, "createdAt", createdAt);

        return comment;
    }

    @Test
    @DisplayName("Should create comment successfully")
    void test_01() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.save(any(TaskComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskComment result = taskCommentService.create(
                1L, CONTENT, EMAIL
        );

        assertNotNull(result);
        assertEquals(CONTENT, result.getContent());
        assertSame(task, result.getTask());
        assertSame(user, result.getUser());

        verify(taskCommentRepository).save(any(TaskComment.class));
    }

    @Test
    @DisplayName("Should throw 401 when authenticated user does not exist")
    void test_02() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.create(1L, CONTENT, EMAIL)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(taskRepository, taskCommentRepository);
    }

    @Test
    @DisplayName("Should throw 404 when task does not exist")
    void test_03() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.create(1L, CONTENT, EMAIL)
        );

        verifyNoInteractions(taskCommentRepository);
    }

    @Test
    @DisplayName("Should throw 404 when user is not a project member")
    void test_04() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.create(1L, CONTENT, EMAIL)
        );

        verifyNoInteractions(taskCommentRepository);
    }

    @Test
    @DisplayName("Should reject invalid comment content")
    void test_05() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        assertThrows(
                IllegalArgumentException.class,
                () -> taskCommentService.create(1L, "   ", EMAIL)
        );

        verifyNoInteractions(taskCommentRepository);
    }
    @Test
    @DisplayName("Should return task comments successfully")
    void test_06() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        Pageable pageable = PageRequest.of(0, 10);

        TaskComment comment = new TaskComment(task, user, CONTENT);

        Page<TaskComment> expectedPage = new PageImpl<>(
                List.of(comment),
                pageable,
                1
        );

        when(taskCommentRepository.findByTaskId(task.getId(), pageable))
                .thenReturn(expectedPage);

        Page<TaskComment> result = taskCommentService.findAll(
                1L, EMAIL, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(comment, result.getContent().getFirst());

        verify(taskCommentRepository).findByTaskId(task.getId(), pageable);
    }

    @Test
    @DisplayName("Should return empty page when task has no comments")
    void test_07() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        Pageable pageable = PageRequest.of(0, 10);

        when(taskCommentRepository.findByTaskId(task.getId(), pageable))
                .thenReturn(Page.empty(pageable));

        Page<TaskComment> result = taskCommentService.findAll(
                1L, EMAIL, pageable
        );

        assertTrue(result.isEmpty());
        verify(taskCommentRepository).findByTaskId(task.getId(), pageable);
    }

    @Test
    @DisplayName("Should throw 404 when task does not exist")
    void test_08() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.findAll(1L, EMAIL, pageable)
        );

        verifyNoInteractions(taskCommentRepository);
    }

    @Test
    @DisplayName("Should throw 404 when user is not a project member")
    void test_09() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.findAll(1L, EMAIL, pageable)
        );

        verifyNoInteractions(taskCommentRepository);
    }
    @Test
    @DisplayName("Should update own comment within 15 minutes")
    void test_10() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(10);

        comment = createComment(user, createdAt);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(taskCommentRepository.save(any(TaskComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskComment result = taskCommentService.update(
                1L, 1L, UPDATED_CONTENT, EMAIL
        );

        assertEquals(UPDATED_CONTENT, result.getContent());
        assertSame(user, result.getUser());

        verify(taskCommentRepository).save(comment);
    }

    @Test
    @DisplayName("Should reject updating own comment after 15 minutes")
    void test_11() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(16);

        comment = createComment(user, createdAt);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.update(
                        1L, 1L, UPDATED_CONTENT, EMAIL
                )
        );

        assertEquals(CONTENT, comment.getContent());

        verify(taskCommentRepository, never()).save(any());
    }
    @Test
    @DisplayName("Should allow OWNER to update another user's comment")
    void test_12() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember owner = new ProjectMember(
                project, user, ProjectRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.of(owner));

        when(taskCommentRepository.save(any(TaskComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskComment result = taskCommentService.update(
                1L, 1L, UPDATED_CONTENT, EMAIL
        );

        assertEquals(UPDATED_CONTENT, result.getContent());
        assertSame(anotherUser, result.getUser());

        verify(taskCommentRepository).save(comment);
    }

    @Test
    @DisplayName("Should allow ADMIN to update another user's comment")
    void test_13() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember admin = new ProjectMember(
                project, user, ProjectRole.ADMIN
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.of(admin));

        when(taskCommentRepository.save(any(TaskComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskComment result = taskCommentService.update(
                1L, 1L, UPDATED_CONTENT, EMAIL
        );

        assertEquals(UPDATED_CONTENT, result.getContent());

        verify(taskCommentRepository).save(comment);
    }

    @Test
    @DisplayName("Should reject MEMBER updating another user's comment")
    void test_14() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember member = new ProjectMember(
                project, user, ProjectRole.MEMBER
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L)).thenReturn(Optional.of(member));

        assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.update(
                        1L, 1L, UPDATED_CONTENT, EMAIL
                )
        );

        assertEquals(CONTENT, comment.getContent());

        verify(taskCommentRepository, never()).save(any());
    }
    @Test
    @DisplayName("Should throw 404 when user is not a project member")
    void test_15() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.update(
                        1L, 1L, UPDATED_CONTENT, EMAIL
                )
        );

        verifyNoInteractions(taskCommentRepository);
        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should throw 404 when comment does not exist")
    void test_16() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.update(
                        1L, 1L, UPDATED_CONTENT, EMAIL
                )
        );

        verify(taskCommentRepository, never()).save(any());
    }
    @Test
    @DisplayName("Should reject invalid comment content")
    void test_17() {
        comment = createComment(
                user,
                LocalDateTime.now().minusMinutes(5)
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

            when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        assertThrows(
                IllegalArgumentException.class,
                () -> taskCommentService.update(
                        1L, 1L, "   ", EMAIL
                )
        );

        assertEquals(CONTENT, comment.getContent());

        verify(taskCommentRepository, never()).save(any());
    }
    @Test
    @DisplayName("Should allow author to delete own comment at any time")
    void test_18() {
        comment = createComment(
                user,
                LocalDateTime.now().minusDays(2)
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        assertDoesNotThrow(() ->
                taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        verify(taskCommentRepository).delete(comment);
    }
    @Test
    @DisplayName("Should allow OWNER to delete another user's comment")
    void test_19() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember owner = new ProjectMember(
                project, user, ProjectRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(owner));

        assertDoesNotThrow(() ->
                taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        verify(taskCommentRepository).delete(comment);
    }
    @Test
    @DisplayName("Should allow ADMIN to delete another user's comment")
    void test_20() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember admin = new ProjectMember(
                project, user, ProjectRole.ADMIN
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        verify(taskCommentRepository).delete(comment);
    }
    @Test
    @DisplayName("Should reject MEMBER deleting another user's comment")
    void test_21() {
        comment = createComment(
                anotherUser,
                LocalDateTime.now().minusDays(2)
        );

        ProjectMember member = new ProjectMember(
                project, user, ProjectRole.MEMBER
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        when(projectMemberRepository.findByProjectIdAndUserId(3L, 1L))
                .thenReturn(Optional.of(member));

        assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        assertEquals(CONTENT, comment.getContent());

        verify(taskCommentRepository, never()).delete(any());
    }
    @Test
    @DisplayName("Should reject deleting comment when user is not a project member")
    void test_22() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        verify(taskCommentRepository, never()).findByIdAndTaskId(anyLong(), anyLong());
        verify(taskCommentRepository, never()).delete(any());
        verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }
    @Test
    @DisplayName("Should throw 404 when comment does not exist during deletion")
    void test_23() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskCommentRepository, never()).delete(any());
    }
    @Test
    @DisplayName("Should allow OWNER to delete own comment")
    void test_24() {
        comment = createComment(
                user,
                LocalDateTime.now().minusDays(2)
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndProjectMemberUserId(1L, 1L))
                .thenReturn(Optional.of(task));

        when(taskCommentRepository.findByIdAndTaskId(1L, 1L))
                .thenReturn(Optional.of(comment));

        assertDoesNotThrow(() ->
                taskCommentService.delete(
                        1L, 1L, EMAIL
                )
        );

        verify(taskCommentRepository).delete(comment);
        verify(projectMemberRepository, never())
                .findByProjectIdAndUserId(anyLong(), anyLong());
    }
}
