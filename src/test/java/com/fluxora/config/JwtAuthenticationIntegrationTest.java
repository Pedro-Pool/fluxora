package com.fluxora.config;

import com.fluxora.entity.User;
import com.fluxora.repository.UserRepository;
import com.fluxora.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtAuthenticationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("Should return 401 when the token is missing")
    void test_01() throws Exception{
        mockMvc.perform(get("/api/tasks/"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("Should return 401 when the token is invalid")
    void test_02() throws Exception{
        mockMvc.perform(get("/api/tasks/")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-invalid"
                ))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("Should return 401 when the token is expired")
    void test_03() throws Exception{
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                        .subject("test@fluxora.com")
                        .issuedAt(Date.from(now.minusSeconds(120)))
                        .expiration(Date.from(now.minusSeconds(60)))
                        .signWith(key)
                        .compact();

        mockMvc.perform(get("/api/tasks/")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer "+ expiredToken
                )).andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("Should allow access when the token is valid")
    void test_04() throws Exception{
        String email = "jwt-test-"+ UUID.randomUUID()+"fluxora.com";

        User user = userRepository.saveAndFlush(
                new User("JWT Integration Test",
                        email,
                        passwordEncoder.encode("test-password")
                )
        );
        try{
            String token = jwtService.generateToken(email);
            mockMvc.perform(get("/api/tasks")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer "+ token
                    ))
                    .andExpect(status().isOk());
        }finally {
            userRepository.delete(user);
        }
    }
}
