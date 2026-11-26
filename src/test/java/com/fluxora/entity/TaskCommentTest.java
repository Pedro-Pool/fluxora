package com.fluxora.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskCommentTest {

    private static final String CONTENT = "This is a task comment.";

    private TaskComment createComment() {
        Task task = new Task("Study Java", "Practice tests");
        User user = new User("Test User", "test@fluxora.com", "hashedPassword");

        return new TaskComment(task, user, CONTENT);
    }

    @Test
    @DisplayName("Should create comment successfully")
    void test_01() {
        TaskComment comment = createComment();

        assertNotNull(comment);
        assertEquals(CONTENT, comment.getContent());
    }

    @Test
    @DisplayName("Should reject null content")
    void test_02() {
        Task task = new Task("Study Java", "Practice tests");
        User user = new User("Test User", "test@fluxora.com", "hashedPassword");

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(task, user, null));
    }

    @Test
    @DisplayName("Should reject empty content")
    void test_03() {
        Task task = new Task("Study Java", "Practice tests");
        User user = new User("Test User", "test@fluxora.com", "hashedPassword");

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(task, user, ""));
    }

    @Test
    @DisplayName("Should reject blank content")
    void test_04() {
        Task task = new Task("Study Java", "Practice tests");
        User user = new User("Test User", "test@fluxora.com", "hashedPassword");

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(task, user, "   "));
    }

    @Test
    @DisplayName("Should normalize content")
    void test_05() {
        TaskComment comment = new TaskComment(
                new Task("Study Java", "Practice tests"),
                new User("Test User", "test@fluxora.com", "hashedPassword"),
                "  Review the implementation.  "
        );

        assertEquals("Review the implementation.", comment.getContent());
    }

    @Test
    @DisplayName("Should reject content exceeding 5000 characters")
    void test_06() {
        String content = "a".repeat(5001);

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(
                        new Task("Study Java", "Practice tests"),
                        new User("Test User", "test@fluxora.com", "hashedPassword"),
                        content
                ));
    }

    @Test
    @DisplayName("Should reject null task")
    void test_07() {
        User user = new User("Test User", "test@fluxora.com", "hashedPassword");

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(null, user, CONTENT));
    }

    @Test
    @DisplayName("Should reject null author")
    void test_08() {
        Task task = new Task("Study Java", "Practice tests");

        assertThrows(IllegalArgumentException.class,
                () -> new TaskComment(task, null, CONTENT));
    }

    @Test
    @DisplayName("Should update comment content")
    void test_09() {
        TaskComment comment = createComment();

        comment.updateContent("Updated comment.");

        assertEquals("Updated comment.", comment.getContent());
    }

}
