package com.fluxora.service;

import com.fluxora.dto.LoginRequest;
import com.fluxora.dto.LoginResponse;
import com.fluxora.entity.User;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private UserRepository userRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp(){
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }
    @Test
    @DisplayName("must authenticate the user and return a JWT")
    void test_01(){
        User user = new User(
                "User test",
                "test@fluxora.com",
                "$2a$10$hashPasswordTest"
        );
        when(userRepository.findByEmail("test@fluxora.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passwordStrong123!", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken("test@fluxora.com")).thenReturn("jwt-de-teste");

        LoginResponse response = authService.login(new LoginRequest(
                "test@fluxora.com", "passwordStrong123!"));

        assertEquals("jwt-de-teste", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400, response.expiresIn());

        verify(userRepository).findByEmail("test@fluxora.com");
        verify(passwordEncoder).matches("passwordStrong123!", user.getPasswordHash());
        verify(jwtService).generateToken("test@fluxora.com");
    }
    @Test
    @DisplayName("Must reject an incorrect password.")
    void test_02(){
        User user = new User(
                "User test",
                "test@fluxora.com",
                "$2a$10$hashPasswordTest"
        );
        when(userRepository.findByEmail("test@fluxora.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passwordWrong", user.getPasswordHash())).thenReturn(false);

        assertThrows(BadCredentialsException.class,() -> authService.login(
                new LoginRequest("test@fluxora.com", "passwordWrong")));
        verify(jwtService, never()).generateToken(anyString());
    }
    @Test
    @DisplayName("Should reject a non-existent email address")
    void test_03(){
        when(userRepository.findByEmail("notExist@fluxora.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,() -> authService.login(
                new LoginRequest("notExist@fluxora.com", "passwordWrong")));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString());
    }

}
