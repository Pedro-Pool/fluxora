package com.fluxora.controller;

import com.fluxora.entity.Project;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import com.fluxora.service.JwtService;
import com.fluxora.service.ProjectService;
import com.fluxora.service.TaskService;
import com.fluxora.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskAuthorizationIntegrationTest {
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

    private User userA;
    private User userB;
    private Project projectA;
    private Task taskA;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp(){
        userA = userService.register("User A", "userA"+ UUID.randomUUID() +"@email.com", "passwordUserA");
        userB = userService.register("User B", "userB"+ UUID.randomUUID() +"@email.com", "passwordUserB");

        projectA = projectService.create("Project A", "Project A description", userA.getEmail());

        tokenA = jwtService.generateToken(userA.getEmail());
        tokenB = jwtService.generateToken(userB.getEmail());

        taskA = taskService.create(projectA.getId(), "Task private A", "test", userA.getEmail());
    }
    @AfterEach
    void tearDown(){
        if (taskA != null && taskA.getId() != null){
            taskRepository.deleteById(taskA.getId());
        }
        if (projectA != null && projectA.getId() != null){
            projectRepository.deleteById(projectA.getId());
        }
        if (userA != null && userA.getId() != null){
            userRepository.delete(userA);
        }
        if (userB != null && userB.getId() != null){
            userRepository.delete(userB);
        }
    }
    @Test
    @DisplayName("The user should only see their own tasks")
    void test_01() throws Exception{
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectA.getId())
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer "+ tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title")
                        .value("Task private A"));
    }
    @Test
    @DisplayName("A user cannot read another user's task.")
    void test_02() throws Exception{
        mockMvc.perform(get("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("It is not possible to update another user's task")
    void test_03() throws Exception{
        mockMvc.perform(put("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB)
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
    void test_04() throws Exception{
        mockMvc.perform(patch("/api/tasks/{id}/complete", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("The user cannot open another user's task")
    void test_05() throws Exception{
        mockMvc.perform(patch("/api/tasks/{id}/reopen", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("A user cannot delete another user's task")
    void test_06() throws Exception{
        mockMvc.perform(delete("/api/tasks/{id}", taskA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());

        assertTrue(taskRepository.findByIdAndProjectUserId(taskA.getId(), userA.getId()).isPresent());
    }
}
