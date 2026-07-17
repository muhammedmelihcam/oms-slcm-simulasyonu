package com.melih.omsslcm.service;

import com.melih.omsslcm.domain.OrderHistory;
import com.melih.omsslcm.domain.enums.OrderStatus;
import com.melih.omsslcm.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Intake: writes the PENDING row and returns immediately (synchronous).
 * Eligibility/business-rule checks happen later, in the async validation step
 * (OrderProcessingService) — intake itself does not check whether the msisdn
 * or productCode even exist.
 */
@Service
@RequiredArgsConstructor
public class OrderIntakeService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderProcessingService orderProcessingService;

    public OrderHistory createOrder(String msisdn, String productCode) {
        OrderHistory order = OrderHistory.builder()
                .orderId(UUID.randomUUID().toString())
                .msisdn(msisdn)
                .productCode(productCode)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        order = orderHistoryRepository.save(order);

        orderProcessingService.processOrder(order.getOrderId());

        return order;
    }
}
