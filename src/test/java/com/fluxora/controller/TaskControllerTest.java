package com.fluxora.controller;

import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.entity.Task;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("It must create a task and return 201 Created.")
    void test_01() throws Exception{
        Task task = new Task("Study java", "Practice tests");

        when(taskService.create("Study java", "Practice tests")).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
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
        verify(taskService).create("Study java", "Practice tests");
    }
    @Test
    @DisplayName("Should list tasks and return 200 OK")
    void test_02() throws Exception{
        when(taskService.findAll()).thenReturn(List.of(
                new Task("Study java", null),
                new Task("Practice spring", null)
        ));
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Study java"))
                .andExpect(jsonPath("$[1].title").value("Practice spring"));
    }
    @Test
    @DisplayName("view an existing task")
    void test_03() throws Exception{
        when(taskService.findById(1L)).thenReturn(new Task("Study java", null));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study java"))
                .andExpect(jsonPath("$.completed").value(false));
    }
    @Test
    @DisplayName("Should return 404 when the task does not exist.")
    void test_04() throws Exception{
        when(taskService.findById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
    @Test
    @DisplayName("Must reject a task with an empty title.")
    void test_05() throws Exception{
        mockMvc.perform(post("/api/tasks")
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
    @DisplayName("It should delete a task and return 204 No Content.")
    void test_06() throws Exception{
        doNothing().when(taskService).delete(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(taskService).delete(1L);
    }

}
