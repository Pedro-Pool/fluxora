package com.fluxora.repository;

import com.fluxora.config.AbstractIntegrationTest;
import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.enums.ProjectRole;
import com.fluxora.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProjectRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        member = userRepository.save(new User(
                "Member",
                "member@test.com",
                "password"
        ));

        owner = userRepository.save(new User(
                "Owner",
                "owner@test.com",
                "password"
        ));

        project = new Project("Project", "Description");
        project.assignTo(owner);
        project = projectRepository.save(project);

        ProjectMember ownerMembership = new ProjectMember(
                project, owner, ProjectRole.OWNER
        );

        ProjectMember memberMembership = new ProjectMember(
                project, member, ProjectRole.MEMBER
        );

        projectMemberRepository.save(ownerMembership);
        projectMemberRepository.save(memberMembership);
    }

    @Test
    @DisplayName("Should find projects by member user ID using pagination")
    void test_01() {
        Page<Project> result = projectRepository
                .findDistinctByMembers_User_Id(
                        member.getId(),
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(project.getId(), result.getContent().getFirst().getId());
    }
    @Test
    @DisplayName("Should find projects where user is a member but not the owner")
    void test_02() {
        Page<Project> result = projectRepository
                .findDistinctByMembers_User_Id(
                        member.getId(),
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertNotEquals(
                member.getId(),
                result.getContent().getFirst().getUser().getId()
        );
    }
    @Test
    @DisplayName("Should return empty page when user has no projects")
    void test_03() {
        User anotherUser = userRepository.save(new User(
                "Another User",
                "another@test.com",
                "password"
        ));

        Page<Project> result = projectRepository
                .findDistinctByMembers_User_Id(
                        anotherUser.getId(),
                        PageRequest.of(0, 10)
                );

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}
