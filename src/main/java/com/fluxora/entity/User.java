package com.fluxora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(name = "uk_users_email", columnNames = "email")})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createAt;

    protected User(){}

    public User(String name, String email, String passwordHash){
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("The name is mandatory.");
        }
        if (name.length() > 100){
            throw new IllegalArgumentException("The name must have a maximum of 100 characters.");
        }
        if (email == null || email.isBlank()){
            throw new IllegalArgumentException("The email address is mandatory.");
        }
        if (email.length() > 254){
            throw new IllegalArgumentException("The email must have a maximum of 254 characters.");
        }
        if (passwordHash == null || passwordHash.isBlank()){
            throw new IllegalArgumentException("The password hash is mandatory.");
        }
        if (passwordHash.length() > 60){
            throw new IllegalArgumentException("The password hash is invalid.");
        }
        this.name = name.trim();
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
        this.passwordHash = passwordHash;
    }
    @PrePersist
    protected void onCreate(){
        createAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }
    @Override
    public boolean equals(Object object){
        if (this == object){
            return true;
        }
        if (object == null || getClass() != object.getClass()){
            return false;
        }
        User user = (User) object;

        return id != null && Objects.equals(id, user.id);
    }
    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
