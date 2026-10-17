package com.fluxora.controller;

import com.fluxora.dto.LoginRequest;
import com.fluxora.dto.LoginResponse;
import com.fluxora.dto.UserRegistrationRequest;
import com.fluxora.dto.UserResponse;
import com.fluxora.entity.User;
import com.fluxora.service.AuthService;
import com.fluxora.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    private final AuthService authService;

    public UserController(UserService userService, AuthService authService){

        this.userService = userService;
        this.authService = authService;

    }
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request){
        User user = userService.register(request.name(), request.email(), request.password());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }
}
