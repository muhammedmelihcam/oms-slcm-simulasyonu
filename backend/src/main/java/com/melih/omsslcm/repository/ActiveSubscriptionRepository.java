package com.melih.omsslcm.repository;

import com.melih.omsslcm.domain.ActiveSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActiveSubscriptionRepository extends JpaRepository<ActiveSubscription, Long> {

    boolean existsByMsisdnAndProductCode(String msisdn, String productCode);

    List<ActiveSubscription> findByMsisdn(String msisdn);
}
