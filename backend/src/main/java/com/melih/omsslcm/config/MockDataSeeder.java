package com.melih.omsslcm.config;

import com.melih.omsslcm.domain.ProductCatalog;
import com.melih.omsslcm.domain.SubscriberProfile;
import com.melih.omsslcm.domain.enums.SubscriberStatus;
import com.melih.omsslcm.domain.enums.SubscriberType;
import com.melih.omsslcm.domain.enums.TargetSegment;
import com.melih.omsslcm.repository.ProductCatalogRepository;
import com.melih.omsslcm.repository.SubscriberProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds baseline mock data on startup. Idempotent (checked by row count) so it
 * is safe to run again against a persisted SQLite volume across restarts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MockDataSeeder implements CommandLineRunner {

    private final SubscriberProfileRepository subscriberProfileRepository;
    private final ProductCatalogRepository productCatalogRepository;

    @Override
    public void run(String... args) {
        seedSubscribers();
        seedProducts();
    }

    private void seedSubscribers() {
        if (subscriberProfileRepository.count() > 0) {
            return;
        }
        subscriberProfileRepository.saveAll(List.of(
                SubscriberProfile.builder()
                        .msisdn("5551112233")
                        .type(SubscriberType.B2C)
                        .status(SubscriberStatus.ACTIVE)
                        .build(),
                SubscriberProfile.builder()
                        .msisdn("2125556677")
                        .type(SubscriberType.B2B)
                        .status(SubscriberStatus.ACTIVE)
                        .build(),
                SubscriberProfile.builder()
                        .msisdn("5050000000")
                        .type(SubscriberType.B2C)
                        .status(SubscriberStatus.BARRED)
                        .build()
        ));
        log.info("Seeded {} subscriber profiles", subscriberProfileRepository.count());
    }

    private void seedProducts() {
        if (productCatalogRepository.count() > 0) {
            return;
        }
        productCatalogRepository.saveAll(List.of(
                ProductCatalog.builder()
                        .productCode("VOL-30GB")
                        .name("Her Yöne 30GB")
                        .targetSegment(TargetSegment.B2C)
                        .price(new BigDecimal("149.90"))
                        .build(),
                ProductCatalog.builder()
                        .productCode("VOL-100GB")
                        .name("Kurumsal 100GB Data Paketi")
                        .targetSegment(TargetSegment.B2B)
                        .price(new BigDecimal("499.90"))
                        .build(),
                ProductCatalog.builder()
                        .productCode("SES-1000DK")
                        .name("1000 Dakika Konuşma Paketi")
                        .targetSegment(TargetSegment.ALL)
                        .price(new BigDecimal("89.90"))
                        .build(),
                ProductCatalog.builder()
                        .productCode("ROAM-EU")
                        .name("Avrupa Roaming Paketi")
                        .targetSegment(TargetSegment.ALL)
                        .price(new BigDecimal("199.90"))
                        .build(),
                ProductCatalog.builder()
                        .productCode("B2B-FILO-10")
                        .name("Filo Yönetim Paketi 10 Hat")
                        .targetSegment(TargetSegment.B2B)
                        .price(new BigDecimal("999.90"))
                        .build()
        ));
        log.info("Seeded {} products", productCatalogRepository.count());
    }
}
