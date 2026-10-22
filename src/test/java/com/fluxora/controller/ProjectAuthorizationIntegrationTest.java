package com.fluxora.controller;

import com.fluxora.entity.Project;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.UserRepository;
import com.fluxora.service.JwtService;
import com.fluxora.service.ProjectService;
import com.fluxora.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProjectAuthorizationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    private User userA;
    private User userB;
    private Project projectA;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp(){
        userA = userService.register(
                "User A",
                "UserA"+ UUID.randomUUID() +"@email.com",
                "passwordUserA"
        );
        userB = userService.register(
                "User B",
                "UserB"+ UUID.randomUUID() +"@email.com",
                "passwordUserB"
        );
        tokenA = jwtService.generateToken(userA.getEmail());
        tokenB = jwtService.generateToken(userB.getEmail());

        projectA = projectService.create(
                "Private Project A",
                "Project owned by user A",
                userA.getEmail()
        );
    }
    @AfterEach
    void tearDown(){
        if (userA != null && userA.getId() != null){
            projectRepository.deleteAll(projectRepository.findAllByUserId(userA.getId()));
        }
        if (userB != null && userB.getId() != null){
            projectRepository.deleteAll(projectRepository.findAllByUserId(userB.getId()));
        }
        if (userA != null && userA.getId() != null){
            userRepository.delete(userA);
        }
        if (userB != null && userB.getId() != null){
            userRepository.delete(userB);
        }
    }
    @Test
    @DisplayName("A user should only see their own project")
    void test_01() throws Exception{
        mockMvc.perform(get("/api/projects")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer "+ tokenB))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Private Project A"));
    }
    @Test
    @DisplayName("An authenticated user can create a project")
    void test_02() throws Exception{
        mockMvc.perform(post("/api/projects")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer "+ tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "New Project",
                                  "description": "Created through the API"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Project"))
                .andExpect(jsonPath("$.description").value("Created through the API"));
    }
    @Test
    @DisplayName("An authenticated user can read their own project")
    void test_03() throws Exception{
        mockMvc.perform(get("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectA.getId()))
                .andExpect(jsonPath("$.name").value(projectA.getName()));
    }
    @Test
    @DisplayName("A user cannot read another user's project")
    void test_04() throws Exception{
        mockMvc.perform(get("/api/tasks/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("A user cannot update another user's project")
    void test_05() throws Exception{
        mockMvc.perform(patch("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                        "Bearer "+ tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Attempted Change",
                                  "description": "This change must not be saved"
                                }
                                """))
                .andExpect(status().isNotFound());

        assertTrue(projectRepository.findByIdAndUserId(projectA.getId(), userA.getId()).isPresent());
    }
    @Test
    @DisplayName("An authenticated user can update their own project")
    void test_06() throws Exception{
        mockMvc.perform(patch("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "Updated Project",
                                  "description": "Updated description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }
    @Test
    @DisplayName("A cannot delete another user's project")
    void test_07() throws Exception{
        mockMvc.perform(delete("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenB))
                .andExpect(status().isNotFound());

        assertTrue(projectRepository.findByIdAndUserId(projectA.getId(), userA.getId()).isPresent());
    }
    @Test
    @DisplayName("An authenticated user can delete their own project")
    void test_08() throws Exception{
        mockMvc.perform(delete("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer "+ tokenA))
                .andExpect(status().isNoContent());

        assertTrue(projectRepository.findById(projectA.getId()).isEmpty());
    }
    @Test
    @DisplayName("Request without authentication are rejected")
    void test_09() throws Exception{
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }
}
