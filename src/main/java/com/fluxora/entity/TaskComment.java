package com.fluxora.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_comments")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaskComment(){}

    public TaskComment(Task task, User user, String content) {

        if (task == null){
            throw new IllegalArgumentException("Comment task is mandatory");
        }
        if (user == null){
            throw new IllegalArgumentException("Comment author is mandatory");
        }

        this.task = task;
        this.user = user;
        this.content = normalizeContent(content);
    }
    public void updateContent(String content){
        this.content = normalizeContent(content);
    }

    private String normalizeContent(String content){
        if (content == null || content.isBlank()){
            throw new IllegalArgumentException("Content is required");
        }

        String normalizedContent = content.trim();

        if (normalizedContent.length() > 5000){
            throw new IllegalArgumentException("Comment content must have a maximum 5000 characters");
        }

        return normalizedContent;
    }

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public User getUser() {
        return user;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
