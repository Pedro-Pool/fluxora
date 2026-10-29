package com.fluxora.service;

import com.fluxora.entity.*;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Project project;
    private Pageable pageable;

    private final String email = "test@fluxora.com";
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;


    @BeforeEach
    void setUp(){
        user = new User("User test", email, "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        pageable = PageRequest.of(0, 10);

        project = new Project("Project test", "project description");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.assignTo(user);
    }
    private void mockAuthenticatedUser(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }
    private void mockOwnerProject(){
        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.of(project));
    }


    @Test
    @DisplayName("It must create a task using the provided data")
    void test_01(){
        mockAuthenticatedUser();
        mockOwnerProject();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                email);

        assertEquals("Study java", createdTask.getTitle());
        assertFalse(createdTask.isCompleted());
        assertSame(project, createdTask.getProject());

        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);
        verify(taskRepository).save(any(Task.class));
    }
    @Test
    @DisplayName("Should find all tasks for user")
    void test_02(){
        mockAuthenticatedUser();
        mockOwnerProject();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findAllWithFilters(
                PROJECT_ID,
                user.getId(),
                null,
                null,
                null,
                null,
                pageable
                ))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                email,
                null,
                null,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Study java", tasks.getContent().get(0).getTitle());

        verify(taskRepository).findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                null,
                null,
                pageable);
    }
    @Test
    @DisplayName("should find a task by ID for the user")
    void test_03(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        Task foundTask = taskService.findById(1L, email);

        assertSame(task, foundTask);

        verify(taskRepository).findByIdAndProjectUserId(1L, USER_ID);
    }
    @Test
    @DisplayName("It should throw an exception when the task does not exist for the user.")
    void test_04(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, email));
    }
    @Test
    @DisplayName("must complete the task")
    void test_05(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L, email);

        assertTrue(completedTask.isCompleted());
    }
    @Test
    @DisplayName("should delete the task")
    void test_06(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        taskService.delete(1L, email);

        verify(taskRepository).delete(task);
    }
    @Test
    @DisplayName("Should not allow a user to access another user's task")
    void test_07(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, email));

        verify(taskRepository).findByIdAndProjectUserId(99L, USER_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }
    @Test
    @DisplayName("Should not allow updating another user's task")
    void test_08(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() ->
                taskService.update(
                        99L,
                        "New Title",
                        "New Description",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        null,
                        email));

        verify(taskRepository).findByIdAndProjectUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow completing another user's task")
    void test_09(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.complete(99L, email));

        verify(taskRepository).findByIdAndProjectUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow reopening another user's task")
    void test_10(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.reopen(99L, email));

        verify(taskRepository).findByIdAndProjectUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow deleting another user's task")
    void test_11(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.delete(99L, email));

        verify(taskRepository).findByIdAndProjectUserId(99L, USER_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }
    @Test
    @DisplayName("Should create a task with default planning values")
    void test_12(){
        mockAuthenticatedUser();
        mockOwnerProject();

        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                null,
                null,
                null,
                email);

        assertEquals(TaskStatus.TODO, createdTask.getStatus());
        assertEquals(TaskPriority.MEDIUM, createdTask.getPriority());
        assertNull(createdTask.getDueDate());
        assertNull(createdTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should create a completed task with completedAt")
    void test_13(){
        mockAuthenticatedUser();
        mockOwnerProject();

        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 30),
                email);

        assertEquals(TaskStatus.DONE, createdTask.getStatus());
        assertEquals(TaskPriority.HIGH, createdTask.getPriority());
        assertEquals(LocalDate.of(2026, 10, 30), createdTask.getDueDate());
        assertNotNull(createdTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should not find task when the project does not belong to the user")
    void test_14(){
        mockAuthenticatedUser();

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,() -> taskService.findAll(
                PROJECT_ID,
                email,
                null,
                null,
                null,
                null,
                pageable));

        verify(taskRepository, never()).findByIdAndProjectUserId(PROJECT_ID, USER_ID);
    }
    @Test
    @DisplayName("Should throws unauthorized when the authenticated user does not exist")
    void test_15(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,() -> taskService.findById(1L, email));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should reopen a task and clear completedAt")
    void test_16(){
        Project taskProject = project;

        Task existingTask = new Task("Completed task", "description");
        existingTask.assignTo(taskProject);
        existingTask.completed();

        assertNotNull(existingTask.getCompletedAt());

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task taskReopen = taskService.reopen(1L, email);

        assertEquals(TaskStatus.TODO, taskReopen.getStatus());
        assertNull(taskReopen.getCompletedAt());
    }
    @Test
    @DisplayName("Should cancel a task and clear completedAt")
    void test_17(){
        Task existingTask = new Task("Completed task", "description");
        existingTask.assignTo(project);
        existingTask.completed();

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task canceledTask = taskService.cancel(1L, email);

        assertEquals(TaskStatus.CANCELED, canceledTask.getStatus());
        assertNull(canceledTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should update a task to DONE and set completedAt")
    void test_18(){
        Task existingTask = new Task("Old title", "Old description");
        existingTask.assignTo(project);

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task updatedTask = taskService.update(
                1L,
                "New title",
                "New description",
                TaskStatus.DONE,
                TaskPriority.URGENT,
                LocalDate.of(2026, 10, 30),
                email
        );
        assertEquals("New title", updatedTask.getTitle());
        assertEquals("New description", updatedTask.getDescription());
        assertEquals(TaskStatus.DONE, updatedTask.getStatus());
        assertEquals(TaskPriority.URGENT, updatedTask.getPriority());
        assertEquals(LocalDate.of(2026, 10, 30), updatedTask.getDueDate());
        assertNotNull(updatedTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should filter tasks by status")
    void test_19(){
        mockAuthenticatedUser();
        mockOwnerProject();

        Task task = new Task("Task in progress", null);

        when(taskRepository.findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                email,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Task in progress", tasks.getContent().get(0).getTitle());

        verify(taskRepository).findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should filter tasks by priority")
    void test_20(){
        mockAuthenticatedUser();
        mockOwnerProject();

        Task task = new Task("High Task", null);

        when(taskRepository.findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                null,
                TaskPriority.HIGH,
                null,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                email,
                null,
                TaskPriority.HIGH,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("High Task", tasks.getContent().get(0).getTitle());

        verify(taskRepository).findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                null,
                TaskPriority.HIGH,
                null,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should filter tasks by due date range")
    void test_21(){
        mockAuthenticatedUser();
        mockOwnerProject();

        Task task = new Task("October Task", null);

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        when(taskRepository.findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                from,
                to,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                email,
                null,
                null,
                from,
                to,
                pageable);

        assertEquals(1, tasks.getTotalElements());

        verify(taskRepository).findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                from,
                to,
                pageable
        );
    }
    @Test
    @DisplayName("Should combine status, priority and due date filters")
    void test_22(){
        mockAuthenticatedUser();
        mockOwnerProject();

        Task task = new Task("Urgent Task", null);

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        when(taskRepository.findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                email,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                pageable);

        assertEquals(1, tasks.getTotalElements());

        verify(taskRepository).findAllWithFilters(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                pageable
        );
    }
    @Test
    @DisplayName("Should reject an invalid due date range")
    void test_23(){
        mockAuthenticatedUser();
        mockOwnerProject();

        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> taskService.findAll(
                        PROJECT_ID,
                        email,
                        null,
                        null,
                        from,
                        to,
                        pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(taskRepository, never()).findAllWithFilters(any(), any(), any(), any(), any(), any(), any());
    }

}
