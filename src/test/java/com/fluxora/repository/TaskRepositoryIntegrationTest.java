package com.fluxora.repository;

import com.fluxora.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

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
    private Pageable pageable;

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

        pageable = PageRequest.of(0, 10);

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
    private Category createCategory(String name, User user){
        Category category = new Category(name);
        category.assignTo(user);

        entityManager.persist(category);
        entityManager.flush();

        return category;
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
        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                TaskStatus.TODO, null, null, null,null, pageable
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

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, TaskPriority.HIGH, null, null,null, pageable
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
        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, from, to,null, pageable
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

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, from, to,null, pageable
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

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null,
                LocalDate.of(2026,10,10),
                LocalDate.of(2026,10,20),
                null,
                pageable
        );

        assertThat(result).extracting(Task::getId).containsExactly(ownTask.getId());
    }
    @Test
    @DisplayName("Should return tasks ordered by title ascending")
    void test_06(){
        createTask(
                "Charlie",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        createTask(
                "Alpha",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        createTask(
                "Bravo",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        Pageable sortedPageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "title")
        );
        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null,null, sortedPageable
        );

        assertThat(result).extracting(Task::getTitle).containsExactly("Alpha", "Bravo", "Charlie");
    }
    @Test
    @DisplayName("Should return tasks ordered by title descending")
    void test_07(){
        createTask(
                "Charlie",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        createTask(
                "Alpha",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        createTask(
                "Bravo",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026,10,20),
                project
        );
        Pageable sortedPageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "title")
        );
        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null,null, sortedPageable
        );

        assertThat(result).extracting(Task::getTitle).containsExactly("Charlie", "Bravo", "Alpha");
    }
    @Test
    @DisplayName("Should paginate tasks correctly and return accurate pagination metadata")
    void test_08() {
        createTask(
                "Charlie",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        createTask(
                "Alpha",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        createTask(
                "Bravo",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        Pageable sortedPageable = PageRequest.of(
                0, 1, Sort.by(Sort.Direction.ASC, "title")
        );
        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null,null, sortedPageable
        );
        assertThat(result).extracting(Task::getTitle).containsExactly("Alpha");
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(1);
    }
    @Test
    @DisplayName("Should return only tasks matching the selected category")
    void test_09(){
        Category javaCategory = createCategory("Java", owner);
        Category springCategory = createCategory("Spring", owner);

        Task javaTask = createTask(
                "Study java",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        Task springTask = createTask(
                "Study spring",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        javaTask.addCategory(javaCategory);
        springTask.addCategory(springCategory);

        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null,javaCategory.getId(), pageable
        );
        assertThat(result).extracting(Task::getTitle).containsExactly("Study java");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
    @Test
    @DisplayName("Should not return tasks when filtering by another user's category")
    void test_10(){
        Category anotherUserCategory = createCategory("Private category", anotherUser);
        Task ownTask = createTask(
                "Owner task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        ownTask.addCategory(anotherUserCategory);

        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null, anotherUserCategory.getId(), pageable
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
    @Test
    @DisplayName("Should paginate correctly when filtering by category")
    void test_11(){
        Category javaCategory = createCategory("Java", owner);
        Category springCategory = createCategory("Spring", owner);

        Task javaTask1 = createTask(
                "Java A",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        Task javaTask2 = createTask(
                "Java B",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        Task springTask = createTask(
                "Spring A",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                project
        );
        javaTask1.addCategory(javaCategory);
        javaTask2.addCategory(javaCategory);
        springTask.addCategory(springCategory);

        entityManager.flush();
        entityManager.clear();

        Pageable oneItemPerPage = PageRequest.of(0, 1, Sort.Direction.ASC, "title");

        Page<Task> result = taskRepository.findAllWithFilters(
                project.getId(), owner.getId(),
                null, null, null, null, javaCategory.getId(), oneItemPerPage
        );
        assertThat(result).extracting(Task::getTitle).containsExactly("Java A");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
    }
}
