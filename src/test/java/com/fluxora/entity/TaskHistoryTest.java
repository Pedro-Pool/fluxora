package com.fluxora.entity;

import com.fluxora.enums.TaskHistoryAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskHistoryTest {

    @Test
    @DisplayName("Should create task history with project")
    void test_01() {
        Project project = new Project("Project test", "Description");
        User user = new User("User test", "test@fluxora.com", "password");
        Task task = new Task("Study Java", "Practice");

        TaskHistory history = new TaskHistory(
                project,
                task,
                user,
                TaskHistoryAction.CREATED,
                null,
                null,
                null
        );

        assertSame(project, history.getProject());
        assertSame(task, history.getTask());
        assertSame(user, history.getUser());
        assertEquals(TaskHistoryAction.CREATED, history.getAction());
    }

    @Test
    @DisplayName("Should reject null project")
    void test_02() {
        User user = new User("User test", "test@fluxora.com", "password");

        assertThrows(IllegalArgumentException.class, () ->
                new TaskHistory(
                        null,
                        null,
                        user,
                        TaskHistoryAction.DELETED,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    @DisplayName("Should allow null task for deleted action")
    void test_03() {
        Project project = new Project("Project test", "Description");
        User user = new User("User test", "test@fluxora.com", "password");

        TaskHistory history = new TaskHistory(
                project,
                null,
                user,
                TaskHistoryAction.DELETED,
                null,
                null,
                null
        );

        assertSame(project, history.getProject());
        assertNull(history.getTask());
        assertEquals(TaskHistoryAction.DELETED, history.getAction());
    }

    @Test
    @DisplayName("Should reject null task for non-deleted action")
    void test_04() {
        Project project = new Project("Project test", "Description");
        User user = new User("User test", "test@fluxora.com", "password");

        assertThrows(IllegalArgumentException.class, () ->
                new TaskHistory(
                        project,
                        null,
                        user,
                        TaskHistoryAction.CREATED,
                        null,
                        null,
                        null
                )
        );
    }
}