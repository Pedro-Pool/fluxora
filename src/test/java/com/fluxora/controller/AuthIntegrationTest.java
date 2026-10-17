package com.fluxora.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String authUrl(){
        return "http://localhost:"+ port +"/api/auth";
    }
    private String registerUser(){
        String email = UUID.randomUUID() + "@fluxora.com";
        Map<String, String> request = Map.of(
                "name", "User test",
                "email", email,
                "password", "passwordStrong123!"
        );
        ResponseEntity<String> response = restTemplate.postForEntity(
                authUrl()+"/register",
                request,
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return email;
    }
    private ResponseEntity<String> login(String email, String password){
        Map<String, String> request = Map.of(
                "email", email,
                "password", password
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                authUrl()+"/login",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );
    }
    @Test
    @DisplayName("Must authenticate the user and return a JWT")
    void test_01() throws Exception{
        String email = registerUser();
        ResponseEntity<String> response = login(email, "passwordStrong123!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isPositive();
    }
    @Test
    @DisplayName("Must reject an incorrect password.")
    void test_02(){
        registerUser();
        ResponseEntity<String> response = login("login@fluxora.com", "passwordWrong!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test
    @DisplayName("Should reject a non-existent email address")
    void test_03(){
        ResponseEntity<String> response = login("notExist@fluxora.com", "passwordWrong!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test
    @DisplayName("Must reject a request with an invalid email")
    void test_04(){
        ResponseEntity<String> response = login("invalidEmail-fluxora", "passwordWrong!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST   );
    }
}
