package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.AuthResponseDto;
import com.melih.omsslcm.dto.SigninRequest;
import com.melih.omsslcm.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite::memory:")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    @Test
    void signupThenSigninGrantsAccessToProtectedEndpoint() {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        ResponseEntity<Void> signupResponse = restTemplate.postForEntity(
                url("/auth/signup"), new SignupRequest(email, "password123"), Void.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AuthResponseDto> signinResponse = restTemplate.postForEntity(
                url("/auth/signin"), new SigninRequest(email, "password123"), AuthResponseDto.class);
        assertThat(signinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = signinResponse.getBody().token();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                url("/catalog/products"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/catalog/products"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signinWithWrongPasswordFails() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(url("/auth/signup"), new SignupRequest(email, "password123"), Void.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/signin"), new SigninRequest(email, "wrongpassword"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateSignupFails() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(url("/auth/signup"), new SignupRequest(email, "password123"), Void.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/signup"), new SignupRequest(email, "password123"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
