package com.melih.omsslcm.repository;

import com.melih.omsslcm.domain.ProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, String> {
}
