package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.AuthResponseDto;
import com.melih.omsslcm.dto.CreateOrderRequest;
import com.melih.omsslcm.dto.EligibilityDto;
import com.melih.omsslcm.dto.OrderStatusDto;
import com.melih.omsslcm.dto.ProductDto;
import com.melih.omsslcm.dto.SigninRequest;
import com.melih.omsslcm.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the 4 OMS/SLCM REST endpoints over real HTTP against an isolated
 * in-memory SQLite instance, the same pattern as OrderProcessingServiceTest.
 * All of them sit behind AuthInterceptor now, so every request carries a
 * bearer token obtained via signup+signin in @BeforeEach.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite::memory:")
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders;

    @BeforeEach
    void signUpAndSignIn() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(url("/auth/signup"), new SignupRequest(email, "password123"), Void.class);
        AuthResponseDto auth = restTemplate.postForObject(
                url("/auth/signin"), new SigninRequest(email, "password123"), AuthResponseDto.class);

        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(auth.token());
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private <T> ResponseEntity<T> authGet(String path, Class<T> responseType) {
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(authHeaders), responseType);
    }

    private <T> ResponseEntity<T> authPost(String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, authHeaders), responseType);
    }

    @Test
    void listsSeededProducts() {
        ResponseEntity<ProductDto[]> response = authGet("/catalog/products", ProductDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(ProductDto::productCode).contains("VOL-30GB");
    }

    @Test
    void returnsEligibilityForKnownSubscriber() {
        ResponseEntity<EligibilityDto> response = authGet("/eligibility/5551112233", EligibilityDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().type()).isEqualTo("B2C");
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void returns404ForUnknownSubscriber() {
        ResponseEntity<String> response = authGet("/eligibility/0000000000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createsOrderAndReachesTerminalStatus() {
        CreateOrderRequest request = new CreateOrderRequest("5551112233", "SES-1000DK");

        ResponseEntity<OrderStatusDto> createResponse = authPost("/orders", request, OrderStatusDto.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(createResponse.getBody().status()).isEqualTo("PENDING");

        OrderStatusDto finalStatus = awaitTerminalStatus(createResponse.getBody().orderId());
        assertThat(finalStatus.status()).isIn("COMPLETED", "FAILED");
    }

    @Test
    void returns404ForUnknownOrder() {
        ResponseEntity<String> response = authGet("/orders/does-not-exist", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private OrderStatusDto awaitTerminalStatus(String orderId) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            OrderStatusDto status = authGet("/orders/" + orderId, OrderStatusDto.class).getBody();
            if ("COMPLETED".equals(status.status()) || "FAILED".equals(status.status())) {
                return status;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Order " + orderId + " did not reach a terminal status within timeout");
    }
}
