package com.fluxora.controller;

import com.fluxora.dto.CategoryRequest;
import com.fluxora.dto.CategoryResponse;
import com.fluxora.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication){
        CategoryResponse response = CategoryResponse.from(
                categoryService.create(request.name(),
                        authentication.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll(Authentication authentication){
        List<CategoryResponse> categories = categoryService
                .findAll(authentication.getName())
                .stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(categories);
    }
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(CategoryResponse.from(categoryService.findById(id, authentication.getName())));
    }
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> rename(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication){
        return ResponseEntity.ok(CategoryResponse.from(categoryService.rename(
                id,
                request.name(),
                authentication.getName())));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication){
        categoryService.delete(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

}
