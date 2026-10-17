package com.fluxora.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
