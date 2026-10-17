package com.fluxora.service;

import com.fluxora.entity.Task;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("It must create a task using the provided data.")
    void test_01(){
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create("Study java", "practice tests");

        assertEquals("Study java", createdTask.getTitle());
        assertFalse(createdTask.isCompleted());

        verify(taskRepository).save(any(Task.class));
    }
    @Test
    @DisplayName("It should find a task by ID.")
    void test_02(){
        Task task = new Task("Study java", null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task foundTask = taskService.findById(1L);

        assertSame(task, foundTask);
        verify(taskRepository).findById(1L);
    }
    @Test
    @DisplayName("It must throw an exception when the task does not exist.")
    void test_03(){
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.findById(99L));

        verify(taskRepository).findById(99L);
    }
    @Test
    @DisplayName("Must complete an existing task")
    void test_04(){
        Task task = new Task("Study java", null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L);

        assertTrue(completedTask.isCompleted());
        verify(taskRepository).findById(1L);
    }
    @Test
    @DisplayName("It must delete an existing task.")
    void test_05(){
        Task task = new Task("Study java", null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L);

        verify(taskRepository).delete(task);
    }
}
