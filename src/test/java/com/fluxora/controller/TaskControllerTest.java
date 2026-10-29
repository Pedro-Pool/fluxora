package com.fluxora.controller;

import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.entity.Task;
import com.fluxora.entity.TaskPriority;
import com.fluxora.entity.TaskStatus;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.service.CategoryService;
import com.fluxora.service.ProjectService;
import com.fluxora.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    private static final String EMAIL = "test@fluxora.com";
    private static final Long PROJECT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication(){
        return new UsernamePasswordAuthenticationToken(EMAIL, "password");
    }

    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("It must create a task and return 201 Created.")
    void test_01() throws Exception{
        Task task = new Task("Study java", "Practice tests");

        when(taskService.create(
                eq(PROJECT_ID),
                eq("Study java"),
                eq("Practice tests"),
                isNull(),
                isNull(),
                isNull(),
                eq(EMAIL))).thenReturn(task);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "title": "Study java",
                                  "description": "Practice tests"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study java"))
                .andExpect(jsonPath("$.description").value("Practice tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        verify(taskService).create(
                PROJECT_ID,
                "Study java",
                "Practice tests",
                null,
                null,
                null,
                EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should list tasks and return 200 OK")
    void test_02() throws Exception{

        when(taskService.findAll(
                eq(PROJECT_ID),
                eq(EMAIL),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
                ))
                .thenAnswer(invocation -> {Pageable pageable = invocation.getArgument(6);
                    return new PageImpl<>(
                            List.of(
                                    new Task("Study java", null),
                                    new Task("Practice spring", null)
                            ),
                            pageable,
                            2
                    );
                });
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Study java"))
                .andExpect(jsonPath("$.content[1].title").value("Practice spring"));

        verify(taskService).findAll(
                eq(PROJECT_ID),
                eq(EMAIL),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class));
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("view an existing task")
    void test_03() throws Exception{
        when(taskService.findById(1L, EMAIL)).thenReturn(new Task("Study java", null));

        mockMvc.perform(get("/api/tasks/1")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study java"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        verify(taskService).findById(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should return 404 when the task does not exist.")
    void test_04() throws Exception{
        when(taskService.findById(99L, EMAIL)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99")
                        .principal(authentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(taskService).findById(99L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Must reject a task with an empty title.")
    void test_05() throws Exception{
        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "description"
                                }
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(taskService);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("It should delete a task and return 204 No Content.")
    void test_06() throws Exception{
        doNothing().when(taskService).delete(1L, EMAIL);

        mockMvc.perform(delete("/api/tasks/1")
                        .principal(authentication()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(taskService).delete(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should update a task and return 200 OK")
    void test_07() throws Exception{
        Task task = new Task("Updated task", "Updated description");

        when(taskService.update(
                eq(1L),
                eq("Updated task"),
                eq("Updated description"),
                eq(TaskStatus.IN_PROGRESS),
                eq(TaskPriority.HIGH),
                isNull(),
                eq(EMAIL)
        )).thenReturn(task);

        mockMvc.perform(put("/api/tasks/1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Updated task",
                      "description": "Updated description",
                      "status": "IN_PROGRESS",
                      "priority": "HIGH"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.description").value("Updated description"));

        verify(taskService).update(
                1L,
                "Updated task",
                "Updated description",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null,
                EMAIL
        );
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should completed a task and return 200 OK")
    void test_08() throws Exception{
        Task task = new Task("Study java", "Practice tests");
        task.completed();

        when(taskService.complete(1L, EMAIL)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/1/complete")
                .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());

        verify(taskService).complete(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should reopen a task and return 200 OK")
    void test_09() throws Exception{
        Task task = new Task("Study java", "Practice tests");
        task.reopen();

        when(taskService.reopen(1L, EMAIL)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/1/reopen")
                .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));

        verify(taskService).reopen(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should cancel a task and return 200 OK")
    void test_10() throws Exception{
        Task task = new Task("Study java", "Practice tests");
        task.cancel();

        when(taskService.cancel(1L, EMAIL)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/1/cancel")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(taskService).cancel(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should return 404 when completing a nonexistent task")
    void test_11() throws Exception{
        when(taskService.complete(99L, EMAIL)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(patch("/api/tasks/99/complete")
                .principal(authentication()))
                .andExpect(status().isNotFound());

        verify(taskService).complete(99L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should return 404 when updating a nonexistent task")
    void test_12() throws Exception{
        when(taskService.update(

                eq(99L),
                eq("Updated task"),
                eq("Description"),
                eq(TaskStatus.TODO),
                eq(TaskPriority.MEDIUM),
                isNull(),
                eq(EMAIL)
        )).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/99")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Updated task",
                      "description": "Description",
                      "status": "TODO",
                      "priority": "MEDIUM"
                    }
                    """))
                .andExpect(status().isNotFound());

        verify(taskService).update(
                99L,
                "Updated task",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                EMAIL
        );
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should pass task filters to the service")
    void test_13() throws Exception{
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        when(taskService.findAll(
                PROJECT_ID,
                EMAIL,
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                from,
                to,
                pageable
        )).thenReturn(new PageImpl<>(
                List.of(new Task("Filtered Task", null)),
                pageable,
                1));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                    .principal(authentication())
                    .param("status", "IN_PROGRESS")
                    .param("priority", "URGENT")
                    .param("dueDateFrom", "2026-10-01")
                    .param("dueDateTo", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Filtered Task"));

        verify(taskService).findAll(
                eq(PROJECT_ID),
                eq(EMAIL),
                eq(TaskStatus.IN_PROGRESS),
                eq(TaskPriority.URGENT),
                eq(from),
                eq(to),
                any(Pageable.class)
        );
    }
}
