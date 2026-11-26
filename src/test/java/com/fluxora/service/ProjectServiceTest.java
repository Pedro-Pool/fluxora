package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.enums.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.UserRepository;
import com.fluxora.validation.project.ProjectPermissionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectPermissionValidator permissionValidator;

    @InjectMocks
    private ProjectService projectService;

    private User user;
    private Project project;

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;

    private final String email = "test@fluxora.com";

    @BeforeEach
    void setUp(){
        user = new User(
                "test user",
                email,
                "password-hash"
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);

        project = new Project(
                "test project",
                "project description"
        );
        project.assignTo(user);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
    }
    @Test
    @DisplayName("Should return all projects belonging to the user")
    void test_01(){
        Page<Project> projects = new PageImpl<>(List.of(project));
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findDistinctByMembers_User_Id(USER_ID, pageable)).thenReturn(projects);

        Page<Project> result = projectService.findAll(email, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(project, result.getContent().getFirst());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findDistinctByMembers_User_Id(USER_ID, pageable);

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return project when it belonging to the user")
    void test_02(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID)).thenReturn(Optional.of(project));

        Project result = projectService.findById(PROJECT_ID, email);

        assertSame(project, result);

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return 401 when user does not exist")
    void test_03(){
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.findAll(email, pageable)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);

        verifyNoInteractions(projectMemberRepository, projectRepository);
    }
    @Test
    @DisplayName("Should return 404 when project does not exist for the user")
    void test_04(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.findById(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should create a project successfully")
    void test_05(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.create(
                "New project",
                "New project description",
                email
        );

        assertNotNull(result);
        assertEquals("New project", result.getName());
        assertSame(user, result.getUser());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).save(any(Project.class));

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should register the project owner as a project member")
    void test_06(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.create(
                "New project",
                "New project description",
                email
        );

        ArgumentCaptor<ProjectMember> memberCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);

        verify(projectMemberRepository).save(memberCaptor.capture());

        ProjectMember savedMember = memberCaptor.getValue();

        assertSame(result, savedMember.getProject());
        assertSame(user, savedMember.getUser());
        assertEquals(ProjectRole.OWNER, savedMember.getRole());
    }
    @Test
    @DisplayName("Should update a project successfully")
    void test_07(){
        ProjectMember owner = new ProjectMember(
                project,
                user,
                ProjectRole.OWNER
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(owner));

        doNothing().when(permissionValidator).validateCanUpdateProject(owner);

        Project result = projectService.update(
                PROJECT_ID,
                "Update project",
                "Update description",
                email
        );

        assertSame(project, result);
        assertEquals("Update project", result.getName());
        assertEquals("Update description", result.getDescription());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);
        verify(projectMemberRepository).findByProjectIdAndUserId(PROJECT_ID, USER_ID);
        verify(permissionValidator).validateCanUpdateProject(owner);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should delete a project successfully")
    void test_08(){
        ProjectMember owner = new ProjectMember(
                project,
                user,
                ProjectRole.OWNER
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(owner));

        projectService.delete(PROJECT_ID, email);

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);
        verify(projectMemberRepository).findByProjectIdAndUserId(PROJECT_ID, USER_ID);
        verify(permissionValidator).validateCanDeleteProject(owner);
        verify(projectRepository).delete(project);

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should return 401 when creating a project with a non-existent user")
    void test_09(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.create(
                        "New project",
                        "New project description",
                        email
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(projectMemberRepository, projectRepository);
    }
    @Test
    @DisplayName("Should return 401 when updating a project with a non-existent user")
    void test_10(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.update(
                        PROJECT_ID,
                        "Update project",
                        "Update description",
                        email
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(projectMemberRepository, projectRepository);
    }
    @Test
    @DisplayName("Should throw 401 when deleting a project with a non-existent user")
    void test_11(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete(PROJECT_ID, email));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(projectMemberRepository, projectRepository);
    }
    @Test
    @DisplayName("Should return 401 when updating a non-existent project")
    void test_12(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.update(
                        PROJECT_ID,
                        "Update project",
                        "Update description",
                        email
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);
        verify(projectRepository, never()).delete(any(Project.class));

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return 401 when deleting a non-existent project")
    void test_13(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);
        verify(projectRepository, never()).delete(any(Project.class));

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should find projects where user is a member but not the owner")
    void test_14() {
        User owner = new User(
                "Project Owner",
                "owner@fluxora.com",
                "password-hash"
        );

        Project sharedProject = new Project(
                "Shared Project",
                "Shared description"
        );

        sharedProject.assignTo(owner);

        ProjectMember membership = new ProjectMember(
                sharedProject,
                user,
                ProjectRole.MEMBER
        );

        sharedProject.addMember(membership);

        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findDistinctByMembers_User_Id(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(sharedProject)));

        Page<Project> result = projectService.findAll(email, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(sharedProject, result.getContent().getFirst());
        assertNotSame(user, sharedProject.getUser());
        assertEquals(ProjectRole.MEMBER, membership.getRole());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findDistinctByMembers_User_Id(USER_ID, pageable);
    }
    @Test
    @DisplayName("Should find a project when user is a member but not the owner")
    void test_15() {
        User owner = new User(
                "Project Owner",
                "owner@fluxora.com",
                "password-hash"
        );

        Project sharedProject = new Project(
                "Shared Project",
                "Shared description"
        );

        sharedProject.assignTo(owner);

        ProjectMember membership = new ProjectMember(
                sharedProject,
                user,
                ProjectRole.MEMBER
        );

        sharedProject.addMember(membership);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(sharedProject));

        Project result = projectService.findById(PROJECT_ID, email);

        assertNotNull(result);
        assertSame(sharedProject, result);
        assertNotSame(user, result.getUser());
        assertSame(user, membership.getUser());
        assertEquals(ProjectRole.MEMBER, membership.getRole());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndMembers_User_Id(PROJECT_ID, USER_ID);
    }
    @Test
    @DisplayName("Should allow admin to update a project")
    void test_16() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(admin));

        Project result = projectService.update(
                PROJECT_ID,
                "Updated by admin",
                "Updated description",
                email
        );

        assertSame(project, result);
        assertEquals("Updated by admin", result.getName());
        assertEquals("Updated description", result.getDescription());

        verify(permissionValidator).validateCanUpdateProject(admin);
    }
    @Test
    @DisplayName("Should return 403 when member tries to update a project")
    void test_17() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        String originalName = project.getName();
        String originalDescription = project.getDescription();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(member));

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to update this project"
        )).when(permissionValidator).validateCanUpdateProject(member);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.update(
                        PROJECT_ID,
                        "Updated by member",
                        "Updated description",
                        email
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(originalName, project.getName());
        assertEquals(originalDescription, project.getDescription());

        verify(permissionValidator).validateCanUpdateProject(member);
    }
    @Test
    @DisplayName("Should return 403 when admin tries to delete a project")
    void test_18() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(admin));

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to delete this project"
        )).when(permissionValidator).validateCanDeleteProject(admin);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(permissionValidator).validateCanDeleteProject(admin);
        verify(projectRepository, never()).delete(any(Project.class));
    }
    @Test
    @DisplayName("Should return 403 when member tries to delete a project")
    void test_19() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndMembers_User_Id(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(member));

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to delete this project"
        )).when(permissionValidator).validateCanDeleteProject(member);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(permissionValidator).validateCanDeleteProject(member);
        verify(projectRepository, never()).delete(any(Project.class));
    }
}
