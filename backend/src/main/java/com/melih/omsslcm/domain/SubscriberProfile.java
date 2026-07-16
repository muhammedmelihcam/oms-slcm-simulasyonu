package com.melih.omsslcm.domain;

import com.melih.omsslcm.domain.enums.SubscriberStatus;
import com.melih.omsslcm.domain.enums.SubscriberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SUBSCRIBER_PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriberProfile {

    @Id
    @Column(name = "msisdn")
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberStatus status;
}
