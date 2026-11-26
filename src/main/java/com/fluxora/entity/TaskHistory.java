package com.fluxora.entity;


import com.fluxora.enums.TaskHistoryAction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskHistoryAction action;

    @Column(name = "field_name", length = 100)
    private String fieldName;


    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TaskHistory(){}

    public TaskHistory(
            Task task,
            User user,
            TaskHistoryAction action,
            String fieldName,
            String oldValue,
            String newValue) {

        if (task == null){
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (user == null){
            throw new IllegalArgumentException("User cannot be null");
        }
        if (action == null){
            throw new IllegalArgumentException("Action cannot be null");
        }

        this.task = task;
        this.user = user;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    @PrePersist
    protected void onCrate(){
        this.createdAt = LocalDateTime.now();
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

    public TaskHistoryAction getAction() {
        return action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
