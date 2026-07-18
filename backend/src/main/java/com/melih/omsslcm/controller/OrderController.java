package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.CreateOrderRequest;
import com.melih.omsslcm.dto.OrderStatusDto;
import com.melih.omsslcm.exception.ResourceNotFoundException;
import com.melih.omsslcm.repository.OrderHistoryRepository;
import com.melih.omsslcm.service.OrderIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order intake and status polling")
public class OrderController {

    private final OrderIntakeService orderIntakeService;
    private final OrderHistoryRepository orderHistoryRepository;

    @PostMapping
    @Operation(summary = "Create a new order; validation and provisioning run asynchronously in the background")
    public ResponseEntity<OrderStatusDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderIntakeService.createOrder(request.msisdn(), request.productCode());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderStatusDto.from(order));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Poll the current status of an order")
    public OrderStatusDto getOrder(@PathVariable String orderId) {
        return orderHistoryRepository.findById(orderId)
                .map(OrderStatusDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
