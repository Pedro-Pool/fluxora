package com.fluxora.repository;

import com.fluxora.config.AbstractIntegrationTest;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TaskRepositoryIntegrationTest extends AbstractIntegrationTest {
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
    @Test
    @DisplayName("Should return overdue tasks belonging to the user")
    void test_12(){
        LocalDate today = LocalDate.now();

        Task overdue = createTask(
                "Overdue task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.minusDays(1),
                project
        );
        createTask(
                "Future Task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).extracting(Task::getId).containsExactly(overdue.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
    @Test
    @DisplayName("Should not return tasks due today")
    void test_13() {
        LocalDate today = LocalDate.now();

        createTask(
                "Due today",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return completed overdue tasks")
    void test_14() {
        LocalDate today = LocalDate.now();

        createTask(
                "Completed overdue task",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                today.minusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return canceled overdue tasks")
    void test_15() {
        LocalDate today = LocalDate.now();

        createTask(
                "Completed overdue task",
                TaskStatus.CANCELED,
                TaskPriority.HIGH,
                today.minusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return overdue tasks belonging to another user")
    void test_16() {
        LocalDate today = LocalDate.now();

        createTask(
                "Another user's overdue tasks",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.minusDays(1),
                anotherProject
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks without a due date")
    void test_17() {
        LocalDate today = LocalDate.now();

        createTask(
                "Task without due date",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                null,
                anotherProject
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should return tasks due date belonging to the user")
    void test_18(){
        LocalDate today = LocalDate.now();

        Task dueDate = createTask(
                "Overdue task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today,
                project
        );
        createTask(
                "Future Task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).extracting(Task::getId).containsExactly(dueDate.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
    @Test
    @DisplayName("Should not return tasks due tomorrow")
    void test_19() {
        LocalDate today = LocalDate.now();

        createTask(
                "Task due tomorrow",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return overdue tasks")
    void test_20() {
        LocalDate today = LocalDate.now();

        createTask(
                "Overdue task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.minusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return completed tasks due today")
    void test_21() {
        LocalDate today = LocalDate.now();

        createTask(
                "Completed task",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                today,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return canceled tasks due today")
    void test_22() {
        LocalDate today = LocalDate.now();

        createTask(
                "Canceled task",
                TaskStatus.CANCELED,
                TaskPriority.HIGH,
                today,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks due today belonging ti another user")
    void test_23() {
        LocalDate today = LocalDate.now();

        createTask(
                "Another user's task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today,
                anotherProject
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks without a due date")
    void test_24() {
        LocalDate today = LocalDate.now();

        createTask(
                "Task without due date",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                null,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateAndStatusNotIn(
                owner.getId(),
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should return upcoming tasks within the date range")
    void test_25() {
        LocalDate today = LocalDate.now();

        Task upcomingTask = createTask(
                "Upcoming task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(6),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).extracting(Task::getId).containsExactly(upcomingTask.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
    @Test
    @DisplayName("Should not return tasks due before today")
    void test_26() {
        LocalDate today = LocalDate.now();

        createTask(
                "Overdue task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.minusDays(1),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks due after the date range")
    void test_27() {
        LocalDate today = LocalDate.now();

        createTask(
                "Future task",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(8),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return completed upcoming tasks")
    void test_28() {
        LocalDate today = LocalDate.now();

        createTask(
                "Completed upcoming task",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                today.plusDays(3),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return canceled upcoming tasks")
    void test_29() {
        LocalDate today = LocalDate.now();

        createTask(
                "Canceled upcoming task",
                TaskStatus.CANCELED,
                TaskPriority.HIGH,
                today.plusDays(3),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return upcoming tasks belonging to another user")
    void test_30() {
        LocalDate today = LocalDate.now();

        createTask(
                "Task without due date",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(3),
                anotherProject
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks without a due date")
    void test_31() {
        LocalDate today = LocalDate.now();

        createTask(
                "Task without due date",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                null,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should not return tasks due exactly today")
    void test_32() {
        LocalDate today = LocalDate.now();

        Task dueToday = createTask(
                "Task due today",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today,
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).extracting(Task::getId).containsExactly(dueToday.getId());
    }
    @Test
    @DisplayName("Should not return tasks without a due date")
    void test_33() {
        LocalDate today = LocalDate.now();

        Task lastDayTask = createTask(
                "Task due on last today",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                today.plusDays(7),
                project
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.Direction.ASC, "dueDate");

        Page<Task> result = taskRepository.findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
                owner.getId(),
                today,
                today.plusDays(7),
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
        assertThat(result).extracting(Task::getId).containsExactly(lastDayTask.getId());
    }
}
