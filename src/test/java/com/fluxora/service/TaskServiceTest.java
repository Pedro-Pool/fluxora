package com.fluxora.service;

import com.fluxora.entity.Task;
import com.fluxora.entity.User;
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

    @InjectMocks
    private TaskService taskService;

    private User user;

    private final String email = "test@fluxora.com";

    @BeforeEach
    void setUp(){
        user = new User("User test", email, "password-hash");
        ReflectionTestUtils.setField(user, "id", 1L);
    }


    @Test
    @DisplayName("It must create a task using the provided data")
    void test_01(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create("Study java", "practice tests", email);

        assertEquals("Study java", createdTask.getTitle());
        assertFalse(createdTask.isCompleted());
        assertSame(user, createdTask.getUser());

        verify(userRepository).findByEmail(email);
        verify(taskRepository).save(any(Task.class));
    }
    @Test
    @DisplayName("Should find all tasks for user")
    void test_02(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findAllByUserId(user.getId())).thenReturn(List.of(new Task("Study java", null)));

        List<Task> task = taskService.findAll(email);

        assertEquals(1, task.size());
        verify(taskRepository).findAllByUserId(user.getId());
    }
    @Test
    @DisplayName("should find a task by ID for the user")
    void test_03(){
        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(task));

        Task foundTask = taskService.findById(1L, email);

        assertSame(task, foundTask);

        verify(taskRepository).findByIdAndUserId(1L, user.getId());
    }
    @Test
    @DisplayName("It should throw an exception when the task does not exist for the user.")
    void test_04(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, email));
    }
    @Test
    @DisplayName("must complete the task")
    void test_05(){
        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L, email);

        assertTrue(completedTask.isCompleted());
    }
    @Test
    @DisplayName("should delete the task")
    void test_06(){
        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(task));

        taskService.delete(1L, email);

        verify(taskRepository).delete(task);
    }
    @Test
    @DisplayName("Should not allow a user to access another user's task")
    void test_07(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, email));

        verify(taskRepository).findByIdAndUserId(99L, user.getId());
        verify(taskRepository, never()).delete(any(Task.class));
    }
    @Test
    @DisplayName("Should not allow updating another user's task")
    void test_08(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() ->
                taskService.update(99L, "New Title","New Description",  email));

        verify(taskRepository).findByIdAndUserId(99L, user.getId());
    }
    @Test
    @DisplayName("Should not allow completing another user's task")
    void test_09(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.complete(99L, email));

        verify(taskRepository).findByIdAndUserId(99L, user.getId());
    }
    @Test
    @DisplayName("Should not allow reopening another user's task")
    void test_10(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.reopen(99L, email));

        verify(taskRepository).findByIdAndUserId(99L, user.getId());
    }
    @Test
    @DisplayName("Should not allow deleting another user's task")
    void test_11(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.delete(99L, email));

        verify(taskRepository).findByIdAndUserId(99L, user.getId());
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
