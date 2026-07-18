package com.melih.omsslcm.dto;

import com.melih.omsslcm.domain.SubscriberProfile;

public record EligibilityDto(String msisdn, String type, String status) {

    public static EligibilityDto from(SubscriberProfile subscriber) {
        return new EligibilityDto(subscriber.getMsisdn(), subscriber.getType().name(), subscriber.getStatus().name());
    }
}
