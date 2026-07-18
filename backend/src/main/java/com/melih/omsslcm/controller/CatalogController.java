package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.ProductDto;
import com.melih.omsslcm.repository.ProductCatalogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog")
public class CatalogController {

    private final ProductCatalogRepository productCatalogRepository;

    @GetMapping("/products")
    @Operation(summary = "List all products in the catalog")
    public List<ProductDto> listProducts() {
        return productCatalogRepository.findAll().stream()
                .map(ProductDto::from)
                .toList();
    }
}
