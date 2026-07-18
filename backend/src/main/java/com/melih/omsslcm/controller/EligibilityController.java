package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.EligibilityDto;
import com.melih.omsslcm.exception.ResourceNotFoundException;
import com.melih.omsslcm.repository.SubscriberProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eligibility")
@RequiredArgsConstructor
@Tag(name = "Eligibility", description = "Subscriber eligibility lookups")
public class EligibilityController {

    private final SubscriberProfileRepository subscriberProfileRepository;

    @GetMapping("/{msisdn}")
    @Operation(summary = "Look up a subscriber's type and status by MSISDN")
    public EligibilityDto getEligibility(@PathVariable String msisdn) {
        return subscriberProfileRepository.findById(msisdn)
                .map(EligibilityDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found: " + msisdn));
    }
}
