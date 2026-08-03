package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.ActiveSubscriptionDto;
import com.melih.omsslcm.repository.ActiveSubscriptionRepository;
import com.melih.omsslcm.repository.ProductCatalogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Active subscriptions for a subscriber")
public class SubscriptionController {

    private final ActiveSubscriptionRepository activeSubscriptionRepository;
    private final ProductCatalogRepository productCatalogRepository;

    @GetMapping("/{msisdn}")
    @Operation(summary = "List active subscriptions for an MSISDN")
    public List<ActiveSubscriptionDto> listActiveSubscriptions(@PathVariable String msisdn) {
        return activeSubscriptionRepository.findByMsisdn(msisdn).stream()
                .map(sub -> ActiveSubscriptionDto.from(sub, productCatalogRepository.findById(sub.getProductCode()).orElse(null)))
                .toList();
    }
}
