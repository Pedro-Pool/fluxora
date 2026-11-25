
package com.fluxora.service;

import com.fluxora.entity.Category;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.CategoryRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    private CategoryRepository categoryRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private CategoryService categoryService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);

        categoryService = new CategoryService(
                categoryRepository,
                taskRepository,
                userRepository
        );

        user = new User("Test User", EMAIL, "hashedPassword");

        ReflectionTestUtils.setField(user, "id", USER_ID);

        category = new Category("Work");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));
    }

    private ResponseStatusException assertStatus(
            HttpStatus expected,
            Executable executable
    ) {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, executable);

        assertEquals(expected, exception.getStatusCode());
        return exception;
    }

    @Test
    @DisplayName("Should return all categories belonging to the user")
    void test_01() {
        when(categoryRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(category));

        List<Category> result = categoryService.findAll(EMAIL);

        assertEquals(1, result.size());
        assertSame(category, result.get(0));
        verify(categoryRepository).findAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should return an empty list when user has no categories")
    void test_02() {
        when(categoryRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of());

        List<Category> result = categoryService.findAll(EMAIL);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return 401 when authenticated user does not exist")
    void test_03() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> categoryService.findAll(EMAIL));
    }

    // findById()

    @Test
    @DisplayName("Should return a category belonging to the user")
    void test_04() {
        Category result = categoryService.findById(CATEGORY_ID, EMAIL);

        assertSame(category, result);
    }

    @Test
    @DisplayName("Should return 404 when category does not exist")
    void test_05() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> categoryService.findById(CATEGORY_ID, EMAIL));
    }

    @Test
    @DisplayName("Should return 404 when category belongs to another user")
    void test_06() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> categoryService.findById(CATEGORY_ID, EMAIL));
    }

    @Test
    @DisplayName("Should return 401 when authenticated user does not exist")
    void test_07() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> categoryService.findById(CATEGORY_ID, EMAIL));
    }

    // create()

    @Test
    @DisplayName("Should create a category with a valid name")
    void test_08() {
        when(categoryRepository.existsByUserIdAndName(USER_ID, "Work"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.create("Work", EMAIL);

        assertEquals("Work", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should trim category name before saving")
    void test_09() {
        when(categoryRepository.existsByUserIdAndName(USER_ID, "Work"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.create("  Work  ", EMAIL);

        assertEquals("Work", result.getName());
        verify(categoryRepository).existsByUserIdAndName(USER_ID, "Work");
    }

    @Test
    @DisplayName("Should return 409 when category name already exists")
    void test_10() {
        when(categoryRepository.existsByUserIdAndName(USER_ID, "Work"))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT,
                () -> categoryService.create("Work", EMAIL));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should reject null category name")
    void test_11() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(null, EMAIL));
    }

    @Test
    @DisplayName("Should reject blank category name")
    void test_12() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create("   ", EMAIL));
    }

    @Test
    @DisplayName("Should reject category name longer than 50 characters")
    void test_13() {
        String name = "A".repeat(51);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(name, EMAIL));
    }

    @Test
    @DisplayName("Should return 401 when authenticated user does not exist")
    void test_14() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> categoryService.create("Work", EMAIL));
    }

    // rename()

    @Test
    @DisplayName("Should rename a category with a valid name")
    void test_15() {
        Category result = categoryService.rename(
                CATEGORY_ID, "Personal", EMAIL
        );

        assertEquals("Personal", result.getName());
    }

    @Test
    @DisplayName("Should trim category name when renaming")
    void test_16() {
        Category result = categoryService.rename(
                CATEGORY_ID, "  Personal  ", EMAIL
        );

        assertEquals("Personal", result.getName());
    }

    @Test
    @DisplayName("Should return 409 when new name already belongs to another category")
    void test_17() {
        when(categoryRepository.existsByUserIdAndName(USER_ID, "Personal"))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT,
                () -> categoryService.rename(
                        CATEGORY_ID, "Personal", EMAIL
                ));
    }

    @Test
    @DisplayName("Should allow keeping the same category name")
    void test_18() {
        Category result = categoryService.rename(
                CATEGORY_ID, "Work", EMAIL
        );

        assertEquals("Work", result.getName());

        verify(categoryRepository, never())
                .existsByUserIdAndName(USER_ID, "Work");
    }

    @Test
    @DisplayName("Should return 404 when category does not belong to the user")
    void test_19() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> categoryService.rename(
                        CATEGORY_ID, "Personal", EMAIL
                ));
    }

    @Test
    @DisplayName("Should reject invalid category name when renaming")
    void test_20() {
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.rename(
                        CATEGORY_ID, "   ", EMAIL
                ));
    }
    @Test
    @DisplayName("Should delete a category without associated tasks")
    void test_21() {
        when(taskRepository.findAllTasksByProjectMemberUserId(USER_ID))
                .thenReturn(List.of());

        categoryService.delete(CATEGORY_ID, EMAIL);

        verify(taskRepository)
                .findAllTasksByProjectMemberUserId(USER_ID);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Should delete a category without associated tasks")
    void test_22() {
        when(taskRepository.findAllTasksByProjectMemberUserId(USER_ID))
                .thenReturn(List.of());

        categoryService.delete(CATEGORY_ID, EMAIL);

        verify(taskRepository).findAllTasksByProjectMemberUserId(USER_ID);
        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Should remove category from associated tasks before deleting it")
    void test_23() {
        Task task1 = mock(Task.class);
        Task task2 = mock(Task.class);

        when(taskRepository.findAllTasksByProjectMemberUserId(USER_ID))
                .thenReturn(List.of(task1, task2));

        categoryService.delete(CATEGORY_ID, EMAIL);

        verify(task1).removeCategory(category);
        verify(task2).removeCategory(category);
        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Should retrieve tasks from all projects where user is a member")
    void test_24() {
        when(taskRepository.findAllTasksByProjectMemberUserId(USER_ID))
                .thenReturn(List.of());

        categoryService.delete(CATEGORY_ID, EMAIL);

        verify(taskRepository).findAllTasksByProjectMemberUserId(USER_ID);
    }

    @Test
    @DisplayName("Should return 404 when deleting a category that does not belong to the user")
    void test_25() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> categoryService.delete(CATEGORY_ID, EMAIL));

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Should return 401 when authenticated user does not exist")
    void test_26() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> categoryService.delete(CATEGORY_ID, EMAIL));

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Should add a category belonging to the user to a task")
    void test_27() {
        Task task = mock(Task.class);

        when(taskRepository.findByIdAndProjectMemberUserId(10L, USER_ID))
                .thenReturn(Optional.of(task));

        categoryService.addToTask(10L, CATEGORY_ID, EMAIL);

        verify(task).addCategory(category);
    }

    @Test
    @DisplayName("Should throw TaskNotFoundException when user is not a member of the task project")
    void test_28() {
        when(taskRepository.findByIdAndProjectMemberUserId(10L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> categoryService.addToTask(10L, CATEGORY_ID, EMAIL));

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should return 404 when adding a category belonging to another user")
    void test_29() {
        Task task = mock(Task.class);

        when(taskRepository.findByIdAndProjectMemberUserId(10L, USER_ID))
                .thenReturn(Optional.of(task));

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> categoryService.addToTask(10L, CATEGORY_ID, EMAIL));

        verify(task, never()).addCategory(any(Category.class));
    }

    @Test
    @DisplayName("Should remove a category from a task")
    void test_30() {
        Task task = mock(Task.class);

        when(taskRepository.findByIdAndProjectMemberUserId(10L, USER_ID))
                .thenReturn(Optional.of(task));

        categoryService.removeFromTask(10L, CATEGORY_ID, EMAIL);

        verify(task).removeCategory(category);
    }

    @Test
    @DisplayName("Should throw TaskNotFoundException when removing category from inaccessible task")
    void test_31() {
        when(taskRepository.findByIdAndProjectMemberUserId(10L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> categoryService.removeFromTask(10L, CATEGORY_ID, EMAIL));

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());
    }

}
