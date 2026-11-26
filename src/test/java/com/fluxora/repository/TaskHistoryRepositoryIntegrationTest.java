package com.fluxora.repository;

import com.fluxora.config.AbstractIntegrationTest;
import com.fluxora.entity.*;
import com.fluxora.enums.ProjectRole;
import com.fluxora.enums.TaskHistoryAction;
import com.fluxora.enums.TaskPriority;
import com.fluxora.enums.TaskStatus;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskHistoryRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User member;
    private User anotherUser;

    private Project project;
    private Project anotherProject;

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

        anotherUser = new User(
                "Another User",
                "another@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );

        pageable = PageRequest.of(0, 10);

        entityManager.persist(owner);
        entityManager.persist(member);
        entityManager.persist(anotherUser);

        project = new Project("Owner Project", "Test project");
        project.assignTo(owner);

        anotherProject = new Project("Another Project", "Another project");
        anotherProject.assignTo(anotherUser);

        entityManager.persist(project);
        entityManager.persist(anotherProject);

        entityManager.persist(
                new ProjectMember(project, owner, ProjectRole.OWNER)
        );

        entityManager.persist(
                new ProjectMember(project, member, ProjectRole.MEMBER)
        );

        task = createTask("First task", project);
        anotherTask = createTask("Another task", anotherProject);

        entityManager.flush();
    }

    private Task createTask(String title, Project taskProject) {
        Task newTask = new Task(title, "Integration test task");
        newTask.assignTo(taskProject);
        newTask.setCreatedBy(owner);
        newTask.updatePlanning(
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(1)
        );

        entityManager.persist(newTask);
        entityManager.flush();

        return newTask;
    }

    private TaskHistory createHistory(
            Task historyTask,
            User historyUser,
            TaskHistoryAction action,
            String fieldName,
            String oldValue,
            String newValue
    ) {
        TaskHistory history = new TaskHistory(
                historyTask,
                historyUser,
                action,
                fieldName,
                oldValue,
                newValue
        );

        entityManager.persist(history);
        entityManager.flush();

        return history;
    }

    @Test
    @DisplayName("Should return all history records belonging to a task")
    void test_01() {
        TaskHistory history1 = createHistory(
                task, owner, TaskHistoryAction.CREATED,
                null, null, null
        );

        TaskHistory history2 = createHistory(
                task, member, TaskHistoryAction.UPDATED,
                "title", "Old title", "New title"
        );

        Page<TaskHistory> result = taskHistoryRepository.findAllByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .extracting(TaskHistory::getId)
                .containsExactlyInAnyOrder(history1.getId(), history2.getId());
    }

    @Test
    @DisplayName("Should return empty page when task has no history")
    void test_02() {
        Page<TaskHistory> result = taskHistoryRepository.findAllByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should not return history belonging to another task")
    void test_03() {
        TaskHistory history = createHistory(
                anotherTask, anotherUser, TaskHistoryAction.CREATED,
                null, null, null
        );

        Page<TaskHistory> result = taskHistoryRepository.findAllByTaskId(
                task.getId(), pageable
        );

        assertThat(result.getContent())
                .extracting(TaskHistory::getId)
                .doesNotContain(history.getId());
    }

    @Test
    @DisplayName("Should return history when task belongs to the specified project")
    void test_04() {
        TaskHistory history = createHistory(
                task, owner, TaskHistoryAction.CREATED,
                null, null, null
        );

        Page<TaskHistory> result =
                taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                        task.getId(), project.getId(), pageable
                );

        assertThat(result.getContent())
                .extracting(TaskHistory::getId)
                .containsExactly(history.getId());
    }

    @Test
    @DisplayName("Should return empty page when task does not belong to specified project")
    void test_05() {
        createHistory(
                task, owner, TaskHistoryAction.CREATED,
                null, null, null
        );

        Page<TaskHistory> result =
                taskHistoryRepository.findAllByTaskIdAndTaskProjectId(
                        task.getId(), anotherProject.getId(), pageable
                );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should paginate history records")
    void test_06() {
        for (int i = 1; i <= 5; i++) {
            createHistory(
                    task, owner, TaskHistoryAction.UPDATED,
                    "title", "Old " + i, "New " + i
            );
        }

        Pageable firstPage = PageRequest.of(0, 2);

        Page<TaskHistory> result = taskHistoryRepository.findAllByTaskId(
                task.getId(), firstPage
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isZero();
        assertThat(result.hasNext()).isTrue();
    }
    @Test
    @DisplayName("Should preserve deletion history after task is deleted")
    void test_07() {
        TaskHistory history = createHistory(
                task,
                owner,
                TaskHistoryAction.DELETED,
                null,
                null,
                null
        );

        Long taskId = task.getId();
        Long historyId = history.getId();
        Long ownerId = owner.getId();

        entityManager.clear();

        entityManager.createQuery(
                        "DELETE FROM Task t WHERE t.id = :taskId"
                )
                .setParameter("taskId", taskId)
                .executeUpdate();

        entityManager.clear();

        TaskHistory persistedHistory =
                entityManager.find(TaskHistory.class, historyId);

        assertThat(persistedHistory).isNotNull();
        assertThat(persistedHistory.getAction())
                .isEqualTo(TaskHistoryAction.DELETED);
        assertThat(persistedHistory.getTask()).isNull();
        assertThat(persistedHistory.getUser().getId())
                .isEqualTo(ownerId);

        assertThat(persistedHistory.getFieldName()).isNull();
        assertThat(persistedHistory.getOldValue()).isNull();
        assertThat(persistedHistory.getNewValue()).isNull();

        assertThat(entityManager.find(Task.class, taskId)).isNull();
    }
}
