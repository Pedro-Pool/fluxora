package com.fluxora.service;

import com.fluxora.dto.TaskStatisticsResponse;
import com.fluxora.entity.*;
import com.fluxora.enums.ProjectRole;
import com.fluxora.enums.TaskHistoryAction;
import com.fluxora.enums.TaskPriority;
import com.fluxora.enums.TaskStatus;
import com.fluxora.repository.ProjectMemberRepository;
import com.fluxora.repository.TaskHistoryRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.UserRepository;
import jakarta.persistence.EntityManager;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Project project;
    private Pageable pageable;

    private final String EMAIL = "test@fluxora.com";
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;


    @BeforeEach
    void setUp(){
        user = new User("User test", EMAIL, "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        pageable = PageRequest.of(0, 10);

        project = new Project("Project test", "project description");
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        project.assignTo(user);
    }
    private void mockAuthenticatedUser(){
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private void mockProjectMembership(){
        ProjectMember membership = new ProjectMember(project, user, ProjectRole.OWNER);

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(membership));
    }


    @Test
    @DisplayName("It must create a task using the provided data")
    void test_01(){
        mockAuthenticatedUser();
        mockProjectMembership();

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL);

        assertEquals("Study java", createdTask.getTitle());
        assertFalse(createdTask.isCompleted());
        assertSame(project, createdTask.getProject());

        verify(projectMemberRepository).findByProjectIdAndUserId(PROJECT_ID, USER_ID);
        verify(taskRepository).save(any(Task.class));
    }
    @Test
    @DisplayName("Should find all tasks for user")
    void test_02(){
        mockAuthenticatedUser();
        mockProjectMembership();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findAllWithFiltersByProjectMember(
                PROJECT_ID,
                user.getId(),
                null,
                null,
                null,
                null,
                null,
                pageable
                ))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                EMAIL,
                null,
                null,
                null,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Study java", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                null,
                null,
                null,
                pageable);
    }
    @Test
    @DisplayName("should find a task by ID for the user")
    void test_03(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        Task foundTask = taskService.findById(1L, EMAIL);

        assertSame(task, foundTask);

        verify(taskRepository).findByIdAndProjectMemberUserId(1L, USER_ID);
    }
    @Test
    @DisplayName("It should throw an exception when the task does not exist for the user.")
    void test_04(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, EMAIL));
    }
    @Test
    @DisplayName("must complete the task")
    void test_05(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);
        task.assignTo(project);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L, EMAIL);

        assertTrue(completedTask.isCompleted());
    }
    @Test
    @DisplayName("should delete the task")
    void test_06(){
        mockAuthenticatedUser();

        Task task = new Task("Study java", null);
        task.assignTo(project);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(task));

        taskService.delete(1L, EMAIL);

        verify(taskRepository).delete(task);
    }
    @Test
    @DisplayName("Should not allow a user to access another user's task")
    void test_07(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.findById(99L, EMAIL));

        verify(taskRepository).findByIdAndProjectMemberUserId(99L, USER_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }
    @Test
    @DisplayName("Should not allow updating another user's task")
    void test_08(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() ->
                taskService.update(
                        99L,
                        "New Title",
                        "New Description",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        null,
                        null,
                        EMAIL));

        verify(taskRepository).findByIdAndProjectMemberUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow completing another user's task")
    void test_09(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.complete(99L, EMAIL));

        verify(taskRepository).findByIdAndProjectMemberUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow reopening another user's task")
    void test_10(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.reopen(99L, EMAIL));

        verify(taskRepository).findByIdAndProjectMemberUserId(99L, USER_ID);
    }
    @Test
    @DisplayName("Should not allow deleting another user's task")
    void test_11(){
        mockAuthenticatedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,() -> taskService.delete(99L, EMAIL));

        verify(taskRepository).findByIdAndProjectMemberUserId(99L, USER_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }
    @Test
    @DisplayName("Should create a task with default planning values")
    void test_12(){
        mockAuthenticatedUser();
        mockProjectMembership();

        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                null,
                null,
                null,
                null,
                EMAIL);

        assertEquals(TaskStatus.TODO, createdTask.getStatus());
        assertEquals(TaskPriority.MEDIUM, createdTask.getPriority());
        assertNull(createdTask.getDueDate());
        assertNull(createdTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should create a completed task with completedAt")
    void test_13(){
        mockAuthenticatedUser();
        mockProjectMembership();

        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study java",
                "practice tests",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                LocalDate.of(2026, 10, 30),
                null,
                EMAIL);

        assertEquals(TaskStatus.DONE, createdTask.getStatus());
        assertEquals(TaskPriority.HIGH, createdTask.getPriority());
        assertEquals(LocalDate.of(2026, 10, 30), createdTask.getDueDate());
        assertNotNull(createdTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should not find task when the project does not belong to the user")
    void test_14(){
        mockAuthenticatedUser();

        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,() -> taskService.findAll(
                PROJECT_ID,
                EMAIL,
                null,
                null,
                null,
                null,
                null,
                pageable));

        verify(taskRepository, never()).findAllWithFiltersByProjectMember(
                anyLong(),
                anyLong(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }
    @Test
    @DisplayName("Should throws unauthorized when the authenticated user does not exist")
    void test_15(){
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,() -> taskService.findById(1L, EMAIL));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
    @Test
    @DisplayName("Should reopen a task and clear completedAt")
    void test_16(){
        Project taskProject = project;

        Task existingTask = new Task("Completed task", "description");
        existingTask.assignTo(taskProject);
        existingTask.completed();

        assertNotNull(existingTask.getCompletedAt());

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task taskReopen = taskService.reopen(1L, EMAIL);

        assertEquals(TaskStatus.TODO, taskReopen.getStatus());
        assertNull(taskReopen.getCompletedAt());
    }
    @Test
    @DisplayName("Should cancel a task and clear completedAt")
    void test_17(){
        Task existingTask = new Task("Completed task", "description");
        existingTask.assignTo(project);
        existingTask.completed();

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task canceledTask = taskService.cancel(1L, EMAIL);

        assertEquals(TaskStatus.CANCELED, canceledTask.getStatus());
        assertNull(canceledTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should update a task to DONE and set completedAt")
    void test_18(){
        Task existingTask = new Task("Old title", "Old description");
        existingTask.assignTo(project);

        mockAuthenticatedUser();
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        Task updatedTask = taskService.update(
                1L,
                "New title",
                "New description",
                TaskStatus.DONE,
                TaskPriority.URGENT,
                LocalDate.of(2026, 10, 30),
                null,
                EMAIL
        );
        assertEquals("New title", updatedTask.getTitle());
        assertEquals("New description", updatedTask.getDescription());
        assertEquals(TaskStatus.DONE, updatedTask.getStatus());
        assertEquals(TaskPriority.URGENT, updatedTask.getPriority());
        assertEquals(LocalDate.of(2026, 10, 30), updatedTask.getDueDate());
        assertNotNull(updatedTask.getCompletedAt());
    }
    @Test
    @DisplayName("Should filter tasks by status")
    void test_19(){
        mockAuthenticatedUser();
        mockProjectMembership();

        Task task = new Task("Task in progress", null);

        when(taskRepository.findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                EMAIL,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Task in progress", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should filter tasks by priority")
    void test_20(){
        mockAuthenticatedUser();
        mockProjectMembership();

        Task task = new Task("High Task", null);

        when(taskRepository.findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                null,
                TaskPriority.HIGH,
                null,
                null,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                EMAIL,
                null,
                TaskPriority.HIGH,
                null,
                null,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("High Task", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                null,
                TaskPriority.HIGH,
                null,
                null,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should filter tasks by due date range")
    void test_21(){
        mockAuthenticatedUser();
        mockProjectMembership();

        Task task = new Task("October Task", null);

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        when(taskRepository.findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                from,
                to,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                EMAIL,
                null,
                null,
                from,
                to,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());

        verify(taskRepository).findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                null,
                null,
                from,
                to,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should combine status, priority and due date filters")
    void test_22(){
        mockAuthenticatedUser();
        mockProjectMembership();

        Task task = new Task("Urgent Task", null);

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        when(taskRepository.findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                null,
                pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<Task> tasks = taskService.findAll(
                PROJECT_ID,
                EMAIL,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                null,
                pageable);

        assertEquals(1, tasks.getTotalElements());

        verify(taskRepository).findAllWithFiltersByProjectMember(
                PROJECT_ID,
                USER_ID,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                null,
                pageable
        );
    }
    @Test
    @DisplayName("Should reject an invalid due date range")
    void test_23(){
        mockAuthenticatedUser();
        mockProjectMembership();

        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> taskService.findAll(
                        PROJECT_ID,
                        EMAIL,
                        null,
                        null,
                        from,
                        to,
                        null,
                        pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(taskRepository, never())
                .findAllWithFiltersByProjectMember(any(), any(), any(), any(), any(), any(), any(), any());
    }
    @Test
    @DisplayName("Should return overdue tasks")
    void test_24(){
        mockAuthenticatedUser();

        LocalDate today = LocalDate.now();
        Task task = new Task("Overdue task", null);

        Page<Task> expected = new PageImpl<>(
                List.of(task), pageable, 1
        );
        when(taskRepository.findOverdueTasksByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        )).thenReturn(expected);

        Page<Task> tasks = taskService.findOverdueTasks(EMAIL, pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Overdue task", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findOverdueTasksByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    @Test
    @DisplayName("Should return an empty page when there are no overdue tasks")
    void test_25(){
        mockAuthenticatedUser();

        LocalDate today = LocalDate.now();

        when(taskRepository.findOverdueTasksByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        )).thenReturn(Page.empty());

        Page<Task> tasks = taskService.findOverdueTasks(EMAIL, pageable);

        assertEquals(0, tasks.getTotalElements());

        verify(taskRepository).findOverdueTasksByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    @Test
    @DisplayName("Should return tasks due today")
    void test_26(){
        mockAuthenticatedUser();

        LocalDate today = LocalDate.now();
        Task task = new Task("Task due today", null);

        Page<Task> expected = new PageImpl<>(
                List.of(task), pageable, 1
        );
        when(taskRepository.findTasksDueTodayByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        )).thenReturn(expected);

        Page<Task> tasks = taskService.findTasksDueDate(EMAIL, pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Task due today", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findTasksDueTodayByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    @Test
    @DisplayName("Should return an empty page when there are no tasks due today")
    void test_27(){
        mockAuthenticatedUser();

        LocalDate today = LocalDate.now();

        when(taskRepository.findTasksDueTodayByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        )).thenReturn(Page.empty());

        Page<Task> tasks = taskService.findTasksDueDate(EMAIL, pageable);

        assertEquals(0, tasks.getTotalElements());

        verify(taskRepository).findTasksDueTodayByProjectMember(
                USER_ID,
                today,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    @Test
    @DisplayName("Should return upcoming tasks within the specified period")
    void test_28(){
        mockAuthenticatedUser();

        int days = 14;

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        Task task = new Task("Upcoming task", null);

        Page<Task> expected = new PageImpl<>(
                List.of(task), pageable, 1
        );
        when(taskRepository.findUpcomingTasksByProjectMember(
                USER_ID,
                today,
                endDate,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        )).thenReturn(expected);

        Page<Task> tasks = taskService.findUpComingTask(EMAIL, days, pageable);

        assertEquals(1, tasks.getTotalElements());
        assertEquals("Upcoming task", tasks.getContent().getFirst().getTitle());

        verify(taskRepository).findUpcomingTasksByProjectMember(
                USER_ID,
                today,
                endDate,
                List.of(TaskStatus.DONE, TaskStatus.CANCELED),
                pageable
        );
    }
    @Test
    @DisplayName("Should reject zero days")
    void test_29(){
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.findUpComingTask(EMAIL, 0, pageable)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(taskRepository);
    }
    @Test
    @DisplayName("Should reject negative days")
    void test_30(){
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.findUpComingTask(EMAIL, -1, pageable)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(taskRepository);
    }
    @Test
    @DisplayName("Should return task statistics")
    void test_31(){
        mockAuthenticatedUser();

        TaskStatisticsResponse expected = new TaskStatisticsResponse(
                18,5,3,8,2,3
        );

        when(taskRepository.findTaskStatistics(USER_ID)).thenReturn(expected);

        TaskStatisticsResponse statistics = taskService.findTaskStatistics(EMAIL);

        assertEquals(18, statistics.totalTasks());
        assertEquals(5, statistics.pendingTasks());
        assertEquals(3, statistics.inProgressTasks());
        assertEquals(8, statistics.completedTasks());
        assertEquals(2, statistics.canceledTasks());
        assertEquals(3, statistics.overdueTasks());
    }
    @Test
    @DisplayName("Should reject statistics request when user does not exist")
    void test_32(){
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> taskService.findTaskStatistics(EMAIL));

        verify(taskRepository, never()).findTaskStatistics(anyLong());
    }
    @Test
    @DisplayName("It must create a task assigned to a project member")
    void test_33() {
        mockAuthenticatedUser();
        mockProjectMembership();

        User assignee = new User(
                "Member",
                "member@email.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(true);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study Java",
                "Practice tests",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                2L,
                EMAIL
        );

        assertSame(assignee, createdTask.getAssignee());

        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
    }
    @Test
    @DisplayName("It must create a task assigned to a project member")
    void test_34() {
        mockAuthenticatedUser();
        mockProjectMembership();

        User assignee = new User(
                "Member",
                "member@email.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(true);
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(
                PROJECT_ID,
                "Study Java",
                "Practice tests",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                2L,
                EMAIL
        );

        assertSame(assignee, createdTask.getAssignee());

        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
    }
    @Test
    @DisplayName("It must not create a task assigned to a non-member")
    void test_35() {
        mockAuthenticatedUser();
        mockProjectMembership();

        User assignee = new User(
                "External User",
                "external@email.com",
                "hashedPassword"
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.create(
                        PROJECT_ID,
                        "Study Java",
                        "Practice tests",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        null,
                        2L,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(taskRepository, never()).save(any(Task.class));
    }
    @Test
    @DisplayName("It must update a task with a valid assignee")
    void test_36() {
        mockAuthenticatedUser();

        Task task = new Task("Study Java", "Old description");
        task.assignTo(project);
        task.setCreatedBy(user);

        User assignee = new User(
                "Member",
                "member@email.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(15L, USER_ID))
                .thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(true);

        Task updatedTask = taskService.update(
                15L,
                "Updated title",
                "Updated description",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null,
                2L,
                EMAIL
        );

        assertSame(assignee, updatedTask.getAssignee());
        assertEquals("Updated title", updatedTask.getTitle());

        verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, 2L);
    }
    @Test
    @DisplayName("It must remove the assignee from a task")
    void test_37() {
        mockAuthenticatedUser();

        Task task = new Task("Study Java", "Description");
        task.assignTo(project);
        task.setCreatedBy(user);

        User assignee = new User(
                "Member",
                "member@email.com",
                "hashedPassword"
        );

        task.assignTo(assignee);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(15L, USER_ID))
                .thenReturn(Optional.of(task));

        Task updatedTask = taskService.update(
                15L,
                "Study Java",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        assertNull(updatedTask.getAssignee());

        verify(projectMemberRepository, never())
                .existsByProjectIdAndUserId(anyLong(), anyLong());
    }
    @Test
    @DisplayName("It must not update a task with a non-member assignee")
    void test_38() {
        mockAuthenticatedUser();

        Task task = new Task("Study Java", "Description");
        task.assignTo(project);
        task.setCreatedBy(user);

        User assignee = new User(
                "External User",
                "external@email.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(15L, USER_ID))
                .thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.update(
                        15L,
                        "Updated title",
                        "Updated description",
                        TaskStatus.IN_PROGRESS,
                        TaskPriority.HIGH,
                        null,
                        2L,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals("Study Java", task.getTitle());
        assertNull(task.getAssignee());
    }
    @Test
    @DisplayName("It must not create a task with a non-existent assignee")
    void test_39() {
        mockAuthenticatedUser();
        mockProjectMembership();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.create(
                        PROJECT_ID,
                        "Study Java",
                        "Practice tests",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        null,
                        2L,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository, never()).save(any(Task.class));
        verify(projectMemberRepository, never())
                .existsByProjectIdAndUserId(anyLong(), anyLong());
    }
    @Test
    @DisplayName("Should create task and register CREATED history")
    void test_40() {
        mockAuthenticatedUser();
        mockProjectMembership();

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.create(
                PROJECT_ID,
                "Test Task",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        assertNotNull(result);

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.CREATED, history.getAction());
        assertNull(history.getFieldName());
        assertNull(history.getOldValue());
        assertNull(history.getNewValue());
        assertSame(user, history.getUser());
        assertSame(result, history.getTask());
    }
    @Test
    @DisplayName("Should create task with assignee and register CREATED history")
    void test_41() {
        mockAuthenticatedUser();
        mockProjectMembership();

        User assignee = new User(
                "Assignee",
                "assignee@email.com",
                "hashedPassword"
        );

        ReflectionTestUtils.setField(assignee, "id", 2L);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(assignee));

        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(true);

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.create(
                PROJECT_ID,
                "Test Task",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                2L,
                EMAIL
        );

        assertNotNull(result);
        assertSame(assignee, result.getAssignee());

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.CREATED, history.getAction());
        assertSame(result, history.getTask());
        assertSame(user, history.getUser());

        verify(taskHistoryRepository, times(1)).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should not register history when task creation fails")
    void test_42() {
        // Arrange
        mockAuthenticatedUser();
        mockProjectMembership();

        when(taskRepository.save(any(Task.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> taskService.create(
                        PROJECT_ID,
                        "Test Task",
                        "Description",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        null,
                        null,
                        EMAIL
                )
        );

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    private Task createTask(){
        Task task = new Task("Old title", "Old description");

        ReflectionTestUtils.setField(task, "id", 1L);

        task.assignTo(project);
        task.setCreatedBy(user);

        return task;
    }
    @Test
    @DisplayName("Should register history when updating task title")
    void test_43() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();

        taskService.update(
                1L,
                "New Title",
                oldDescription,
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.UPDATED, history.getAction());
        assertEquals(oldTitle, history.getOldValue());
        assertEquals("New Title", history.getNewValue());

        verify(taskHistoryRepository, times(1)).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history when updating task description")
    void test_44() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();

        taskService.update(
                1L,
                oldTitle,
                "New Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.UPDATED, history.getAction());
        assertEquals("description", history.getFieldName());
        assertEquals(oldDescription, history.getOldValue());
        assertEquals("New Description", history.getNewValue());

        verify(taskHistoryRepository, times(1)).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register separate history records when updating title and description")
    void test_45() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();

        taskService.update(
                1L,
                "New Title",
                "New Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository, times(2)).save(captor.capture());

        List<TaskHistory> histories = captor.getAllValues();

        assertEquals(2, histories.size());

        TaskHistory titleHistory = histories.stream()
                .filter(h -> h.getFieldName().equals("title"))
                .findFirst()
                .orElseThrow();

        TaskHistory descriptionHistory = histories.stream()
                .filter(h -> h.getFieldName().equals("description"))
                .findFirst()
                .orElseThrow();

        assertEquals(oldTitle, titleHistory.getOldValue());
        assertEquals("New Title", titleHistory.getNewValue());

        assertEquals(oldDescription, descriptionHistory.getOldValue());
        assertEquals("New Description", descriptionHistory.getNewValue());

        assertTrue(histories.stream()
                .allMatch(h -> h.getAction() == TaskHistoryAction.UPDATED));
    }
    @Test
    @DisplayName("Should not register history when title and description remain unchanged")
    void test_46() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                null,
                EMAIL
        );

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history when only priority changes")
    void test_47() {
        mockAuthenticatedUser();

        Task task = createTask();
        TaskPriority oldPriority = task.getPriority();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                TaskPriority.HIGH,
                task.getDueDate(),
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.UPDATED, history.getAction());
        assertEquals("priority", history.getFieldName());
        assertEquals(oldPriority.name(), history.getOldValue());
        assertEquals(TaskPriority.HIGH.name(), history.getNewValue());
        assertEquals(user, history.getUser());
        assertEquals(task, history.getTask());
    }
    @Test
    @DisplayName("Should register history when only due date changes")
    void test_48() {
        mockAuthenticatedUser();

        Task task = createTask();
        LocalDate oldDueDate = task.getDueDate();
        LocalDate newDueDate = LocalDate.of(2026, 12, 15);

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                newDueDate,
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.UPDATED, history.getAction());
        assertEquals("dueDate", history.getFieldName());
        assertEquals(null, history.getOldValue());
        assertEquals(newDueDate.toString(), history.getNewValue());
        assertEquals(user, history.getUser());
        assertEquals(task, history.getTask());
    }
    @Test
    @DisplayName("Should register two history records when priority and due date change")
    void test_49() {
        mockAuthenticatedUser();

        Task task = createTask();
        TaskPriority oldPriority = task.getPriority();
        LocalDate newDueDate = LocalDate.of(2026, 12, 20);

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                TaskPriority.HIGH,
                newDueDate,
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository, times(2)).save(captor.capture());

        List<TaskHistory> histories = captor.getAllValues();

        assertEquals(2, histories.size());

        TaskHistory priorityHistory = histories.stream()
                .filter(history -> "priority".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        TaskHistory dueDateHistory = histories.stream()
                .filter(history -> "dueDate".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(oldPriority.name(), priorityHistory.getOldValue());
        assertEquals(TaskPriority.HIGH.name(), priorityHistory.getNewValue());

        assertNull(dueDateHistory.getOldValue());
        assertEquals(newDueDate.toString(), dueDateHistory.getNewValue());
    }
    @Test
    @DisplayName("Should not register history when priority and due date do not change")
    void test_50() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history when assignee changes")
    void test_51() {
        mockAuthenticatedUser();

        Task task = createTask();

        User oldAssignee = new User("Old Assignee", "old@email.com", "hash");
        User newAssignee = new User("New Assignee", "new@email.com", "hash");

        ReflectionTestUtils.setField(oldAssignee, "id", 2L);
        ReflectionTestUtils.setField(newAssignee, "id", 3L);

        task.assignTo(oldAssignee);

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 3L))
                .thenReturn(true);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(newAssignee));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                3L,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals("assignee", history.getFieldName());
        assertEquals("2", history.getOldValue());
        assertEquals("3", history.getNewValue());
        assertEquals(TaskHistoryAction.ASSIGNED, history.getAction());
    }
    @Test
    @DisplayName("Should register history when assignee is removed")
    void test_52() {
        mockAuthenticatedUser();

        Task task = createTask();

        User oldAssignee = new User("Old Assignee", "old@email.com", "hash");
        ReflectionTestUtils.setField(oldAssignee, "id", 2L);

        task.assignTo(oldAssignee);

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals("assignee", history.getFieldName());
        assertEquals("2", history.getOldValue());
        assertNull(history.getNewValue());
        assertEquals(TaskHistoryAction.ASSIGNED, history.getAction());
    }
    @Test
    @DisplayName("Should not register history when assignee does not change")
    void test_53() {
        mockAuthenticatedUser();

        Task task = createTask();

        User assignee = new User("Assignee", "assignee@email.com", "hash");
        ReflectionTestUtils.setField(assignee, "id", 2L);

        task.assignTo(assignee);

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(true);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(assignee));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                2L,
                EMAIL
        );

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history when task is completed")
    void test_54() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.DONE,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.COMPLETED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals("TODO", history.getOldValue());
        assertEquals("DONE", history.getNewValue());
    }
    @Test
    @DisplayName("Should register history when completed task is reopened")
    void test_55() {
        mockAuthenticatedUser();

        Task task = createTask();
        task.completed();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.REOPENED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals("DONE", history.getOldValue());
        assertEquals("TODO", history.getNewValue());
    }
    @Test
    @DisplayName("Should register history when task is canceled")
    void test_56() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.CANCELED,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.CANCELED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals("TODO", history.getOldValue());
        assertEquals("CANCELED", history.getNewValue());
    }
    @Test
    @DisplayName("Should not register history when task status does not change")
    void test_57() {
        mockAuthenticatedUser();

        Task task = createTask();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.update(
                1L,
                task.getTitle(),
                task.getDescription(),
                TaskStatus.TODO,
                task.getPriority(),
                task.getDueDate(),
                null,
                EMAIL
        );

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history for all changed task fields")
    void test_58() {
        User assignee = new User("New Assignee", "assignee@email.com", "hashedPassword");
        ReflectionTestUtils.setField(assignee, "id", 3L);

        Task task = createTask();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 3L))
                .thenReturn(true);

        LocalDate newDueDate = LocalDate.of(2026, 12, 31);

        taskService.update(
                1L,
                "New title",
                "New description",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                newDueDate,
                3L,
                EMAIL
        );

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository, times(6)).save(captor.capture());

        List<TaskHistory> histories = captor.getAllValues();

        assertEquals(6, histories.size());

        TaskHistory titleHistory = histories.stream()
                .filter(history -> "title".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.UPDATED, titleHistory.getAction());
        assertEquals("Old title", titleHistory.getOldValue());
        assertEquals(task.getTitle(), titleHistory.getNewValue());

        TaskHistory descriptionHistory = histories.stream()
                .filter(history -> "description".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.UPDATED, descriptionHistory.getAction());
        assertEquals("Old description", descriptionHistory.getOldValue());
        assertEquals("New description", descriptionHistory.getNewValue());

        TaskHistory statusHistory = histories.stream()
                .filter(history -> "status".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.COMPLETED, statusHistory.getAction());
        assertEquals(TaskStatus.TODO.name(), statusHistory.getOldValue());
        assertEquals(TaskStatus.DONE.name(), statusHistory.getNewValue());

        TaskHistory priorityHistory = histories.stream()
                .filter(history -> "priority".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.UPDATED, priorityHistory.getAction());
        assertEquals(TaskPriority.MEDIUM.name(), priorityHistory.getOldValue());
        assertEquals(TaskPriority.HIGH.name(), priorityHistory.getNewValue());

        TaskHistory dueDateHistory = histories.stream()
                .filter(history -> "dueDate".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.UPDATED, dueDateHistory.getAction());
        assertNull(dueDateHistory.getOldValue());
        assertEquals(newDueDate.toString(), dueDateHistory.getNewValue());

        TaskHistory assigneeHistory = histories.stream()
                .filter(history -> "assignee".equals(history.getFieldName()))
                .findFirst()
                .orElseThrow();

        assertEquals(TaskHistoryAction.ASSIGNED, assigneeHistory.getAction());
        assertNull(assigneeHistory.getOldValue());
        assertEquals("3", assigneeHistory.getNewValue());
    }
    @Test
    @DisplayName("Should register history when completing a task")
    void test_59() {
        Task task = createTask();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L, EMAIL);

        assertEquals(TaskStatus.DONE, completedTask.getStatus());

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.COMPLETED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals(TaskStatus.TODO.name(), history.getOldValue());
        assertEquals(TaskStatus.DONE.name(), history.getNewValue());
        assertSame(task, history.getTask());
        assertSame(user, history.getUser());
    }
    @Test
    @DisplayName("Should register history when reopening a task")
    void test_60() {
        Task task = createTask();
        task.completed();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task reopenedTask = taskService.reopen(1L, EMAIL);

        assertEquals(TaskStatus.TODO, reopenedTask.getStatus());

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.REOPENED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals(TaskStatus.DONE.name(), history.getOldValue());
        assertEquals(TaskStatus.TODO.name(), history.getNewValue());
        assertSame(task, history.getTask());
        assertSame(user, history.getUser());
    }
    @Test
    @DisplayName("Should register history when canceling a task")
    void test_61() {
        Task task = createTask();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task canceledTask = taskService.cancel(1L, EMAIL);

        assertEquals(TaskStatus.CANCELED, canceledTask.getStatus());

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.CANCELED, history.getAction());
        assertEquals("status", history.getFieldName());
        assertEquals(TaskStatus.TODO.name(), history.getOldValue());
        assertEquals(TaskStatus.CANCELED.name(), history.getNewValue());
        assertSame(task, history.getTask());
        assertSame(user, history.getUser());
    }
    @Test
    @DisplayName("Should not register history when completing an already completed task")
    void test_62() {
        Task task = createTask();
        task.completed();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task completedTask = taskService.complete(1L, EMAIL);

        assertEquals(TaskStatus.DONE, completedTask.getStatus());

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should not register history when reopening an already open task")
    void test_63() {
        Task task = createTask();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task reopenedTask = taskService.reopen(1L, EMAIL);

        assertEquals(TaskStatus.TODO, reopenedTask.getStatus());

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should not register history when canceling an already canceled task")
    void test_64() {
        Task task = createTask();
        task.cancel();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        Task canceledTask = taskService.cancel(1L, EMAIL);

        assertEquals(TaskStatus.CANCELED, canceledTask.getStatus());

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }
    @Test
    @DisplayName("Should register history when deleting a task")
    void test_65() {
        Task task = createTask();

        mockAuthenticatedUser();

        when(taskRepository.findByIdAndProjectMemberUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.delete(1L, EMAIL);

        ArgumentCaptor<TaskHistory> captor =
                ArgumentCaptor.forClass(TaskHistory.class);

        verify(taskHistoryRepository).saveAndFlush(captor.capture());

        TaskHistory history = captor.getValue();

        assertEquals(TaskHistoryAction.DELETED, history.getAction());
        assertNull(history.getFieldName());
        assertNull(history.getOldValue());
        assertNull(history.getNewValue());
        assertSame(task, history.getTask());
        assertSame(user, history.getUser());

        verify(taskRepository).delete(task);
    }
}
