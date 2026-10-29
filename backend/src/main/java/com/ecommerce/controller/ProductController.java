package com.ecommerce.controller;

import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CatalogDtos.CategoryResponse>> getCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    @GetMapping("/products")
    public ResponseEntity<CatalogDtos.PagedResponse<CatalogDtos.ProductResponse>> getProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "activeOnly", defaultValue = "true") Boolean activeOnly,
            @RequestParam(name = "sortBy", defaultValue = "newest") String sortBy,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {

        CatalogDtos.PagedResponse<CatalogDtos.ProductResponse> result =
                catalogService.getProducts(keyword, categoryId, minPrice, maxPrice, activeOnly, sortBy, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogDtos.ProductResponse> getProductById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(catalogService.getProductById(id));
    }
}

