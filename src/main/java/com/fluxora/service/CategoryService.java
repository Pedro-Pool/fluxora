package com.fluxora.service;

import com.fluxora.entity.Category;
import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.CategoryRepository;
import com.fluxora.repository.TaskRepository;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }
    public List<Category> findAll(String email){
        User user = findUserByEmail(email);
        return categoryRepository.findAllByUserId(user.getId());
    }
    public Category findById(Long id, String email){
        User user = findUserByEmail(email);
        return findCategoryByIdAndUser(id, user.getId());
    }
    @Transactional
    public Category create(String name, String email){
        User user = findUserByEmail(email);
        String normalizedName = validateName(name);

        if (categoryRepository.existsByUserIdAndName(user.getId(), normalizedName)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        Category category = new Category(normalizedName);
        category.assignTo(user);

        return categoryRepository.save(category);
    }
    @Transactional
    public Category rename(Long id, String name, String email){
        User user = findUserByEmail(email);
        Category category = findCategoryByIdAndUser(id, user.getId());
        String normalizedName = validateName(name);

        if (!category.getName().equals(normalizedName)
                && categoryRepository.existsByUserIdAndName(user.getId(), normalizedName)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        category.rename(name);
        return category;
    }
    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Category category = findCategoryByIdAndUser(id, user.getId());

        List<Task> tasks = taskRepository.findAllByProjectUserId( user.getId());

        for (Task task : tasks){
            task.removeCategory(category);
        }

        categoryRepository.delete(category);
    }
    @Transactional
    public void addToTask(Long taskId, Long categoryId, String email){
        User user = findUserByEmail(email);

        Task task = taskRepository.findByIdAndProjectUserId(taskId, user.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Category category = findCategoryByIdAndUser(categoryId, user.getId());

        task.addCategory(category);
    }
    @Transactional
    public void removeFromTask(Long taskId, Long categoryId, String email){
        User user = findUserByEmail(email);

        Task task = taskRepository.findByIdAndProjectUserId(taskId, user.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Category category = findCategoryByIdAndUser(categoryId, user.getId());

        task.removeCategory(category);
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
    private Category findCategoryByIdAndUser(Long id, Long userId){
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
    private String validateName(String name){
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Category name is mandatory");
        }
        String normalizedName = name.trim();

        if (normalizedName.length() > 50){
            throw new IllegalArgumentException("Category name must have a maximum of 50 characters");
        }
        return normalizedName;
    }
}
