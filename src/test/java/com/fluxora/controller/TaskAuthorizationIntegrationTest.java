package com.fluxora.controller;

import com.fluxora.entity.*;
import com.fluxora.repository.*;
import com.fluxora.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskAuthorizationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;
    private User userC;
    private Project projectA;
    private Project projectB;
    private Task taskA;
    private Task taskB;
    private Category categoryA;
    private String tokenA;
    private String tokenB;
    private String tokenC;

    @BeforeEach
    void setUp() {
        userA = userService.register("User A", "userA" + UUID.randomUUID() + "@email.com", "passwordUserA");
        userB = userService.register("User B", "userB" + UUID.randomUUID() + "@email.com", "passwordUserB");
        userC = userService.register("User C", "userC" + UUID.randomUUID() + "@email.com", "passwordUserC");

        projectA = projectService.create("Project A", "Project A description", userA.getEmail());
        projectB = projectService.create("Project B", "Project B description", userB.getEmail());

        categoryA = categoryService.create("Backend", userA.getEmail());

        tokenA = jwtService.generateToken(userA.getEmail());
        tokenB = jwtService.generateToken(userB.getEmail());
        tokenC = jwtService.generateToken(userC.getEmail());

        taskA = taskService.create(
                projectA.getId(),
                "Task private A",
                "test",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                userA.getEmail());

        taskB = taskService.create(
                projectB.getId(),
                "Task private B",
                "test",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                userB.getEmail());

        projectMemberRepository.save(new ProjectMember(projectA, userC, ProjectRole.MEMBER));
    }

    @AfterEach
    void tearDown() {
        if (projectA != null && projectA.getId() != null) {
            taskRepository.deleteAll(
                    taskRepository.findAllByProjectId(projectA.getId())
            );
        }

        if (projectB != null && projectB.getId() != null) {
            taskRepository.deleteAll(
                    taskRepository.findAllByProjectId(projectB.getId())
            );
        }

        if (categoryA != null && categoryA.getId() != null) {
            categoryRepository.deleteById(categoryA.getId());
        }
        if (userB != null && userB.getId() != null) {
            categoryRepository.deleteAll(categoryRepository.findAllByUserId(userB.getId()));
        }

        if (projectA != null && projectA.getId() != null) {
            projectMemberRepository.deleteAll(
                    projectMemberRepository.findByProjectId(projectA.getId())
            );
            projectRepository.deleteById(projectA.getId());
        }

        if (projectB != null && projectB.getId() != null) {
            projectMemberRepository.deleteAll(
                    projectMemberRepository.findByProjectId(projectB.getId())
            );
            projectRepository.deleteById(projectB.getId());
        }

        if (projectA != null && projectA.getId() != null) {
            projectRepository.deleteById(projectA.getId());
        }
        if (projectB != null && projectB.getId() != null) {
            projectRepository.deleteById(projectB.getId());
        }

        if (userA != null && userA.getId() != null) {
            userRepository.delete(userA);
        }
        if (userB != null && userB.getId() != null) {
            userRepository.delete(userB);
        }
        if (userC != null && userC.getId() != null) {
            userRepository.delete(userC);
        }
    }

    @Test
    @DisplayName("The user should only see their own tasks")
    void test_01() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title")
                        .value("Task private A"));
    }

    @Test
    @DisplayName("A user cannot read another user's task.")
    void test_02() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("It is not possible to update another user's task")
    void test_03() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Attempted change",
                                  "description": "She should not be saved"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A user cannot complete another user's task.")
    void test_04() throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/complete", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The user cannot open another user's task")
    void test_05() throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/reopen", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A user cannot delete another user's task")
    void test_06() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(taskRepository.findByIdAndProjectMemberUserId(taskA.getId(), userA.getId()).isPresent());
    }

    @Test
    @DisplayName("A user cannot cancel another user's task")
    void test_07() throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/cancel", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(taskRepository.findByIdAndProjectMemberUserId(taskA.getId(), userA.getId()).isPresent());
    }

    @Test
    @DisplayName("A request without a token must return 401")
    void test_08() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", taskA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A request with an invalid token must return 401")
    void test_09() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A user should associate a category with their own task")
    void test_10() throws Exception {
        mockMvc.perform(put("/api/tasks/{taskId}/categories/{categoryId}",
                        taskA.getId(),
                        categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        Integer associationCount = jdbcTemplate.queryForObject(
                """
                    
                    SELECT COUNT(*)
                    FROM task_categories
                    WHERE task_id = ? AND category_id = ?
                    """,
                Integer.class,
                taskA.getId(),
                categoryA.getId()
        );
        assertEquals(1, associationCount);
    }
    @Test
    @DisplayName("A user not associate another user's category with their task")
    void test_11() throws Exception{
        mockMvc.perform(put("/api/tasks/{taskId}/categories/{categoryId}",
                    taskB.getId(),
                    categoryA.getId())
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer "+ tokenB))
                .andExpect(status().isNotFound());

        Integer associationCount = jdbcTemplate.queryForObject(
                        """
                    SELECT COUNT(*)
                    FROM task_categories
                    WHERE task_id = ? AND category_id = ?
                    """,
                Integer.class,
                taskB.getId(),
                categoryA.getId()
        );

    assertEquals(0, associationCount);
    }
    @Test
    @DisplayName("Removing a category from a task should preserve both entity ies")
    void test_12() throws Exception{
        mockMvc.perform(put("/api/tasks/{taskId}/categories/{categoryId}",
                    taskA.getId(),
                    categoryA.getId())
                    .header(HttpHeaders.
                                AUTHORIZATION,
                            "Bearer "+ tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/tasks/{taskId}/categories/{categoryId}",
                        taskA.getId(),
                        categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isNoContent());

        Integer associationCount = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM task_categories
                    WHERE task_id = ? AND category_id = ?
                    """,
                Integer.class,
                taskA.getId(),
                categoryA.getId()
        );

        assertEquals(0, associationCount);

        assertTrue(taskRepository.existsById(taskA.getId()));
        assertTrue(categoryRepository.existsById(categoryA.getId()));
    }
    @Test
    @DisplayName("Deleting  an associated category should preserve the task and remove associations")
    void test_13() throws Exception{
        mockMvc.perform(put("/api/tasks/{taskId}/categories/{categoryId}",
                        taskA.getId(),
                        categoryA .getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/categories/{id}",
                        categoryA .getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isNoContent());

        assertFalse(categoryRepository.existsById(categoryA.getId()));
        assertTrue(taskRepository.existsById(taskA.getId(
                        )));
                        Integer associationCount =
                        jdbcTemplate.
                        queryForObject(
                """
                    SELECT COUNT(*)
                    FROM task_categories
                   WHERE category_id = ?
                   """,
                Integer.class,
                categoryA .getId()
        );
        assertEquals(0, associationCount);
    }
    @Test
    @DisplayName("A user should not delete another user's category")
    void test_14() throws Exception{
        mockMvc .perform(delete("/api/categories/{id}",
                        categoryA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());

        assertTrue(categoryRepository.existsById(categoryA.getId()));
    }
    @Test
    @DisplayName("A user should not rename another user's category")
    void test_15() throws Exception{
        mockMvc.perform(put("/api/categories/{id}", categoryA.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Renamed by B"
                    }
                    """))
                .andExpect(status().isNotFound()

    );

        Category category = categoryRepository
                .findById(categoryA.getId())
                . orElseThrow();

        assertEquals("Backend", category.getName());
    }
    @Test
    @DisplayName("A user should not create categories with duplicate names")
    void test_16() throws Exception{
        mockMvc.perform(post("/api/categories")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenA
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Backend"
                    }
                    """))
                .andExpect(
                        status().isConflict());

        Integer categoryCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM categories
                WHERE user_id = ? AND name = ?
                """,
                Integer.class,
                userA.getId(),
                "Backend"
        );

        assertEquals(1, categoryCount);
    }
    @Test
    @DisplayName("Different users should be allowed to create categories with same name")
    void test_17() throws Exception{
        mockMvc.perform(post("/api/categories")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenB
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name":
                        "Backend"
                    }
                    """)
                        )
                .andExpect(
                        status().isCreated());

        Integer categoryCount = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM categories
                    WHERE name = ?
                    AND user_id = ?
                    """,
                Integer.class,
                "Backend",
                userB.getId()
        );
        assertEquals(1, categoryCount);
    }
    @Test
    @DisplayName("Should assign a task to a project member")
    void test_18() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Task private A",
                        "description": "test",
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "assigneeId": %d
                    }
                    """.formatted(userC.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(userC.getId()))
                .andExpect(jsonPath("$.assigneeName").value("User C"))
                .andExpect(jsonPath("$.assigneeEmail").value(userC.getEmail()));
    }
    @Test
    @DisplayName("Should not assign a task to a user who is not a project member")
    void test_19() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Task private A",
                        "description": "test",
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "assigneeId": %d
                    }
                    """.formatted(userB.getId())))
                .andExpect(status().isBadRequest());

        Task unchangedTask = taskRepository.findById(taskA.getId()).orElseThrow();

        assertNull(unchangedTask.getAssignee());
    }
    @Test
    @DisplayName("Should remove task assignee")
    void test_20() throws Exception {
        taskA.assignTo(userC);
        taskRepository.save(taskA);

        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Task private A",
                        "description": "test",
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "assigneeId": null
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").doesNotExist());

        Task updatedTask = taskRepository.findById(taskA.getId()).orElseThrow();

        assertNull(updatedTask.getAssignee());
    }
    @Test
    @DisplayName("Should create a task with an assigned project member")
    void test_21() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Assigned task",
                        "description": "Task assigned during creation",
                        "status": "TODO",
                        "priority": "HIGH",
                        "assigneeId": %d
                    }
                    """.formatted(userC.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Assigned task"))
                .andExpect(jsonPath("$.assigneeId").value(userC.getId()))
                .andExpect(jsonPath("$.assigneeName").value("User C"))
                .andExpect(jsonPath("$.assigneeEmail").value(userC.getEmail()))
                .andExpect(jsonPath("$.createdById").value(userA.getId()))
                .andExpect(jsonPath("$.createdByEmail").value(userA.getEmail()));
    }
    @Test
    @DisplayName("Should not allow changing task creator through update")
    void test_22() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Task private A",
                        "description": "test",
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "createdById": %d
                    }
                    """.formatted(userB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdById").value(userA.getId()))
                .andExpect(jsonPath("$.createdByEmail").value(userA.getEmail()));

        Task updatedTask = taskRepository.findById(taskA.getId()).orElseThrow();

        assertEquals(userA.getId(), updatedTask.getCreatedBy().getId());
    }
    @Test
    @DisplayName("Should not assign a task to a nonexistent user")
    void test_23() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "title": "Task private A",
                        "description": "test",
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "assigneeId": 999999
                    }
                    """))
                .andExpect(status().isNotFound());

        Task unchangedTask = taskRepository.findById(taskA.getId()).orElseThrow();

        assertNull(unchangedTask.getAssignee());
    }
}