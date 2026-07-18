package com.melih.omsslcm.dto;

import com.melih.omsslcm.domain.OrderHistory;

public record OrderStatusDto(String orderId, String status, String reason) {

    public static OrderStatusDto from(OrderHistory order) {
        return new OrderStatusDto(order.getOrderId(), order.getStatus().name(), order.getFailureReason());
    }
}
