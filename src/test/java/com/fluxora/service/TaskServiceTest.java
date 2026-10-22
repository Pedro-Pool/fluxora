package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    private final String email = "test@fluxora.com";
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;

    @BeforeEach
    void setUp(){
        user = new User("User test", email, "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);

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

        Task createdTask = taskService.create(PROJECT_ID, "Study java", "practice tests", email);

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

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findAllByProjectIdAndProjectUserId(PROJECT_ID, user.getId()))
                .thenReturn(List.of(new Task("Study java", null)));

        List<Task> task = taskService.findAll(PROJECT_ID, email);

        assertEquals(1, task.size());
        assertEquals("Study java", task.get(0).getTitle());

        verify(taskRepository).findAllByProjectIdAndProjectUserId(PROJECT_ID, USER_ID);
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
                taskService.update(99L, "New Title","New Description",  email));

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
}
