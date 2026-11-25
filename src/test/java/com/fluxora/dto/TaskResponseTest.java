package com.fluxora.dto;

import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskResponseTest {

    @Test
    @DisplayName("Should return assignee data when task has an assignee")
    void test_01() {
        User creator = new User("Creator", "creator@fluxora.com", "hashedPassword");
        User assignee = new User("Assignee", "assignee@fluxora.com", "hashedPassword");

        Task task = new Task("Task title", "Task description");
        task.setCreatedBy(creator);
        task.assignTo(assignee);

        TaskResponse response = TaskResponse.from(task);

        assertEquals(assignee.getId(), response.assigneeId());
        assertEquals(assignee.getName(), response.assigneeName());
        assertEquals(assignee.getEmail(), response.assigneeEmail());
    }

    @Test
    @DisplayName("Should return null assignee fields when task has no assignee")
    void test_02() {
        User creator = new User("Creator", "creator@fluxora.com", "hashedPassword");

        Task task = new Task("Task title", "Task description");
        task.setCreatedBy(creator);

        TaskResponse response = TaskResponse.from(task);

        assertNull(response.assigneeId());
        assertNull(response.assigneeName());
        assertNull(response.assigneeEmail());
    }

    @Test
    @DisplayName("Should return creator data")
    void test_03() {
        User creator = new User("Creator", "creator@fluxora.com", "hashedPassword");

        Task task = new Task("Task title", "Task description");
        task.setCreatedBy(creator);

        TaskResponse response = TaskResponse.from(task);

        assertEquals(creator.getId(), response.createdById());
        assertEquals(creator.getName(), response.createdByName());
        assertEquals(creator.getEmail(), response.createdByEmail());
    }
}
