package com.melih.omsslcm.service;

import com.melih.omsslcm.domain.ActiveSubscription;
import com.melih.omsslcm.domain.OrderHistory;
import com.melih.omsslcm.domain.enums.OrderStatus;
import com.melih.omsslcm.repository.ActiveSubscriptionRepository;
import com.melih.omsslcm.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * State-transition writes for OrderProcessingService, split into its own
 * Spring bean so @Transactional actually applies: calling an @Transactional
 * method on `this` from within the same class bypasses Spring's proxy and
 * silently runs without a transaction. Each method here is a short,
 * independent transaction and none of them ever wrap the provisioning
 * Thread.sleep() in OrderProcessingService.
 */
@Component
@RequiredArgsConstructor
class OrderStateStore {

    private final OrderHistoryRepository orderHistoryRepository;
    private final ActiveSubscriptionRepository activeSubscriptionRepository;

    @Transactional
    void updateStatus(String orderId, OrderStatus status, String failureReason) {
        OrderHistory order = orderHistoryRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
        order.setStatus(status);
        order.setFailureReason(failureReason);
        orderHistoryRepository.save(order);
    }

    @Transactional
    void complete(String orderId, String msisdn, String productCode) {
        activeSubscriptionRepository.save(ActiveSubscription.builder()
                .msisdn(msisdn)
                .productCode(productCode)
                .orderId(orderId)
                .activatedAt(LocalDateTime.now())
                .build());

        OrderHistory order = orderHistoryRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
        order.setStatus(OrderStatus.COMPLETED);
        orderHistoryRepository.save(order);
    }
}
