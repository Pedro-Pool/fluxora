package com.fluxora.service;


import com.fluxora.entity.User;
import com.fluxora.exception.EmailAlreadyRegisteredException;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should successfully register a user")
    void test_01(){
        String name = "User Test";
        String email = "test@fluxora.com";
        String password = "passwordStrong123!";
        String passwordHash = "$2a$10$hashDeTeste";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(passwordHash);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });
        User user = userService.register(name, email, password);

        assertNotNull(user);
        assertEquals(name, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }
    @Test
    @DisplayName("A user with a duplicate email address should not be registered.")
    void test_02(){
        String email = "test@fluxora.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> userService.register("User test", email, "passwordStrong123!"));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
    @Test
    @DisplayName("Should normalize the email address before checking for duplicates.")
    void test_03(){
        when(userRepository.existsByEmail("test@fluxora.com")).thenReturn(false);
        when(passwordEncoder.encode("passwordStrong123!")).thenReturn("$2a$10$hashDeTeste");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(
                "User test",
                "  TEST@FLUXORA.COM",
                "passwordStrong123!"
        );
        assertEquals("test@fluxora.com", user.getEmail());
        verify(userRepository).existsByEmail("test@fluxora.com");
    }
}
