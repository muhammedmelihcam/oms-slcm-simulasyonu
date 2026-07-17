package com.melih.omsslcm.service;

import com.melih.omsslcm.domain.ActiveSubscription;
import com.melih.omsslcm.domain.OrderHistory;
import com.melih.omsslcm.domain.ProductCatalog;
import com.melih.omsslcm.domain.SubscriberProfile;
import com.melih.omsslcm.domain.enums.OrderStatus;
import com.melih.omsslcm.domain.enums.SubscriberStatus;
import com.melih.omsslcm.domain.enums.SubscriberType;
import com.melih.omsslcm.domain.enums.TargetSegment;
import com.melih.omsslcm.repository.ActiveSubscriptionRepository;
import com.melih.omsslcm.repository.OrderHistoryRepository;
import com.melih.omsslcm.repository.ProductCatalogRepository;
import com.melih.omsslcm.repository.SubscriberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the PENDING -> VALIDATING -> PROVISIONING -> COMPLETED/FAILED
 * state machine end-to-end through OrderIntakeService, on an isolated
 * in-memory SQLite instance so it doesn't touch the dev data file.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
class OrderProcessingServiceTest {

    @Autowired
    private OrderIntakeService orderIntakeService;
    @Autowired
    private OrderHistoryRepository orderHistoryRepository;
    @Autowired
    private SubscriberProfileRepository subscriberProfileRepository;
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    @Autowired
    private ActiveSubscriptionRepository activeSubscriptionRepository;

    @Test
    void barredSubscriberOrderFails() {
        subscriberProfileRepository.save(SubscriberProfile.builder()
                .msisdn("5000000001").type(SubscriberType.B2C).status(SubscriberStatus.BARRED).build());
        productCatalogRepository.save(ProductCatalog.builder()
                .productCode("TEST-BARRED").name("Test Product").targetSegment(TargetSegment.ALL)
                .price(BigDecimal.TEN).build());

        OrderHistory order = orderIntakeService.createOrder("5000000001", "TEST-BARRED");

        OrderHistory result = awaitTerminalStatus(order.getOrderId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("MSISDN is barred");
    }

    @Test
    void segmentMismatchFails() {
        subscriberProfileRepository.save(SubscriberProfile.builder()
                .msisdn("5000000002").type(SubscriberType.B2C).status(SubscriberStatus.ACTIVE).build());
        productCatalogRepository.save(ProductCatalog.builder()
                .productCode("TEST-B2B").name("Test B2B Product").targetSegment(TargetSegment.B2B)
                .price(BigDecimal.TEN).build());

        OrderHistory order = orderIntakeService.createOrder("5000000002", "TEST-B2B");

        OrderHistory result = awaitTerminalStatus(order.getOrderId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Product not eligible for subscriber segment");
    }

    @Test
    void duplicateActiveSubscriptionFails() {
        subscriberProfileRepository.save(SubscriberProfile.builder()
                .msisdn("5000000003").type(SubscriberType.B2C).status(SubscriberStatus.ACTIVE).build());
        productCatalogRepository.save(ProductCatalog.builder()
                .productCode("TEST-DUP").name("Test Dup Product").targetSegment(TargetSegment.ALL)
                .price(BigDecimal.TEN).build());
        activeSubscriptionRepository.save(ActiveSubscription.builder()
                .msisdn("5000000003").productCode("TEST-DUP").orderId(UUID.randomUUID().toString())
                .activatedAt(LocalDateTime.now()).build());

        OrderHistory order = orderIntakeService.createOrder("5000000003", "TEST-DUP");

        OrderHistory result = awaitTerminalStatus(order.getOrderId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Subscriber already has an active subscription to this product");
    }

    @Test
    void eligibleOrderCompletesAndActivatesSubscription() {
        subscriberProfileRepository.save(SubscriberProfile.builder()
                .msisdn("5000000004").type(SubscriberType.B2C).status(SubscriberStatus.ACTIVE).build());
        productCatalogRepository.save(ProductCatalog.builder()
                .productCode("TEST-OK").name("Test OK Product").targetSegment(TargetSegment.ALL)
                .price(BigDecimal.TEN).build());

        OrderHistory order = orderIntakeService.createOrder("5000000004", "TEST-OK");

        OrderHistory result = awaitTerminalStatus(order.getOrderId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(activeSubscriptionRepository.existsByMsisdnAndProductCode("5000000004", "TEST-OK")).isTrue();
    }

    private OrderHistory awaitTerminalStatus(String orderId) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            OrderHistory order = orderHistoryRepository.findById(orderId).orElseThrow();
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.FAILED) {
                return order;
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
