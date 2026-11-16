package com.fluxora.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    private Project project;
    private User user;
    private ProjectMember member;

    @BeforeEach
    void setUp(){
        user = new User(
                "Test user",
                "test@fluxora.com",
                "password-hash"
        );
        project = new Project(
                "Test project",
                "Test description"
        );
        project.assignTo(user);

        member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );
    }

    @Test
    @DisplayName("Should add member to the project and synchronize both sides")
    void test_01(){
        project.addMember(member);

        assertEquals(1, project.getMembers().size());
        assertSame(project, member.getProject());
        assertTrue(project.getMembers().contains(member));
    }
    @Test
    @DisplayName("Should not add the same member twice")
    void test_02(){
        project.addMember(member);
        project.addMember(member);

        assertEquals(1, project.getMembers().size());
        assertTrue(project.getMembers().contains(member));
    }
    @Test
    @DisplayName("Should not add the same member twice")
    void test_03(){
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> project.addMember(null)
        );

        assertEquals("Project member cannot be null", exception.getMessage());
        assertTrue(project.getMembers().isEmpty());
    }

}
