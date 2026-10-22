package com.fluxora.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Project(){}

    public Project(String name, String description) {
        this.name = validateName(name);
        this.description = description;
    }

    public void assignTo(User user){
        if (user == null){
            throw new IllegalArgumentException("Project owner is mandatory");
        }
        this.user = user;
    }

    public void updateDetails(String name, String description){
        this.name = validateName(name);
        this.description = description;
    }

    private String validateName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Project name is mandatory");
        }
        String normalizedName = name.trim();

        if (normalizedName.length() > 100){
            throw new IllegalArgumentException("Project name must have a maximum of 100 characters");
        }
        return normalizedName;
    }
    @PrePersist
    protected void onCreate(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        Project project = (Project) obj;

        return id != null && Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
