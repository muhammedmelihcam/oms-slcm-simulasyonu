package com.melih.omsslcm.dto;

import com.melih.omsslcm.domain.ActiveSubscription;
import com.melih.omsslcm.domain.ProductCatalog;

import java.time.LocalDateTime;

public record ActiveSubscriptionDto(String productCode, String productName, LocalDateTime activatedAt) {

    public static ActiveSubscriptionDto from(ActiveSubscription subscription, ProductCatalog product) {
        String productName = product != null ? product.getName() : subscription.getProductCode();
        return new ActiveSubscriptionDto(subscription.getProductCode(), productName, subscription.getActivatedAt());
    }
}
