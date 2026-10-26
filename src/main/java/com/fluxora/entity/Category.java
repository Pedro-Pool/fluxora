package com.fluxora.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "categories",  uniqueConstraints = @UniqueConstraint(
        name = "uk_cateogory_name_user",
        columnNames = {"user_id", "name"}))
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Category(){}

    public Category(String name) {
        this.name = validateName(name);
    }
    public void assignTo(User user){
        if (user == null){
            throw new IllegalArgumentException("Category owner is mandatory");
        }
        this.user = user;
    }
    public void rename(String name){
        this.name = validateName(name);
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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        Category category = (Category) obj;

        return id != null && Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
