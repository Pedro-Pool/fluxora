package com.fluxora.repository;


import com.fluxora.config.AbstractIntegrationTest;
import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProjectMemberRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ProjectMemberRepository projectMemberRepository;

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
                "AnotherUser",
                "anotherUser@example.com",
                "$2a$10$abcdefghijklmnopqrstuu123456789012345678901234567890"
        );
        entityManager.persist(owner);
        entityManager.persist(anotherUser);

        project = new Project("Owner project", "description test");
        project.assignTo(owner);

        anotherProject = new Project("Another project", "description another test");
        anotherProject.assignTo(anotherUser);

        entityManager.persist(project);
        entityManager.persist(anotherProject);

        entityManager.flush();
    }
    @Test
    @DisplayName("Should save a project member successfully")
    void test_01(){
        ProjectMember member = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );

        ProjectMember savedMember = projectMemberRepository.save(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getProject().getId()).isEqualTo(project.getId());
        assertThat(savedMember.getUser().getId()).isEqualTo(anotherUser.getId());
        assertThat(savedMember.getRole()).isEqualTo(ProjectRole.MEMBER);
        assertThat(savedMember.getJoinedAt()).isNotNull();
    }
    @Test
    @DisplayName("Should find all members belonging to a project")
    void test_02(){
        ProjectMember member1 = new ProjectMember(
                project,
                owner,
                ProjectRole.OWNER
        );
        ProjectMember member2 = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );
        ProjectMember anotherProjectMember = new ProjectMember(
                anotherProject,
                anotherUser,
                ProjectRole.OWNER
        );

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(anotherProjectMember);

        entityManager.flush();

        List<ProjectMember> result = projectMemberRepository.findByProjectId(project.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(ProjectMember::getUser)
                .extracting(User::getId)
                .containsExactlyInAnyOrder(owner.getId(), anotherUser.getId());
    }
    @Test
    @DisplayName("Should find a member by project and user")
    void test_03(){
        ProjectMember member = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );

        entityManager.persist(member);
        entityManager.flush();

        var result = projectMemberRepository.findByProjectIdAndUserId(project.getId(), anotherUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getProject().getId()).isEqualTo(project.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(anotherUser.getId());
        assertThat(result.get().getRole()).isEqualTo(ProjectRole.MEMBER);

    }
    @Test
    @DisplayName("Should return empty when member does not belonging to project")
    void test_04(){
        var result = projectMemberRepository.findByProjectIdAndUserId(project.getId(), anotherUser.getId());

        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("Should verify if a user belongs to a project")
    void test_05(){
        ProjectMember member = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );

        entityManager.persist(member);
        entityManager.flush();

        boolean exists = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), anotherUser.getId());

        boolean notExists = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), owner.getId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    @Test
    @DisplayName("Should not allow duplicate members in the same project")
    void test_06(){
        ProjectMember member1 = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );
        ProjectMember member2 = new ProjectMember(
                project,
                anotherUser,
                ProjectRole.MEMBER
        );

        projectMemberRepository.save(member1);

        assertThatThrownBy(() ->
                projectMemberRepository.saveAndFlush(member2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
