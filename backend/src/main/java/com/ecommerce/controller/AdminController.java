package com.ecommerce.controller;

import com.ecommerce.dto.AuthDtos;
import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.CatalogService;
import com.ecommerce.service.FileStorageService;
import com.ecommerce.service.OrderTrackingService;
import com.ecommerce.util.SessionAuthUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CatalogService catalogService;
    private final OrderTrackingService orderTrackingService;
    private final AuthService authService;
    private final FileStorageService fileStorageService;

    public AdminController(
            CatalogService catalogService,
            OrderTrackingService orderTrackingService,
            AuthService authService,
            FileStorageService fileStorageService) {
        this.catalogService = catalogService;
        this.orderTrackingService = orderTrackingService;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<OrderDtos.AdminDashboardResponse> getDashboard(HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.ok(orderTrackingService.getAdminDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AuthDtos.UserResponse>> getAllUsers(HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // Category Administration
    @PostMapping("/categories")
    public ResponseEntity<CatalogDtos.CategoryResponse> createCategory(
            @Valid @RequestBody CatalogDtos.CategoryRequest request,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CatalogDtos.CategoryResponse> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CatalogDtos.CategoryRequest request,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.ok(catalogService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable("id") Long id,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        catalogService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully."));
    }

    // Product Administration
    @PostMapping("/products")
    public ResponseEntity<CatalogDtos.ProductResponse> createProduct(
            @Valid @RequestBody CatalogDtos.ProductRequest request,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<CatalogDtos.ProductResponse> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody CatalogDtos.ProductRequest request,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @PathVariable("id") Long id,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        catalogService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product removed/deactivated successfully."));
    }

    @PostMapping("/products/{id}/image")
    public ResponseEntity<CatalogDtos.ProductResponse> uploadProductImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        String imagePath = fileStorageService.storeProductImage(file);
        return ResponseEntity.ok(catalogService.updateProductImage(id, imagePath));
    }

    @PostMapping("/uploads/image")
    public ResponseEntity<Map<String, String>> uploadStandaloneProductImage(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        String imagePath = fileStorageService.storeProductImage(file);
        return ResponseEntity.ok(Map.of("imagePath", imagePath));
    }

    // Order & Automated Tracking Administration
    @GetMapping("/orders")
    public ResponseEntity<CatalogDtos.PagedResponse<OrderDtos.OrderResponse>> getAllOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        OrderStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid order status filter: " + status);
            }
        }
        return ResponseEntity.ok(orderTrackingService.getAllOrdersForAdmin(statusFilter, page, size));
    }

    @RequestMapping(value = "/orders/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDtos.OrderResponse> updateOrderStatus(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody OrderDtos.OrderStatusUpdateRequest request,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + request.getStatus());
        }
        String actor = SessionAuthUtil.getSessionEmail(session);
        OrderDtos.OrderResponse updated = orderTrackingService.updateOrderStatus(
                orderId, targetStatus, request.getDescription(), request.getLocationNote(), actor);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/automation/toggle")
    public ResponseEntity<Map<String, Object>> toggleTrackingAutomation(
            @RequestBody Map<String, Boolean> body,
            HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        orderTrackingService.setAutomationEnabled(enabled);
        return ResponseEntity.ok(Map.of(
                "automationEnabled", orderTrackingService.isAutomationEnabled(),
                "message", enabled
                        ? "Simulated automated delivery progression enabled."
                        : "Simulated automated delivery progression disabled."
        ));
    }

    @PostMapping("/automation/step")
    public ResponseEntity<Map<String, Object>> triggerSimulationStepNow(HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        String actor = "Admin Demo Trigger (" + SessionAuthUtil.getSessionEmail(session) + ")";
        int advancedCount = orderTrackingService.advanceActiveOrdersInSimulationStep(actor);
        return ResponseEntity.ok(Map.of(
                "advancedOrdersCount", advancedCount,
                "message", advancedCount > 0
                        ? "Advanced " + advancedCount + " active order(s) to their next delivery stage."
                        : "No active orders were eligible to advance."
        ));
    }

    @GetMapping("/cache-stats")
    public ResponseEntity<OrderDtos.CacheMetricsDto> getCacheStatistics(HttpSession session) {
        SessionAuthUtil.requireAdmin(session);
        return ResponseEntity.ok(orderTrackingService.getCacheMetrics());
    }
}

