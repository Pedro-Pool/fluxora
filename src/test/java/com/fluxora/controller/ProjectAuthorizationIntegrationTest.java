package com.fluxora.controller;

import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.enums.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectAuthorizationIntegrationTest {
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

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private User userA;
    private User userB;
    private User userC;
    private Project projectA;
    private String tokenA;
    private String tokenB;
    private String tokenC;

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
        userC = userService.register(
                "User C",
                "UserC"+ UUID.randomUUID() +"@emai.com",
                "passwordUserC"
        );
        tokenA = jwtService.generateToken(userA.getEmail());
        tokenB = jwtService.generateToken(userB.getEmail());
        tokenC = jwtService.generateToken(userC.getEmail());

        projectA = projectService.create(
                "Private Project A",
                "Project owned by user A",
                userA.getEmail()
        );
    }
    @AfterEach
    void tearDown(){
        if (userA != null && userA.getId() != null){
            var projectA = projectRepository.findAllByUserId(userA.getId());

            projectA.forEach(project ->
                    projectMemberRepository.deleteAll(
                            projectMemberRepository.findByProjectId(project.getId())
                    ));

            projectRepository.deleteAll(projectA);
            userRepository.delete(userA);
        }

        if (userB != null && userB.getId() != null){
            var projectsB = projectRepository.findAllByUserId(userB.getId());

            projectsB.forEach(project ->
                    projectMemberRepository.deleteAll(
                            projectMemberRepository.findByProjectId(project.getId())
                    ));

            projectRepository.deleteAll(projectsB);
            userRepository.delete(userB);
        }
        if (userC != null && userC.getId() != null) {
            userRepository.delete(userC);
        }
    }
    @Test
    @DisplayName("A user should only see their own project")
    void test_01() throws Exception {

        mockMvc.perform(get("/api/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get("/api/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Private Project A"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
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
        mockMvc.perform(get("/api/projects/{id}", projectA.getId())
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
    @Test
    @DisplayName("An ADMIN member can update the project")
    void test_10() throws Exception {
        ProjectMember admin = new ProjectMember(
                projectA,
                userB,
                ProjectRole.ADMIN
        );

        projectA.addMember(admin);
        projectMemberRepository.save(admin);

        mockMvc.perform(patch("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Updated by ADMIN",
                              "description": "Updated by project administrator"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated by ADMIN"))
                .andExpect(jsonPath("$.description")
                        .value("Updated by project administrator"));
    }
    @Test
    @DisplayName("A MEMBER cannot update the project")
    void test_11() throws Exception {
        ProjectMember member = new ProjectMember(
                projectA,
                userB,
                ProjectRole.MEMBER
        );

        projectA.addMember(member);
        projectMemberRepository.save(member);

        mockMvc.perform(patch("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Attempted Change",
                              "description": "This change must not be saved"
                            }
                            """))
                .andExpect(status().isForbidden());

        Project persistedProject = projectRepository.findById(projectA.getId())
                .orElseThrow();

        assertEquals("Private Project A", persistedProject.getName());
        assertEquals("Project owned by user A", persistedProject.getDescription());
    }
    @Test
    @DisplayName("An ADMIN member cannot delete the project")
    void test_12() throws Exception {
        ProjectMember admin = new ProjectMember(
                projectA,
                userB,
                ProjectRole.ADMIN
        );

        projectA.addMember(admin);
        projectMemberRepository.save(admin);

        mockMvc.perform(delete("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        assertTrue(projectRepository.findById(projectA.getId()).isPresent());
    }
    @Test
    @DisplayName("A MEMBER cannot delete the project")
    void test_13() throws Exception {
        ProjectMember member = new ProjectMember(
                projectA,
                userB,
                ProjectRole.MEMBER
        );

        projectA.addMember(member);
        projectMemberRepository.save(member);

        mockMvc.perform(delete("/api/projects/{id}", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        assertTrue(projectRepository.findById(projectA.getId()).isPresent());
    }
    @Test
    @DisplayName("An OWNER can add a member with ADMIN role")
    void test_14() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/members", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "role": "ADMIN"
                            }
                            """.formatted(userB.getEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(userB.getEmail()))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(projectA.getId(), userB.getId())
                .orElseThrow();

        assertEquals(ProjectRole.ADMIN, membership.getRole());
    }
    @Test
    @DisplayName("An OWNER can add a member with MEMBER role")
    void test_15() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/members", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "role": "MEMBER"
                            }
                            """.formatted(userB.getEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(userB.getEmail()))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(projectA.getId(), userB.getId())
                .orElseThrow();

        assertEquals(ProjectRole.MEMBER, membership.getRole());
    }
    @Test
    @DisplayName("An ADMIN can add a member with MEMBER role")
    void test_16() throws Exception {
        ProjectMember admin = new ProjectMember(
                projectA,
                userB,
                ProjectRole.ADMIN
        );

        projectA.addMember(admin);
        projectMemberRepository.save(admin);

        mockMvc.perform(post("/api/projects/{projectId}/members", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "role": "MEMBER"
                            }
                            """.formatted(userC.getEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(userC.getEmail()))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(projectA.getId(), userC.getId())
                .orElseThrow();

        assertEquals(ProjectRole.MEMBER, membership.getRole());
    }
    @Test
    @DisplayName("An ADMIN cannot add another member with ADMIN role")
    void test_17() throws Exception {
        ProjectMember admin = new ProjectMember(
                projectA,
                userB,
                ProjectRole.ADMIN
        );

        projectA.addMember(admin);
        projectMemberRepository.save(admin);

        mockMvc.perform(post("/api/projects/{projectId}/members", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "role": "ADMIN"
                            }
                            """.formatted(userC.getEmail())))
                .andExpect(status().isForbidden());

        assertTrue(projectMemberRepository
                .findByProjectIdAndUserId(projectA.getId(), userC.getId())
                .isEmpty());
    }
    @Test
    @DisplayName("A MEMBER cannot add another member to the project")
    void test_18() throws Exception {
        ProjectMember member = new ProjectMember(
                projectA,
                userB,
                ProjectRole.MEMBER
        );

        projectA.addMember(member);
        projectMemberRepository.save(member);

        mockMvc.perform(post("/api/projects/{projectId}/members", projectA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "role": "MEMBER"
                            }
                            """.formatted(userC.getEmail())))
                .andExpect(status().isForbidden());

        assertTrue(projectMemberRepository
                .findByProjectIdAndUserId(projectA.getId(), userC.getId())
                .isEmpty());
    }
    @Test
    @DisplayName("Should allow OWNER to remove an ADMIN member")
    void test_19() throws Exception {
        ProjectMember admin = projectMemberRepository.save(
                new ProjectMember(projectA, userC, ProjectRole.ADMIN)
        );

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        admin.getId()
                )
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertFalse(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userC.getId()
                )
        );
    }
    @Test
    @DisplayName("Should allow OWNER to remove a MEMBER")
    void test_20() throws Exception {
        ProjectMember regularMember = projectMemberRepository.save(
                new ProjectMember(projectA, userC, ProjectRole.MEMBER)
        );

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        regularMember.getId()
                )
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertFalse(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userC.getId()
                )
        );
    }
    @Test
    @DisplayName("Should prevent OWNER from removing another OWNER")
    void test_21() throws Exception {
        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        projectMemberRepository.findByProjectIdAndUserId(
                                projectA.getId(),
                                userA.getId()
                        ).orElseThrow().getId()
                )
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        assertTrue(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userA.getId()
                )
        );
    }
    @Test
    @DisplayName("Should prevent ADMIN from removing members")
    void test_22() throws Exception {
        ProjectMember admin = projectMemberRepository.save(
                new ProjectMember(projectA, userC, ProjectRole.ADMIN)
        );

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        admin.getId()
                )
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        assertTrue(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userC.getId()
                )
        );
    }
    @Test
    @DisplayName("Should prevent MEMBER from removing members")
    void test_23() throws Exception {
        ProjectMember regularMember = projectMemberRepository.save(
                new ProjectMember(projectA, userC, ProjectRole.MEMBER)
        );

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        regularMember.getId()
                )
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        assertTrue(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userC.getId()
                )
        );
    }
    @Test
    @DisplayName("Should return 404 when non-member tries to remove a project member")
    void test_24() throws Exception {
        ProjectMember regularMember = projectMemberRepository.save(
                new ProjectMember(projectA, userC, ProjectRole.MEMBER)
        );

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        regularMember.getId()
                )
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectA.getId(),
                        userC.getId()
                )
        );
    }
    @Test
    @DisplayName("Should return 404 when member belongs to another project")
    void test_25() throws Exception {
        Project projectB = projectService.create(
                "Private Project B",
                "Project owned by user B",
                userB.getEmail()
        );

        ProjectMember memberB = projectMemberRepository.findByProjectIdAndUserId(
                projectB.getId(),
                userB.getId()
        ).orElseThrow();

        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        memberB.getId()
                )
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertTrue(
                projectMemberRepository.existsByProjectIdAndUserId(
                        projectB.getId(),
                        userB.getId()
                )
        );
    }
    @Test
    @DisplayName("Should return 401 when removing member without authentication")
    void test_26() throws Exception {
        mockMvc.perform(delete(
                        "/api/projects/{projectId}/members/{memberId}",
                        projectA.getId(),
                        1L
                ))
                .andExpect(status().isUnauthorized());
    }
}
