package com.fluxora.repository;

import com.fluxora.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TaskRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("fluxora_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernet.ddl-auto", () -> "create-drop");
    }
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User anotherUser;
    private Project project;
    private Project anotherProject;

    @BeforeEach
    void setUp(){
        owner = new User(
                "Owner",
                "owner@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );
        anotherUser = new User(
                "Another User",
                "another@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );
        entityManager.persist(owner);
        entityManager.persist(anotherUser);

        project = new Project("Owner Project", "Test project");
        project.assignTo(owner);

        anotherProject = new Project("Another Project", "Test another project");
        anotherProject.assignTo(anotherUser);

        entityManager.persist(project);
        entityManager.persist(anotherProject);

        entityManager.flush();
    }
    private Task createTask(
            String title,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Project taskProject
    ){
        Task task = new Task(title, "Integration test task");
        task.assignTo(taskProject);
        task.updatePlanning(status,priority, dueDate);

        entityManager.persist(task);
        entityManager.flush();

        return task;
    }
    @Test
    @DisplayName("Should return only tasks matching select status")
    void test_01(){
        Task matching = createTask(
                "Todo task",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.of(2026,10,20),
                project);

        createTask(
                "Done task",
                TaskStatus.DONE,
                TaskPriority.MEDIUM,
                LocalDate.of(2026,10,20),
                project
        );
        List<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                TaskStatus.TODO, null, null, null
        );

        assertThat(result).extracting(Task::getId).containsExactly(matching.getId());
    }
    @Test
    @DisplayName("Should return only tasks matching select priority")
    void test_02(){
        Task matching = createTask(
                "High priority",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project);

        createTask(
                "Low priority",
                TaskStatus.DONE,
                TaskPriority.LOW,
                LocalDate.of(2026,10,20),
                project
        );

        List<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, TaskPriority.HIGH, null, null
        );

        assertThat(result).extracting(Task::getId).containsExactly(matching.getId());
    }
    @Test
    @DisplayName("Should include task on both date boundaries")
    void test_03(){
        LocalDate from = LocalDate.of(2026,10,10);
        LocalDate to = LocalDate.of(2026,10,20);

        Task onStart = createTask(
                "Start boundary",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                from,
                project);

        Task insideRange = createTask(
                "Inside Range",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.of(2026,10,15),
                project);

        Task onEnd = createTask(
                "End boundary",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                to,
                project);

        createTask(
                "Before range",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                from.minusDays(1),
                project
        );
        createTask(
                "After range",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                to.plusDays(1),
                project
        );
        List<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, from, to
        );

        assertThat(result).extracting(Task::getId).containsExactlyInAnyOrder(
                onStart.getId(),
                insideRange.getId(),
                onEnd.getId()
        );
    }
    @Test
    @DisplayName("Should exclude task without a due date when date filters are active")
    void test_04(){
        LocalDate from = LocalDate.of(2026,10,10);
        LocalDate to = LocalDate.of(2026,10,20);

        Task withDueDate = createTask(
                "Dated task",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.of(2026,10,15),
                project);

        createTask(
                "Undated task",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                project
        );

        List<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, from, to
        );

        assertThat(result).extracting(Task::getId).containsExactly(withDueDate.getId());
    }
    @Test
    @DisplayName("Should not return task belonging to another user's project")
    void test_05(){
        Task ownTask = createTask(
                "Own task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,15),
                project);

        createTask(
                "Other user's task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,15),
                anotherProject
        );

        List<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null,
                LocalDate.of(2026,10,10),
                LocalDate.of(2026,10,20)
        );

        assertThat(result).extracting(Task::getId).containsExactly(ownTask.getId());
    }
}
