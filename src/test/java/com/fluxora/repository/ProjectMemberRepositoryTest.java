package com.fluxora.repository;

import com.fluxora.config.AbstractIntegrationTest;
import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProjectMemberRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User(
                "Owner", "owner@test.com", "password"
        ));

        member = userRepository.save(new User(
                "Member", "member@test.com", "password"
        ));

        project = new Project("Project", "Description");
        project.assignTo(owner);
        project = projectRepository.save(project);

        projectMemberRepository.save(
                new ProjectMember(project, owner, ProjectRole.OWNER)
        );

        projectMemberRepository.save(
                new ProjectMember(project, member, ProjectRole.MEMBER)
        );
    }

    @Test
    @DisplayName("Should save a project member")
    void test_01() {
        User anotherUser = userRepository.save(new User(
                "Another", "another@test.com", "password"
        ));

        ProjectMember saved = projectMemberRepository.save(
                new ProjectMember(project, anotherUser, ProjectRole.MEMBER)
        );

        assertNotNull(saved.getId());
        assertEquals(ProjectRole.MEMBER, saved.getRole());
    }
    @Test
    @DisplayName("Should find members by project ID using pagination")
    void test_02() {
        Page<ProjectMember> result = projectMemberRepository
                .findByProjectId(project.getId(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }
    @Test
    @DisplayName("Should find all members by project ID without pagination")
    void test_03() {
        List<ProjectMember> result =
                projectMemberRepository.findByProjectId(project.getId());

        assertEquals(2, result.size());
    }
    @Test
    @DisplayName("Should find member by project ID and user ID")
    void test_04() {
        var result = projectMemberRepository.findByProjectIdAndUserId(
                project.getId(), member.getId()
        );

        assertTrue(result.isPresent());
        assertEquals(ProjectRole.MEMBER, result.get().getRole());
    }
    @Test
    @DisplayName("Should verify whether user belongs to project")
    void test_05() {
        assertTrue(projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(), member.getId()
        ));

        User anotherUser = userRepository.save(new User(
                "Another", "another@test.com", "password"
        ));

        assertFalse(projectMemberRepository.existsByProjectIdAndUserId(
                project.getId(), anotherUser.getId()
        ));
    }
    @Test
    @DisplayName("Should return empty page when project has no members")
    void test_06() {
        User anotherOwner = userRepository.save(new User(
                "Another Owner", "anotherowner@test.com", "password"
        ));

        Project anotherProject = new Project(
                "Empty Project", "Description"
        );
        anotherProject.assignTo(anotherOwner);
        anotherProject = projectRepository.save(anotherProject);

        Page<ProjectMember> result = projectMemberRepository
                .findByProjectId(
                        anotherProject.getId(),
                        PageRequest.of(0, 10)
                );

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}
