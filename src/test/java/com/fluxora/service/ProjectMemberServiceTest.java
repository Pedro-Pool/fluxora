package com.fluxora.service;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectPermissionValidator projectPermissionValidator;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private User user;
    private Project project;
    private ProjectMember member;

    private final String email = "test@fluxora.com";

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;

    @BeforeEach
    void setUp(){
        user = new User("User test", email, "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        project = new Project("Project test", "project description");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.assignTo(user);

        member = new ProjectMember(project, user, ProjectRole.OWNER);
    }
    private void mockAuthenticatedUser(){
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }
    @Test
    @DisplayName("Should find all members of a project")
    void test_01(){
        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);

        when(projectMemberRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(member));

        List<ProjectMember> members = projectMemberService.findMembers(PROJECT_ID, email);

        assertEquals(1, members.size());
        assertSame(member, members.get(0));
        assertEquals(ProjectRole.OWNER, members.get(0).getRole());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectMemberRepository).findByProjectId(PROJECT_ID);

        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should throw 404 when user is not a project member")
    void test_02(){
        when(projectPermissionValidator.validateMembership(PROJECT_ID, email))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.findMembers(PROJECT_ID, email)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);

        verify(projectMemberRepository, never()).findByProjectId(anyLong());
        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should throw 401 when authenticated user is not found")
    void test_03(){
        when(projectPermissionValidator.validateMembership(PROJECT_ID, email))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.findMembers(PROJECT_ID, email)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);

        verifyNoInteractions(projectMemberRepository);
        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should return all projects members")
    void test_04(){
        User anotherUser = new User(
                "another user",
                "another@fluxora.com",
                "password-hash"
        );
        ReflectionTestUtils.setField(anotherUser, "id", 2L);

        ProjectMember anotherMember = new ProjectMember(
                project, anotherUser, ProjectRole.MEMBER
        );

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);
        when(projectMemberRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(member, anotherMember));

        List<ProjectMember> members = projectMemberService.findMembers(PROJECT_ID, email);

        assertEquals(2, members.size());
        assertSame(member, members.get(0));
        assertSame(anotherMember, members.get(1));

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectMemberRepository).findByProjectId(PROJECT_ID);

        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should allow owner to add a member to the project")
    void test_05(){
        String invitedEmail = "invited@fluxora.com";

        User invitedUser = new User(
                "Invited user",
                invitedEmail,
                "password-hash"
        );
        ReflectionTestUtils.setField(invitedUser, "id", 2L);

        when(userRepository.findByEmail(invitedEmail)).thenReturn(Optional.of(invitedUser));

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);

        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMember savedMember = projectMemberService.addMember(
                PROJECT_ID,
                invitedEmail,
                ProjectRole.MEMBER,
                email
        );

        assertNotNull(savedMember);
        assertSame(project, savedMember.getProject());
        assertSame(invitedUser, savedMember.getUser());
        assertEquals(ProjectRole.MEMBER, savedMember.getRole());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(member, ProjectRole.MEMBER);

        verify(userRepository).findByEmail(invitedEmail);
        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should allow admin to add a member to the project")
    void test_06(){
        String invitedEmail = "invited@fluxora.com";

        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        User invitedUser = new User(
                "Invited user",
                invitedEmail,
                "password-hash"
        );
        ReflectionTestUtils.setField(invitedUser, "id", 2L);

        when(userRepository.findByEmail(invitedEmail)).thenReturn(Optional.of(invitedUser));

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(admin);

        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMember savedMember = projectMemberService.addMember(
                PROJECT_ID,
                invitedEmail,
                ProjectRole.MEMBER,
                email
        );

        assertSame(invitedUser, savedMember.getUser());
        assertEquals(ProjectRole.MEMBER, savedMember.getRole());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(admin, ProjectRole.MEMBER);

        verify(userRepository).findByEmail(invitedEmail);
        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should not allow admin to add another admin")
    void test_07(){
        String invitedEmail = "invited@fluxora.com";

        ProjectMember admin = new ProjectMember(
                project,
                user,
                ProjectRole.ADMIN
        );

        User invitedUser = new User(
                "Invited user",
                invitedEmail,
                "password-hash"
        );
        ReflectionTestUtils.setField(invitedUser, "id", 2L);

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(admin);

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Administrators can only add members"
        )).when(projectPermissionValidator).validateCanAddMember(admin, ProjectRole.ADMIN);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        invitedEmail,
                        ProjectRole.ADMIN,
                        email
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(admin, ProjectRole.ADMIN);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));

        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should not allow member to add another user")
    void test_08(){
        ProjectMember regularMember = new ProjectMember(
                project,
                user,
                ProjectRole.MEMBER
        );

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(regularMember);

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "User does not have permission to add members"
        )).when(projectPermissionValidator).validateCanAddMember(regularMember, ProjectRole.MEMBER);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        "invited@fluxora.com",
                        ProjectRole.MEMBER,
                        email
                )
        );;

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(regularMember, ProjectRole.MEMBER);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));

        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should not allow owner role assigment through invitation")
    void test_09(){
        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);

        doThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid member role"
        )).when(projectPermissionValidator).validateCanAddMember(member, ProjectRole.OWNER);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        "invited@fluxora.com",
                        ProjectRole.OWNER,
                        email
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(member, ProjectRole.OWNER);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));

        verifyNoInteractions(userRepository);
    }
    @Test
    @DisplayName("Should not allow adding an existing project member")
    void test_10(){
        String invitedEmail = "invited@fluxora.com";

        User invitedUser = new User(
                "Invited user",
                invitedEmail,
                "password-hash"
        );
        ReflectionTestUtils.setField(invitedUser, "id", 2L);

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);
        when(userRepository.findByEmail(invitedEmail)).thenReturn(Optional.of(invitedUser));

        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        invitedEmail,
                        ProjectRole.MEMBER,
                        email
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(member, ProjectRole.MEMBER);

        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));

        verify(userRepository).findByEmail(invitedEmail);
    }
    @Test
    @DisplayName("Should return 404 when invited user does not exist")
    void test_11(){
        String unknownEmail = "unknow@fluxora.com";

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        unknownEmail,
                        ProjectRole.MEMBER,
                        email
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findByEmail(unknownEmail);
        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(member, ProjectRole.MEMBER);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should return 404 when requester is not a project member")
    void test_12(){
        doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User is not a member of this project"
        )).when(projectPermissionValidator).validateMembership(PROJECT_ID, email);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        "invited@fluxora.com",
                        ProjectRole.MEMBER,
                        email
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(userRepository);
        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator, never()).validateCanAddMember(
                any(ProjectMember.class), any(ProjectRole.class));

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should return 404 when requester does not exist")
    void test_13(){
        String unknownEmail = "unknow@fluxora.com";

        doThrow(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user not found"
        )).when(projectPermissionValidator).validateMembership(PROJECT_ID, unknownEmail);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        "invited@fluxora.com",
                        ProjectRole.MEMBER,
                        unknownEmail
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, unknownEmail);
        verify(projectPermissionValidator, never()).validateCanAddMember(
                any(ProjectMember.class), any(ProjectRole.class));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(projectMemberRepository);
    }
    @Test
    @DisplayName("Should allow owner to add an admin to the project")
    void test_14(){
        String invitedEmail = "invited@fluxora.com";

        User invitedUser = new User(
                "Invited user",
                invitedEmail,
                "password-hash"
        );
        ReflectionTestUtils.setField(invitedUser, "id", 2L);

        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);
        when(userRepository.findByEmail(invitedEmail)).thenReturn(Optional.of(invitedUser));

        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMember savedMember = projectMemberService.addMember(
                PROJECT_ID,
                invitedEmail,
                ProjectRole.ADMIN,
                email
        );
        assertNotNull(savedMember);
        assertSame(invitedUser, savedMember.getUser());
        assertSame(invitedUser, savedMember.getUser());
        assertEquals(ProjectRole.ADMIN, savedMember.getRole());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);
        verify(projectPermissionValidator).validateCanAddMember(member, ProjectRole.ADMIN);

        verify(userRepository).findByEmail(invitedEmail);

        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }
    @Test
    @DisplayName("Should return 404 when requester does not exist")
    void test_15(){
        when(projectPermissionValidator.validateMembership(PROJECT_ID, email)).thenReturn(member);

        doThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid member role"
        )).when(projectPermissionValidator).validateCanAddMember(member, null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectMemberService.addMember(
                        PROJECT_ID,
                        "invited@fluxora.com",
                        null,
                        email
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(projectPermissionValidator).validateMembership(PROJECT_ID, email);

        verify(projectPermissionValidator).validateCanAddMember(member, null);

        verifyNoInteractions(userRepository);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
}
