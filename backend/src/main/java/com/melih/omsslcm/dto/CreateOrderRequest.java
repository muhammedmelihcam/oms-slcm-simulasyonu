package com.melih.omsslcm.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String msisdn,
        @NotBlank String productCode) {
}
