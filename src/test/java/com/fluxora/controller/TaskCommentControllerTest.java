package com.fluxora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.dto.TaskCommentRequest;
import com.fluxora.entity.TaskComment;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.service.TaskCommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskCommentControllerTest {

    private static final String EMAIL = "test@fluxora.com";
    private static final Long TASK_ID = 1L;
    private static final Long COMMENT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskCommentService taskCommentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(EMAIL, "password");
    }

    @Test
    @DisplayName("Should create comment with 201 CREATED")
    void test_01() throws Exception {

        User user = new User("Test User", EMAIL, "hashedPassword");
        Task task = new Task("Study Java", "Practice tests");

        TaskComment comment = new TaskComment(task, user, "Great progress!");

        when(taskCommentService.create(
                eq(TASK_ID),
                eq("Great progress!"),
                eq(EMAIL)
        )).thenReturn(comment);

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("Great progress!")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Great progress!"))
                .andExpect(jsonPath("$.authorName").value("Test User"))
                .andExpect(jsonPath("$.edited").value(false));

        verify(taskCommentService).create(
                eq(TASK_ID),
                eq("Great progress!"),
                eq(EMAIL)
        );
    }

    @Test
    @DisplayName("Should return 400 when comment content is blank")
    void test_02() throws Exception {

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("   ")
                        )))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return 400 when comment content exceeds 5000 characters")
    void test_03() throws Exception {

        String content = "a".repeat(5001);

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest(content)
                        )))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return 400 when comment content is missing")
    void test_04() throws Exception {

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }
    @Test
    @DisplayName("Should return task comments with 200 OK")
    void test_05() throws Exception {

        User user = new User("Test User", EMAIL, "hashedPassword");
        Task task = new Task("Study Java", "Practice tests");

        TaskComment comment = new TaskComment(task, user, "Great progress!");

        Page<TaskComment> commentPage = new PageImpl<>(
                List.of(comment),
                PageRequest.of(0, 10),
                1
        );

        when(taskCommentService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(commentPage);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Great progress!"))
                .andExpect(jsonPath("$.content[0].authorName").value("Test User"))
                .andExpect(jsonPath("$.content[0].edited").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskCommentService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Should apply pagination and sorting parameters")
    void test_06() throws Exception {

        Page<TaskComment> commentPage = new PageImpl<>(
                List.of(),
                PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "content")),
                0
        );

        when(taskCommentService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(commentPage);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "content")
                        .param("direction", "asc")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(taskCommentService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                argThat(pageable ->
                        pageable.getPageNumber() == 2
                                && pageable.getPageSize() == 5
                                && pageable.getSort().getOrderFor("content").isAscending()
                )
        );
    }

    @Test
    @DisplayName("Should use default pagination and sorting parameters")
    void test_07() throws Exception {

        Page<TaskComment> commentPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                0
        );

        when(taskCommentService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(commentPage);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(taskCommentService).findAll(
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
    void test_08() throws Exception {

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("page", "-1")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return 400 when page size is invalid")
    void test_09() throws Exception {

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("size", "0")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return 400 when sort direction is invalid")
    void test_10() throws Exception {

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("direction", "invalid")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return 400 when sort field is not allowed")
    void test_11() throws Exception {

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("sortBy", "authorName")
                        .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }

    @Test
    @DisplayName("Should return empty comments")
    void test_12() throws Exception {

        Page<TaskComment> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(taskCommentService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(taskCommentService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Should allow sorting by updatedAt")
    void test_13() throws Exception {

        Page<TaskComment> commentPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "updatedAt")),
                0
        );

        when(taskCommentService.findAll(
                eq(TASK_ID),
                eq(EMAIL),
                any(Pageable.class)
        )).thenReturn(commentPage);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID)
                        .param("sortBy", "updatedAt")
                        .param("direction", "asc")
                        .principal(authentication()))
                .andExpect(status().isOk());

        verify(taskCommentService).findAll(
                eq(TASK_ID),
                eq(EMAIL),
                argThat(pageable ->
                        pageable.getSort().getOrderFor("updatedAt") != null
                                && pageable.getSort().getOrderFor("updatedAt").isAscending()
                )
        );
    }
    @Test
    @DisplayName("Should update comment with 200 OK")
    void test_14() throws Exception {

        TaskComment comment = mock(TaskComment.class);
        User user = mock(User.class);

        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        LocalDateTime updatedAt = LocalDateTime.now();

        when(comment.getId()).thenReturn(COMMENT_ID);
        when(comment.getContent()).thenReturn("Updated comment");
        when(comment.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Test User");
        when(comment.getCreatedAt()).thenReturn(createdAt);
        when(comment.getUpdatedAt()).thenReturn(updatedAt);

        when(taskCommentService.update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        )).thenReturn(comment);

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("Updated comment")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMMENT_ID))
                .andExpect(jsonPath("$.content").value("Updated comment"))
                .andExpect(jsonPath("$.authorName").value("Test User"))
                .andExpect(jsonPath("$.edited").value(true));

        verify(taskCommentService).update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should return 400 when updated comment content is blank")
    void test_15() throws Exception {

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("   ")
                        )))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }
    @Test
    @DisplayName("Should return 400 when updated comment exceeds 5000 characters")
    void test_16() throws Exception {

        String content = "a".repeat(5001);

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest(content)
                        )))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskCommentService);
    }
    @Test
    @DisplayName("Should return 404 when comment does not exist")
    void test_17() throws Exception {

        when(taskCommentService.update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Comment not found"
        ));

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("Updated comment")
                        )))
                .andExpect(status().isNotFound());

        verify(taskCommentService).update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should return 403 when user cannot update another user's comment")
    void test_18() throws Exception {

        when(taskCommentService.update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to manage another user's comment"
        ));

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("Updated comment")
                        )))
                .andExpect(status().isForbidden());

        verify(taskCommentService).update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should return 403 when author's editing period has expired")
    void test_19() throws Exception {

        when(taskCommentService.update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "The 15-minute editing period has expired"
        ));

        mockMvc.perform(put(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskCommentRequest("Updated comment")
                        )))
                .andExpect(status().isForbidden());

        verify(taskCommentService).update(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq("Updated comment"),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should delete comment with 204 NO CONTENT")
    void test_20() throws Exception {

        doNothing().when(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );

        mockMvc.perform(delete(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should return 404 when deleting a nonexistent comment")
    void test_21() throws Exception {

        doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Comment not found"
        )).when(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );

        mockMvc.perform(delete(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication()))
                .andExpect(status().isNotFound());

        verify(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should return 403 when user cannot delete another user's comment")
    void test_22() throws Exception {

        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to manage another user's comment"
        )).when(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );

        mockMvc.perform(delete(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication()))
                .andExpect(status().isForbidden());

        verify(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );
    }
    @Test
    @DisplayName("Should allow ADMIN or OWNER to delete another user's comment")
    void test_23() throws Exception {

        doNothing().when(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );

        mockMvc.perform(delete(
                        "/api/tasks/{taskId}/comments/{commentId}",
                        TASK_ID,
                        COMMENT_ID
                )
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(taskCommentService).delete(
                eq(COMMENT_ID),
                eq(TASK_ID),
                eq(EMAIL)
        );
    }
}