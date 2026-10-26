package com.fluxora.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column( length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updateAt;

    @Column
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToMany
    @JoinTable(name = "task_categories", joinColumns = @JoinColumn(name = "task_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "category_id"}))
    private Set<Category> categories = new HashSet<>();

    public Task() {}

    public Task(String title, String description){
        this.title = requireTitle(title);
        this.description = description;
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
    }
    public void assignTo(Project project){
        if (project == null){
            throw new IllegalArgumentException("Task project is mandatory");
        }
        this.project = project;
    }

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
        updateAt = now;
    }
    @PreUpdate
    protected void onUpdate(){
        updateAt = LocalDateTime.now();
    }
    public void updateDetails(String title, String description){
        this.title = requireTitle(title);
        this.description = description;
    }
    public void updatePlanning(TaskStatus status, TaskPriority priority, LocalDate dueDate){
        if (status != null){
            this.status = status;
        }
        if (priority != null){
            this.priority = priority;
        }
        this.dueDate = dueDate;
    }

    public void completed(){
        this.status = TaskStatus.DONE;
        this.completedAt = LocalDateTime.now();
    }
    public void reopen(){
        this.status = TaskStatus.TODO;
        this.completedAt = null;
    }
    public void cancel(){
        this.status = TaskStatus.CANCELED;
        this.completedAt = null;
    }
    public boolean isCompleted(){
        return this.status == TaskStatus.DONE;
    }

    public Set<Category> getCategories(){
        return Set.copyOf(categories);
    }
    public void addCategory(Category category){
        if (category == null){
            throw new IllegalArgumentException("Category is mandatory");
        }
        categories.add(category);
    }

    public void removeCategory(Category category){
        categories.remove(category);
    }
    public void replaceCategories(Set<Category> categories){
        if (categories == null){
            throw new IllegalArgumentException("Categories are mandatory");
        }
        if (categories.contains(null)){
            throw new IllegalArgumentException("Category cannot be null");
        }
        this.categories.clear();
        this.categories.addAll(categories);
    }


    private String requireTitle(String title){
        if (title == null || title.isBlank()){
            throw new IllegalArgumentException("The task title is mandatory.");
        }
        String normalizedTitle = title.trim();

        if (normalizedTitle.length() > 150){
            throw new IllegalArgumentException("The title must have a maximum of 150 characters.");
        }
        return normalizedTitle;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus(){
        return status;
    }

    public TaskPriority getPriority(){
        return priority;
    }

    public LocalDate getDueDate(){
        return dueDate;
    }

    public LocalDateTime getCreatedAt(){
        return  createdAt;
    }

    public LocalDateTime getUpdateAt(){
        return updateAt;
    }

    public LocalDateTime getCompletedAt(){
        return completedAt;
    }

    public Project getProject(){
        return project;
    }

    @Override
    public boolean equals(Object object){
        if (this == object) {
            return true;
        }
        if (!(object instanceof Task other)){
            return false;
        }
        return id != null && Objects.equals(id, other.id);
    }
    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
