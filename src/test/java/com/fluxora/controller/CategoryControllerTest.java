package com.fluxora.controller;

import com.fluxora.config.JwtAuthenticationFilter;
import com.fluxora.entity.Category;
import com.fluxora.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    private static final String EMAIL = "test@fluxora.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication(){
        return new UsernamePasswordAuthenticationToken(EMAIL, "password");
    }

    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should create a category and return 201 Created")
    void test_01() throws Exception{
        Category category = new Category("Backend");

        when(categoryService.create("Backend", EMAIL)).thenReturn(category);

        mockMvc.perform(post("/api/categories")
                    .principal(authentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Backend"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Backend"));
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should list categories and return 200 OK")
    void test_02() throws Exception{
        when(categoryService.findAll(EMAIL)).thenReturn(List.of(
                new Category("Backend"),
                new Category("Bug")
        ));

        mockMvc.perform(get("/api/categories")
                    .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Backend"))
                .andExpect(jsonPath("$[1].name").value("Bug"));

        verify(categoryService).findAll(EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should find a categories and return 200 OK")
    void test_03() throws Exception{
        when(categoryService.findById(1L, EMAIL)).thenReturn(new Category("Backend"));

        mockMvc.perform(get("/api/categories/1")
                    .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend"));

        verify(categoryService).findById(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should rename categories and return 200 OK")
    void test_04() throws Exception{
        when(categoryService.rename(1L,"Studies",  EMAIL)).thenReturn(new Category("Studies"));

        mockMvc.perform(put("/api/categories/1")
                    .principal(authentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Studies"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Studies"));

        verify(categoryService).rename(1L, "Studies", EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should delete categories and return 204 No content")
    void test_05() throws Exception{
        doNothing().when(categoryService).delete(1L, EMAIL);

        mockMvc.perform(delete("/api/categories/1")
                    .principal(authentication()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(categoryService).delete(1L, EMAIL);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should reject category with a blank name")
    void test_06() throws Exception{
        mockMvc.perform(post("/api/categories")
                    .principal(authentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }
    @Test
    @WithMockUser(username = EMAIL)
    @DisplayName("Should reject category name longer than 50 characters")
    void test_07() throws Exception{
        String longName = "A".repeat(51);

        mockMvc.perform(post("/api/categories")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }
}
