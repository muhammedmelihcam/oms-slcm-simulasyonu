package com.melih.omsslcm.service;

import com.melih.omsslcm.domain.OrderHistory;
import com.melih.omsslcm.domain.ProductCatalog;
import com.melih.omsslcm.domain.SubscriberProfile;
import com.melih.omsslcm.domain.enums.OrderStatus;
import com.melih.omsslcm.domain.enums.SubscriberStatus;
import com.melih.omsslcm.domain.enums.TargetSegment;
import com.melih.omsslcm.repository.ActiveSubscriptionRepository;
import com.melih.omsslcm.repository.OrderHistoryRepository;
import com.melih.omsslcm.repository.ProductCatalogRepository;
import com.melih.omsslcm.repository.SubscriberProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs the PENDING -> VALIDATING -> PROVISIONING -> COMPLETED/FAILED order
 * state machine in the background. State writes go through OrderStateStore
 * (short, independent transactions); the provisioning Thread.sleep() below
 * deliberately happens outside of any transaction. The Hikari pool is capped
 * at a single connection (SQLite is single-writer), so holding a connection
 * open across a multi-second sleep would serialize every other DB access —
 * concurrent orders, and eventually the order-status polling endpoint —
 * behind that sleep.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final SubscriberProfileRepository subscriberProfileRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final ActiveSubscriptionRepository activeSubscriptionRepository;
    private final OrderStateStore orderStateStore;

    @Async("orderProcessingExecutor")
    public CompletableFuture<Void> processOrder(String orderId) {
        try {
            OrderHistory order = orderHistoryRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
            String msisdn = order.getMsisdn();
            String productCode = order.getProductCode();

            orderStateStore.updateStatus(orderId, OrderStatus.VALIDATING, null);

            String failureReason = validate(msisdn, productCode);
            if (failureReason != null) {
                orderStateStore.updateStatus(orderId, OrderStatus.FAILED, failureReason);
                return CompletableFuture.completedFuture(null);
            }

            orderStateStore.updateStatus(orderId, OrderStatus.PROVISIONING, null);

            // Simulates the network/billing provisioning round-trip.
            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001));

            orderStateStore.complete(orderId, msisdn, productCode);
        } catch (Exception e) {
            log.error("Order {} processing failed unexpectedly", orderId, e);
            orderStateStore.updateStatus(orderId, OrderStatus.FAILED, "Internal error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns null when the order passes all rules, otherwise the failure reason.
     */
    private String validate(String msisdn, String productCode) {
        Optional<SubscriberProfile> subscriber = subscriberProfileRepository.findById(msisdn);
        if (subscriber.isEmpty()) {
            return "Subscriber not found";
        }

        Optional<ProductCatalog> product = productCatalogRepository.findById(productCode);
        if (product.isEmpty()) {
            return "Product not found";
        }

        // Rule 1: barred subscribers cannot order anything.
        if (subscriber.get().getStatus() == SubscriberStatus.BARRED) {
            return "MSISDN is barred";
        }

        // Rule 2 (eligibility): product's target segment must match the subscriber type, unless it targets ALL.
        TargetSegment segment = product.get().getTargetSegment();
        if (segment != TargetSegment.ALL && !segment.name().equals(subscriber.get().getType().name())) {
            return "Product not eligible for subscriber segment";
        }

        // Rule 3: no duplicate active subscription to the same product.
        if (activeSubscriptionRepository.existsByMsisdnAndProductCode(msisdn, productCode)) {
            return "Subscriber already has an active subscription to this product";
        }

        return null;
    }
}
