package com.fluxora.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    @Test
    @DisplayName("Should create a task with a valid title")
    void test_01(){
        Task task = new Task(
                "Study Java",
                "Practice unit testing"
        );
        assertEquals("Study Java", task.getTitle());
        assertEquals("Practice unit testing", task.getDescription());
        assertFalse(task.isCompleted());
        assertNull(task.getId());
    }
    @Test
    @DisplayName("Should reject blank title")
    void test_02(){
        assertThrows(IllegalArgumentException.class, () -> new Task("   ", "description"));
    }
    @Test
    @DisplayName("Should complete task")
    void test_03(){
        Task task = new Task("Study java", null);

        task.completed();

        assertTrue(task.isCompleted());
    }
    @Test
    @DisplayName("Should reopen task")
    void test_04(){
        Task task = new Task("Study java", null);
        task.completed();

        task.reopen();

        assertFalse(task.isCompleted());
    }
    @Test
    @DisplayName("Completing task must set status and completed")
    void test_05(){
        Task task = new Task("Study java", "Practice test");

        task.completed();

        assertEquals(TaskStatus.DONE, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }
    @Test
    @DisplayName("Reopening the task must be completedAt")
    void test_06(){
        Task task = new Task("Study java", "Practice test");
        task.completed();

        task.reopen();

        assertEquals(TaskStatus.TODO, task.getStatus());
        assertNull(task.getCompletedAt());
    }
    @Test
    @DisplayName("By cancelling the task, it must be completedAt")
    void test_07(){
        Task task = new Task("Study java", "Practice test");
        task.completed();

        task.cancel();

        assertEquals(TaskStatus.CANCELED, task.getStatus());
        assertNull(task.getCompletedAt());
    }
    @Test
    @DisplayName("Updating the plan must preserve the details of the tasks.")
    void test_08(){
        Task task = new Task("Study java", "Practice test");
        LocalDateTime originalCompletedAt = null;

        task.updatePlanning(
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 30));

        assertEquals("Study java", task.getTitle());
        assertEquals("Practice test", task.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(LocalDate.of(2026, 10, 30), task.getDueDate());
        assertEquals(originalCompletedAt, task.getCompletedAt());
    }

}
