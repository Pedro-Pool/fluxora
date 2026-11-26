package com.fluxora.service;

import com.fluxora.entity.*;
import com.fluxora.enums.TaskHistoryAction;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskHistoryRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceTest {

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskHistoryService taskHistoryService;

    private User user;
    private Project project;
    private Task task;
    private Pageable pageable;

    private final String email = "test@fluxora.com";

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long TASK_ID = 100L;

    @BeforeEach
    void setUp() {
        user = new User("User test", email, "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        project = new Project("Project test", "Project description");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.assignTo(user);

        task = new Task("Study Java", "Practice tests");
        ReflectionTestUtils.setField(task, "id", TASK_ID);
        task.assignTo(project);
        task.setCreatedBy(user);

        pageable = PageRequest.of(0, 10);
    }

    private void mockAuthenticatedUser() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
    }

    private void mockTaskAccessibleByUser() {
        when(taskRepository.findByIdAndProjectMemberUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(task));
    }

    @Test
    @DisplayName("It must return task history for a project member")
    void test_01() {
        mockAuthenticatedUser();
        mockTaskAccessibleByUser();

        TaskHistory history = new TaskHistory(
                task,
                user,
                TaskHistoryAction.CREATED,
                null,
                null,
                null
        );

        Page<TaskHistory> historyPage =
                new PageImpl<>(List.of(history), pageable, 1);

        when(taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                TASK_ID, PROJECT_ID, pageable
        )).thenReturn(historyPage);

        Page<TaskHistory> result = taskHistoryService.findAll(
                TASK_ID, email, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertSame(history, result.getContent().getFirst());

        verify(taskRepository)
                .findByIdAndProjectMemberUserId(TASK_ID, USER_ID);

        verify(taskHistoryRepository)
                .findAllByTaskIdAndTaskProjectId(
                        TASK_ID, PROJECT_ID, pageable
                );
    }

    @Test
    @DisplayName("It must return an empty page when task has no history")
    void test_02() {
        mockAuthenticatedUser();
        mockTaskAccessibleByUser();

        Page<TaskHistory> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

        when(taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                TASK_ID, PROJECT_ID, pageable
        )).thenReturn(emptyPage);

        Page<TaskHistory> result = taskHistoryService.findAll(
                TASK_ID, email, pageable
        );

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(taskHistoryRepository)
                .findAllByTaskIdAndTaskProjectId(
                        TASK_ID, PROJECT_ID, pageable
                );
    }

    @Test
    @DisplayName("It must throw TaskNotFoundException when task does not exist or is inaccessible")
    void test_03() {
        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskHistoryService.findAll(TASK_ID, email, pageable)
        );

        verify(taskHistoryRepository, never())
                .findAllByTaskIdAndTaskProjectId(
                        anyLong(), anyLong(), any(Pageable.class)
                );
    }

    @Test
    @DisplayName("It must not return history when user is not a project member")
    void test_04() {
        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskHistoryService.findAll(TASK_ID, email, pageable)
        );

        verify(taskHistoryRepository, never())
                .findAllByTaskIdAndTaskProjectId(
                        anyLong(), anyLong(), any(Pageable.class)
                );
    }

    @Test
    @DisplayName("It must throw 401 when authenticated user does not exist")
    void test_05() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskHistoryService.findAll(TASK_ID, email, pageable)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(taskRepository, never())
                .findByIdAndProjectMemberUserId(anyLong(), anyLong());

        verifyNoInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("It must respect the requested pagination")
    void test_06() {
        mockAuthenticatedUser();
        mockTaskAccessibleByUser();

        Pageable requestedPageable = PageRequest.of(1, 5);

        Page<TaskHistory> historyPage =
                new PageImpl<>(List.of(), requestedPageable, 8);

        when(taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                TASK_ID, PROJECT_ID, requestedPageable
        )).thenReturn(historyPage);

        Page<TaskHistory> result = taskHistoryService.findAll(
                TASK_ID, email, requestedPageable
        );

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(8, result.getTotalElements());
        assertEquals(2, result.getTotalPages());

        verify(taskHistoryRepository)
                .findAllByTaskIdAndTaskProjectId(
                        TASK_ID, PROJECT_ID, requestedPageable
                );
    }
}
