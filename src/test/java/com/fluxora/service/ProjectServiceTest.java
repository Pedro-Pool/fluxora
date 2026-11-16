package com.fluxora.service;

import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.ProjectRepository;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        List<Project> projects = List.of(project);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findAllByUserId(USER_ID)).thenReturn(projects);

        List<Project> result = projectService.findAll(email);

        assertEquals(1, result.size());
        assertSame(project, result.getFirst());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findAllByUserId(USER_ID);

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return project when it belonging to the user")
    void test_02(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.of(project));

        Project result = projectService.findById(PROJECT_ID, email);

        assertSame(project, result);

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return 401 when user does not exist")
    void test_03(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.findAll(email)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);

        verifyNoInteractions(projectMemberRepository, projectRepository);
    }
    @Test
    @DisplayName("Should return 404 when project does not exist for the user")
    void test_04(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.findById(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);

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
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        Project result = projectService.update(
                PROJECT_ID,
                "Update project",
                "Update description",
                email
        );

        assertSame(project, result);
        assertEquals("Update project", result.getName());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should delete a project successfully")
    void test_08(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(project));

        projectService.delete(PROJECT_ID, email);

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);
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

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

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
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);
        verify(projectRepository, never()).delete(any(Project.class));

        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should return 401 when deleting a non-existent project")
    void test_13(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectRepository).findByIdAndUserId(PROJECT_ID, USER_ID);
        verify(projectRepository, never()).delete(any(Project.class));

        verifyNoInteractions(projectMemberRepository);
    }
}
