package com.fluxora.controller;

import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.entity.Project;
import com.fluxora.entity.Task;
import com.fluxora.entity.TaskHistory;
import com.fluxora.entity.User;
import com.fluxora.enums.TaskHistoryAction;
import com.fluxora.service.TaskHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskHistoryControllerTest {

    private static final String EMAIL = "test@fluxora.com";
    private static final Long TASK_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskHistoryService taskHistoryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(EMAIL, "password");
    }

    @Test
    @DisplayName("Should return task history with 200 OK")
    void test_01() throws Exception {

        User user = new User("Test User", EMAIL, "hashedPassword");

        Task task = new Task("Study Java", "Practice tests");

        task.assignTo(new Project("Study java", null));

        TaskHistory history = new TaskHistory(
                task.getProject(),
                task,
                user,
                TaskHistoryAction.CREATED,
                null,
                null,
                null
        );

        Page<TaskHistory> historyPage = new PageImpl<>(
                List.of(history),
                PageRequest.of(0, 10),
                1
        );

        when(taskHistoryService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any()
        )).thenReturn(historyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .principal(authentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("CREATED"))
                .andExpect(jsonPath("$.content[0].fieldName").value(nullValue()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskHistoryService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any()
        );
    }
    @Test
    @DisplayName("Should apply pagination and sorting parameters")
    void test_02() throws Exception {
        Page<TaskHistory> historyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "action")),
                0
        );

        when(taskHistoryService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(historyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "action")
                        .param("direction", "asc")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(taskHistoryService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                argThat(pageable ->
                        pageable.getPageNumber() == 2
                                && pageable.getPageSize() == 5
                                && pageable.getSort().getOrderFor("action").isAscending()
                )
        );
    }
    @Test
    @DisplayName("Should use default pagination and sorting parameters")
    void test_03() throws Exception {
        Page<TaskHistory> historyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                0
        );

        when(taskHistoryService.findAll(eq(TASK_ID), eq(EMAIL), any(Pageable.class)))
                .thenReturn(historyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(taskHistoryService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("createdAt").isDescending()
                )
        );
    }
    @Test
    @DisplayName("Should return 400 when page is negative")
    void test_04() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .param("page", "-1")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskHistoryService);
    }
    @Test
    @DisplayName("Should return 400 when page size is invalid")
    void test_05() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .param("size", "0")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskHistoryService);
    }
    @Test
    @DisplayName("Should return 400 when sort direction is invalid")
    void test_06() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .param("direction", "invalid")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskHistoryService);
    }
    @Test
    @DisplayName("Should return 400 when sort field is not allowed")
    void test_07() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .param("sortBy", "userEmail")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskHistoryService);
    }
    @Test
    @DisplayName("Should return empty history")
    void test_08() throws Exception {
        Page<TaskHistory> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(taskHistoryService.findAll(eq(TASK_ID), eq(EMAIL), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(taskHistoryService).findAll(eq(TASK_ID), eq(EMAIL), any(Pageable.class));
    }
    @Test
    @DisplayName("Should return deletion history with null task id")
    void test_09() throws Exception {

        Project project = new Project("Test Project", "Test Description");

        User user = new User("Test User", EMAIL, "hashedPassword");



        TaskHistory history = new TaskHistory(
                project,
                null,
                user,
                TaskHistoryAction.DELETED,
                null,
                null,
                null
        );

        Page<TaskHistory> historyPage = new PageImpl<>(
                List.of(history),
                PageRequest.of(0, 10),
                1
        );

        when(taskHistoryService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(historyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/history", TASK_ID)
                        .principal(authentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("DELETED"))
                .andExpect(jsonPath("$.content[0].taskId").value(nullValue()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}