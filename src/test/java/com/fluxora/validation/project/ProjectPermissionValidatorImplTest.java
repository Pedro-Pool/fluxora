package com.fluxora.validation.project;

import com.fluxora.entity.Project;
import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;
import com.fluxora.entity.User;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionValidatorImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectPermissionValidatorImpl permissionValidator;

    private User user;
    private Project project;
    private ProjectMember owner;

    private final String email = "test@fluxora.com";

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;

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

        owner = new ProjectMember(
                project,
                user,
                ProjectRole.OWNER
        );
    }
    @Test
    @DisplayName("Should return project membership when user belongs to project")
    void test_01(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(owner));

        ProjectMember result = permissionValidator.validateMembership(PROJECT_ID, email);

        assertSame(owner, result);

        verify(userRepository).findByEmail(email);
        verify(projectMemberRepository).findByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }
    @Test
    @DisplayName("Should throw 401 when authenticated user does not exist")
    void test_02(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateMembership(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should throw 404 when user is not a project member")
    void test_03() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateMembership(PROJECT_ID, email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(email);
        verify(projectMemberRepository).findByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }
    @Test
    @DisplayName("Should allow owner to perform owner-only actions")
    void test_04() {
        assertDoesNotThrow(() -> permissionValidator.validateOwner(owner));
    }
    @Test
    @DisplayName("Should deny admin from performing owner-only actions")
    void test_05() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateOwner(admin)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should deny member from performing owner-only actions")
    void test_06() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateOwner(member)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should allow owner to add a member")
    void test_07(){
        permissionValidator.validateCanAddMember(owner, ProjectRole.MEMBER);
    }
    @Test
    @DisplayName("Should allow owner to add a admin")
    void test_08(){
        permissionValidator.validateCanAddMember(owner, ProjectRole.ADMIN);
    }
    @Test
    @DisplayName("Should return 400 when owner tries to add another owner")
    void test_09() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(owner, ProjectRole.OWNER)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 400 when owner tries to add a member with null role")
    void test_10() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(owner, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should allow admin to add a member")
    void test_11() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        permissionValidator.validateCanAddMember(admin, ProjectRole.MEMBER);
    }
    @Test
    @DisplayName("Should return 403 when admin tries to add another admin")
    void test_12() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(admin, ProjectRole.ADMIN)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 400 when admin tries to add an owner")
    void test_13() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(admin, ProjectRole.OWNER)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 400 when admin tries to add a member with null role")
    void test_14() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(admin, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 403 when member tries to add another member")
    void test_15() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(member, ProjectRole.MEMBER)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 403 when member tries to add an admin")
    void test_16() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanAddMember(member, ProjectRole.ADMIN)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should allow owner to update the project")
    void test_17() {
        assertDoesNotThrow(
                () -> permissionValidator.validateCanUpdateProject(owner)
        );
    }
    @Test
    @DisplayName("Should allow admin to update the project")
    void test_18() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        assertDoesNotThrow(
                () -> permissionValidator.validateCanUpdateProject(admin)
        );
    }
    @Test
    @DisplayName("Should return 403 when member tries to update the project")
    void test_19() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanUpdateProject(member)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should allow owner to delete the project")
    void test_20() {
        assertDoesNotThrow(
                () -> permissionValidator.validateCanDeleteProject(owner)
        );
    }
    @Test
    @DisplayName("Should return 403 when admin tries to delete the project")
    void test_21() {
        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanDeleteProject(admin)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should return 403 when member tries to delete the project")
    void test_22() {
        ProjectMember member = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> permissionValidator.validateCanDeleteProject(member)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
