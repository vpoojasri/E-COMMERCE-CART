package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class OrderDtos {

    public static class AddToCartRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class UpdateCartItemRequest {
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class CartItemDto {
        private Long id;
        private Long productId;
        private String productName;
        private String imagePath;
        private BigDecimal unitPrice;
        private Integer quantity;
        private Integer availableStock;
        private BigDecimal subtotal;
        private boolean stockValid;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getAvailableStock() { return availableStock; }
        public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public boolean isStockValid() { return stockValid; }
        public void setStockValid(boolean stockValid) { this.stockValid = stockValid; }
    }

    public static class CartResponse {
        private List<CartItemDto> items;
        private int totalItems;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal totalAmount;
        private boolean allItemsInStock;

        public List<CartItemDto> getItems() { return items; }
        public void setItems(List<CartItemDto> items) { this.items = items; }
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getShippingFee() { return shippingFee; }
        public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public boolean isAllItemsInStock() { return allItemsInStock; }
        public void setAllItemsInStock(boolean allItemsInStock) { this.allItemsInStock = allItemsInStock; }
    }

    public static class PlaceOrderRequest {
        @NotBlank(message = "Shipping address is required")
        @Size(min = 5, max = 500, message = "Shipping address must be between 5 and 500 characters")
        private String shippingAddress;

        @NotBlank(message = "Contact phone is required")
        @Size(min = 6, max = 40, message = "Contact phone must be between 6 and 40 characters")
        private String contactPhone;

        private String paymentMethod = "SIMULATED_CARD_DEMO";

        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class OrderStatusUpdateRequest {
        @NotBlank(message = "Target status is required")
        private String status;

        private String description;
        private String locationNote;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLocationNote() { return locationNote; }
        public void setLocationNote(String locationNote) { this.locationNote = locationNote; }
    }

    public static class CancelOrderRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productNameSnapshot;
        private String imagePath;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductNameSnapshot() { return productNameSnapshot; }
        public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    }

    public static class TrackingEventResponse {
        private Long id;
        private String status;
        private String description;
        private String locationNote;
        private LocalDateTime updatedAt;
        private String updatedBy;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLocationNote() { return locationNote; }
        public void setLocationNote(String locationNote) { this.locationNote = locationNote; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    }

    public static class OrderResponse {
        private Long id;
        private String orderNumber;
        private String trackingNumber;
        private Long userId;
        private String customerName;
        private String customerEmail;
        private LocalDateTime orderDate;
        private BigDecimal totalAmount;
        private String shippingAddress;
        private String contactPhone;
        private LocalDate estimatedDeliveryDate;
        private String currentStatus;
        private String paymentStatus;
        private String paymentMethod;
        private boolean cancellableByCustomer;
        private Set<String> allowedNextStatuses;
        private List<OrderItemResponse> items;
        private List<TrackingEventResponse> trackingHistory;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
        public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public boolean isCancellableByCustomer() { return cancellableByCustomer; }
        public void setCancellableByCustomer(boolean cancellableByCustomer) { this.cancellableByCustomer = cancellableByCustomer; }
        public Set<String> getAllowedNextStatuses() { return allowedNextStatuses; }
        public void setAllowedNextStatuses(Set<String> allowedNextStatuses) { this.allowedNextStatuses = allowedNextStatuses; }
        public List<OrderItemResponse> getItems() { return items; }
        public void setItems(List<OrderItemResponse> items) { this.items = items; }
        public List<TrackingEventResponse> getTrackingHistory() { return trackingHistory; }
        public void setTrackingHistory(List<TrackingEventResponse> trackingHistory) { this.trackingHistory = trackingHistory; }
    }

    public static class NotificationResponse {
        private Long id;
        private Long orderId;
        private String orderNumber;
        private String message;
        private LocalDateTime createdAt;
        private boolean readStatus;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public boolean isReadStatus() { return readStatus; }
        public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }
    }

    public static class AdminDashboardResponse {
        private long totalProducts;
        private long totalCustomers;
        private long totalOrders;
        private long pendingOrders;
        private long shippedOrders;
        private long deliveredOrders;
        private long cancelledOrders;
        private BigDecimal totalRevenue;
        private boolean automationEnabled;
        private List<OrderResponse> recentOrders;
        private List<CatalogDtos.ProductResponse> lowStockProducts;
        private CacheMetricsDto cacheMetrics;

        public long getTotalProducts() { return totalProducts; }
        public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
        public long getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
        public long getShippedOrders() { return shippedOrders; }
        public void setShippedOrders(long shippedOrders) { this.shippedOrders = shippedOrders; }
        public long getDeliveredOrders() { return deliveredOrders; }
        public void setDeliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; }
        public long getCancelledOrders() { return cancelledOrders; }
        public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public boolean isAutomationEnabled() { return automationEnabled; }
        public void setAutomationEnabled(boolean automationEnabled) { this.automationEnabled = automationEnabled; }
        public List<OrderResponse> getRecentOrders() { return recentOrders; }
        public void setRecentOrders(List<OrderResponse> recentOrders) { this.recentOrders = recentOrders; }
        public List<CatalogDtos.ProductResponse> getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(List<CatalogDtos.ProductResponse> lowStockProducts) { this.lowStockProducts = lowStockProducts; }
        public CacheMetricsDto getCacheMetrics() { return cacheMetrics; }
        public void setCacheMetrics(CacheMetricsDto cacheMetrics) { this.cacheMetrics = cacheMetrics; }
    }

    public static class CacheMetricsDto {
        private boolean secondLevelCacheEnabled;
        private boolean queryCacheEnabled;
        private long secondLevelCacheHitCount;
        private long secondLevelCacheMissCount;
        private long secondLevelCachePutCount;
        private long queryCacheHitCount;
        private long queryCacheMissCount;
        private long queryCachePutCount;

        public boolean isSecondLevelCacheEnabled() { return secondLevelCacheEnabled; }
        public void setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) { this.secondLevelCacheEnabled = secondLevelCacheEnabled; }
        public boolean isQueryCacheEnabled() { return queryCacheEnabled; }
        public void setQueryCacheEnabled(boolean queryCacheEnabled) { this.queryCacheEnabled = queryCacheEnabled; }
        public long getSecondLevelCacheHitCount() { return secondLevelCacheHitCount; }
        public void setSecondLevelCacheHitCount(long secondLevelCacheHitCount) { this.secondLevelCacheHitCount = secondLevelCacheHitCount; }
        public long getSecondLevelCacheMissCount() { return secondLevelCacheMissCount; }
        public void setSecondLevelCacheMissCount(long secondLevelCacheMissCount) { this.secondLevelCacheMissCount = secondLevelCacheMissCount; }
        public long getSecondLevelCachePutCount() { return secondLevelCachePutCount; }
        public void setSecondLevelCachePutCount(long secondLevelCachePutCount) { this.secondLevelCachePutCount = secondLevelCachePutCount; }
        public long getQueryCacheHitCount() { return queryCacheHitCount; }
        public void setQueryCacheHitCount(long queryCacheHitCount) { this.queryCacheHitCount = queryCacheHitCount; }
        public long getQueryCacheMissCount() { return queryCacheMissCount; }
        public void setQueryCacheMissCount(long queryCacheMissCount) { this.queryCacheMissCount = queryCacheMissCount; }
        public long getQueryCachePutCount() { return queryCachePutCount; }
        public void setQueryCachePutCount(long queryCachePutCount) { this.queryCachePutCount = queryCachePutCount; }
    }
}

