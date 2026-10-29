package com.ecommerce.service;

import com.ecommerce.dao.CategoryDao;
import com.ecommerce.dao.OrderDao;
import com.ecommerce.dao.ProductDao;
import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;

    public CatalogService(CategoryDao categoryDao, ProductDao productDao, OrderDao orderDao) {
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        this.orderDao = orderDao;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryResponse> getAllCategories() {
        return categoryDao.findAllCached().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " was not found."));
    }

    @Transactional
    public CatalogDtos.CategoryResponse createCategory(CatalogDtos.CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryDao.findByName(name).isPresent()) {
            throw new BadRequestException("A category named '" + name + "' already exists.");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        return toCategoryResponse(categoryDao.save(category));
    }

    @Transactional
    public CatalogDtos.CategoryResponse updateCategory(Long id, CatalogDtos.CategoryRequest request) {
        Category category = getCategoryEntity(id);
        String name = request.getName().trim();
        categoryDao.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Another category named '" + name + "' already exists.");
            }
        });
        category.setName(name);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        return toCategoryResponse(categoryDao.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryEntity(id);
        long count = categoryDao.countProductsInCategory(id);
        if (count > 0) {
            throw new BadRequestException("Cannot delete category '" + category.getName() + "' because it still contains " + count + " product(s).");
        }
        categoryDao.delete(category);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PagedResponse<CatalogDtos.ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean activeOnly,
            String sortBy,
            int page,
            int size) {

        List<CatalogDtos.ProductResponse> items = productDao.searchAndFilter(
                        keyword, categoryId, minPrice, maxPrice, activeOnly, sortBy, page, size)
                .stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        long total = productDao.countFiltered(keyword, categoryId, minPrice, maxPrice, activeOnly);
        return new CatalogDtos.PagedResponse<>(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductResponse getProductById(Long id) {
        Product product = productDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " was not found."));
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " was not found."));
    }

    @Transactional
    public CatalogDtos.ProductResponse createProduct(CatalogDtos.ProductRequest request) {
        Category category = getCategoryEntity(request.getCategoryId());
        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setImagePath(request.getImagePath() != null && !request.getImagePath().isBlank()
                ? request.getImagePath().trim()
                : "/api/uploads/fallback-product.svg");
        product.setActive(request.getActive() != null ? request.getActive() : true);
        return toProductResponse(productDao.save(product));
    }

    @Transactional
    public CatalogDtos.ProductResponse updateProduct(Long id, CatalogDtos.ProductRequest request) {
        Product product = getProductEntity(id);
        Category category = getCategoryEntity(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        if (request.getImagePath() != null && !request.getImagePath().isBlank()) {
            product.setImagePath(request.getImagePath().trim());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        return toProductResponse(productDao.save(product));
    }

    @Transactional
    public CatalogDtos.ProductResponse updateProductImage(Long id, String imagePath) {
        Product product = getProductEntity(id);
        product.setImagePath(imagePath);
        return toProductResponse(productDao.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductEntity(id);
        long orderRefs = orderDao.countOrderItemsByProductId(id);
        if (orderRefs > 0) {
            // Preserve order history integrity by soft-deactivating if already ordered
            product.setActive(false);
            product.setStockQuantity(0);
            productDao.save(product);
        } else {
            productDao.delete(product);
        }
    }

    public CatalogDtos.CategoryResponse toCategoryResponse(Category category) {
        CatalogDtos.CategoryResponse dto = new CatalogDtos.CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setProductCount(categoryDao.countProductsInCategory(category.getId()));
        return dto;
    }

    public CatalogDtos.ProductResponse toProductResponse(Product product) {
        CatalogDtos.ProductResponse dto = new CatalogDtos.ProductResponse();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        boolean inStock = Boolean.TRUE.equals(product.getActive()) && product.getStockQuantity() != null && product.getStockQuantity() > 0;
        dto.setAvailable(inStock);
        if (!Boolean.TRUE.equals(product.getActive())) {
            dto.setAvailabilityMessage("Discontinued / Currently Unavailable");
        } else if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            dto.setAvailabilityMessage("Out of Stock — Currently Unavailable");
        } else if (product.getStockQuantity() <= 5) {
            dto.setAvailabilityMessage("Low Stock — Only " + product.getStockQuantity() + " left!");
        } else {
            dto.setAvailabilityMessage("In Stock (" + product.getStockQuantity() + " available)");
        }
        dto.setImagePath(product.getImagePath() != null && !product.getImagePath().isBlank()
                ? product.getImagePath()
                : "/api/uploads/fallback-product.svg");
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setActive(product.getActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }
}

