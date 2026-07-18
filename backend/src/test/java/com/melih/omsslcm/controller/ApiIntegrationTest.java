package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.CreateOrderRequest;
import com.melih.omsslcm.dto.EligibilityDto;
import com.melih.omsslcm.dto.OrderStatusDto;
import com.melih.omsslcm.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the 4 REST endpoints over real HTTP against an isolated
 * in-memory SQLite instance, the same pattern as OrderProcessingServiceTest.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite::memory:")
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    @Test
    void listsSeededProducts() {
        ResponseEntity<ProductDto[]> response = restTemplate.getForEntity(url("/catalog/products"), ProductDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(ProductDto::productCode).contains("VOL-30GB");
    }

    @Test
    void returnsEligibilityForKnownSubscriber() {
        ResponseEntity<EligibilityDto> response = restTemplate.getForEntity(url("/eligibility/5551112233"), EligibilityDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().type()).isEqualTo("B2C");
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void returns404ForUnknownSubscriber() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/eligibility/0000000000"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createsOrderAndReachesTerminalStatus() {
        CreateOrderRequest request = new CreateOrderRequest("5551112233", "SES-1000DK");

        ResponseEntity<OrderStatusDto> createResponse = restTemplate.postForEntity(url("/orders"), request, OrderStatusDto.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(createResponse.getBody().status()).isEqualTo("PENDING");

        OrderStatusDto finalStatus = awaitTerminalStatus(createResponse.getBody().orderId());
        assertThat(finalStatus.status()).isIn("COMPLETED", "FAILED");
    }

    @Test
    void returns404ForUnknownOrder() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/orders/does-not-exist"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private OrderStatusDto awaitTerminalStatus(String orderId) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            OrderStatusDto status = restTemplate.getForObject(url("/orders/" + orderId), OrderStatusDto.class);
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
