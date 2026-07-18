package com.melih.omsslcm.dto;

import com.melih.omsslcm.domain.ProductCatalog;

public record ProductDto(String productCode, String name, String segment) {

    public static ProductDto from(ProductCatalog product) {
        return new ProductDto(product.getProductCode(), product.getName(), product.getTargetSegment().name());
    }
}
