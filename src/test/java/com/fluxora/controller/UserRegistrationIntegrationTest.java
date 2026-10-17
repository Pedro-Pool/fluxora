package com.fluxora.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxora.entity.User;
import com.fluxora.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserRegistrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("fluxora_test")
                    .withUsername("test")
                    .withPassword("test");
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Must register the user and store the password using BCrypt.")
    void test_01() throws Exception{
        String email = "integracao@fluxora.com";
        String password = "passwordStrong123!";

        Map<String, String> request = Map.of(
                "name", "User integration",
                "email", email,
                "password", password
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:"+ port + "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.has("id")).isTrue();
        assertThat(body.get("name").asText()).isEqualTo("User integration");
        assertThat(body.get("email").asText()).isEqualTo(email);
        assertThat(body.has("password")).isFalse();
        assertThat(body.has("passwordHash")).isFalse();
        assertThat(body.has("password_hash")).isFalse();

        User user = userRepository.findByEmail(email).orElseThrow();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getPasswordHash()).isNotEqualTo(password);
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();
    }
    @Test
    @DisplayName("Should return 409 when registering an email that already exists")
    void test_02(){
        String email = "duplicate@fluxora.com";

        Map<String, String> request = Map.of(
                "name", "User duplicate",
                "email", email,
                "password", "passwordStrong123!"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        String url = "http://localhost:"+ port +"/api/auth/register";

        ResponseEntity<String> firstResponse = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> secundResponse = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
        );
        assertThat(secundResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .count())
                .isEqualTo(1);
    }
    @Test
    @DisplayName("Must reject registration with an invalid email")
    void test_03(){
        Map<String, String> request = Map.of(
                "name", "User test",
                "email", "email-invalid",
                "password", "passwordStrong123!"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:"+ port +"/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test
    @DisplayName("Must reject registration with an empty name")
    void test_04(){
        Map<String, String> request = Map.of(
                "name", "",
                "email", "integracao@fluxora.com",
                "password", "passwordStrong123!"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:"+ port +"/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test
    @DisplayName("Must reject registration with a short password")
    void test_05(){
        Map<String, String> request = Map.of(
                "name", "User test",
                "email", "integracao@fluxora.com",
                "password", "1234567"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:"+ port +"/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
