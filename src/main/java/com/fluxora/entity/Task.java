package com.fluxora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Task() {}

    public Task(String title, String description){
        this.title = requireTitle(title);
        this.description = description;
        this.completed = false;
    }
    public void assignTo(User user){
        if (user == null){
            throw new IllegalArgumentException("Task owner is mandatory");
        }
        this.user = user;
    }
    public User getUser(){
        return user;
    }

    @PrePersist
    protected void onCreate(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }
    public void completed(){
        this.completed = true;
    }
    public void reopen(){
        this.completed = false;
    }
    public void updateDetails(String title, String description){
        this.title = requireTitle(title);
        this.description = description;
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
    public boolean isCompleted(){
        return completed;
    }
    public LocalDateTime getCreatedAt(){
        return  createdAt;
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
