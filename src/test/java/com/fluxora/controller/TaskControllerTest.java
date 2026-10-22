package com.fluxora.controller;

import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.entity.Task;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.service.ProjectService;
import com.fluxora.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    private static final String EMAIl = "test@fluxora.com";
    private static final Long PROJECT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication(){
        return new UsernamePasswordAuthenticationToken(EMAIl, "password");
    }

    @Test
    @WithMockUser(username = EMAIl)
    @DisplayName("It must create a task and return 201 Created.")
    void test_01() throws Exception{
        Task task = new Task("Study java", "Practice tests");

        when(taskService.create(PROJECT_ID, "Study java", "Practice tests", EMAIl)).thenReturn(task);

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
                .andExpect(jsonPath("$.completed").value(false));

        verify(taskService).create(PROJECT_ID,"Study java", "Practice tests", EMAIl);
    }
    @Test
    @WithMockUser(username = EMAIl)
    @DisplayName("Should list tasks and return 200 OK")
    void test_02() throws Exception{

        when(taskService.findAll(PROJECT_ID, EMAIl)).thenReturn(List.of(
                new Task("Study java", null),
                new Task("Practice spring", null)
        ));
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Study java"))
                .andExpect(jsonPath("$[1].title").value("Practice spring"));

        verify(taskService).findAll(PROJECT_ID, EMAIl);
    }
    @Test
    @WithMockUser(username = EMAIl)
    @DisplayName("view an existing task")
    void test_03() throws Exception{
        when(taskService.findById(1L, EMAIl)).thenReturn(new Task("Study java", null));

        mockMvc.perform(get("/api/tasks/1")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study java"))
                .andExpect(jsonPath("$.completed").value(false));

        verify(taskService).findById(1L, EMAIl);
    }
    @Test
    @WithMockUser(username = EMAIl)
    @DisplayName("Should return 404 when the task does not exist.")
    void test_04() throws Exception{
        when(taskService.findById(99L, EMAIl)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99")
                        .principal(authentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(taskService).findById(99L, EMAIl);
    }
    @Test
    @WithMockUser(username = EMAIl)
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
    @WithMockUser(username = EMAIl)
    @DisplayName("It should delete a task and return 204 No Content.")
    void test_06() throws Exception{
        doNothing().when(taskService).delete(1L, EMAIl);

        mockMvc.perform(delete("/api/tasks/1")
                        .principal(authentication()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(taskService).delete(1L, EMAIl);
    }

}
