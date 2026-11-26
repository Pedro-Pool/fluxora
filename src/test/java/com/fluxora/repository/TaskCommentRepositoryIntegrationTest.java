package com.fluxora.repository;

import com.fluxora.config.AbstractIntegrationTest;
import com.fluxora.entity.*;
import com.fluxora.enums.ProjectRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskCommentRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User member;
    private Project project;
    private Task task;
    private Task anotherTask;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        owner = new User(
                "Owner",
                "owner@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );

        member = new User(
                "Member",
                "member@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );

        entityManager.persist(owner);
        entityManager.persist(member);

        project = new Project("Owner Project", "Test project");
        project.assignTo(owner);

        entityManager.persist(project);

        entityManager.persist(
                new ProjectMember(project, owner, ProjectRole.OWNER)
        );

        entityManager.persist(
                new ProjectMember(project, member, ProjectRole.MEMBER)
        );

        task = createTask("First task");
        anotherTask = createTask("Another task");

        pageable = PageRequest.of(0, 10);

        entityManager.flush();
    }

    private Task createTask(String title) {
        Task newTask = new Task(title, "Integration test task");
        newTask.assignTo(project);
        newTask.setCreatedBy(owner);

        entityManager.persist(newTask);
        entityManager.flush();

        return newTask;
    }

    private TaskComment createComment(Task commentTask, User author, String content) {
        TaskComment comment = new TaskComment(commentTask, author, content);

        entityManager.persist(comment);
        entityManager.flush();

        return comment;
    }

    @Test
    @DisplayName("Should return all comments belonging to a task")
    void test_01() {
        TaskComment comment1 = createComment(task, owner, "First comment");
        TaskComment comment2 = createComment(task, member, "Second comment");

        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .extracting(TaskComment::getId)
                .containsExactlyInAnyOrder(comment1.getId(), comment2.getId());
    }

    @Test
    @DisplayName("Should return empty page when task has no comments")
    void test_02() {
        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should not return comments belonging to another task")
    void test_03() {
        TaskComment comment = createComment(
                anotherTask, owner, "Comment from another task"
        );

        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .doesNotContain(comment);
    }

    @Test
    @DisplayName("Should return only comments belonging to specified task")
    void test_04() {
        TaskComment comment1 = createComment(task, owner, "First comment");
        TaskComment comment2 = createComment(anotherTask, owner, "Other task comment");

        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .extracting(TaskComment::getId)
                .containsExactly(comment1.getId());

        assertThat(result.getContent())
                .doesNotContain(comment2);
    }

    @Test
    @DisplayName("Should paginate task comments")
    void test_05() {
        for (int i = 1; i <= 5; i++) {
            createComment(task, owner, "Comment " + i);
        }

        Pageable firstPage = PageRequest.of(0, 2);

        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), firstPage
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isZero();
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should return comments written by different authors")
    void test_06() {
        TaskComment ownerComment = createComment(task, owner, "Owner comment");
        TaskComment memberComment = createComment(task, member, "Member comment");

        Page<TaskComment> result = taskCommentRepository.findByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .extracting(comment -> comment.getUser().getId())
                .contains(owner.getId(), member.getId());

        assertThat(result.getContent())
                .extracting(TaskComment::getId)
                .containsExactlyInAnyOrder(
                        ownerComment.getId(),
                        memberComment.getId()
                );
    }
}
