package com.ecommerce.service;

import com.ecommerce.dao.*;
import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ForbiddenException;
import com.ecommerce.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class OrderTrackingService {

    private final OrderDao orderDao;
    private final CartItemDao cartItemDao;
    private final ProductDao productDao;
    private final UserDao userDao;
    private final NotificationDao notificationDao;
    private final CatalogService catalogService;
    private final EntityManagerFactory entityManagerFactory;
    private final AtomicBoolean automationEnabled;

    public OrderTrackingService(
            OrderDao orderDao,
            CartItemDao cartItemDao,
            ProductDao productDao,
            UserDao userDao,
            NotificationDao notificationDao,
            CatalogService catalogService,
            EntityManagerFactory entityManagerFactory,
            @Value("${tracking.automation.enabled:false}") boolean defaultAutomationEnabled) {
        this.orderDao = orderDao;
        this.cartItemDao = cartItemDao;
        this.productDao = productDao;
        this.userDao = userDao;
        this.notificationDao = notificationDao;
        this.catalogService = catalogService;
        this.entityManagerFactory = entityManagerFactory;
        this.automationEnabled = new AtomicBoolean(defaultAutomationEnabled);
    }

    public boolean isAutomationEnabled() {
        return automationEnabled.get();
    }

    public void setAutomationEnabled(boolean enabled) {
        this.automationEnabled.set(enabled);
    }

    /**
     * Places an order within an atomic database transaction:
     * - Re-validates stock with pessimistic write lock
     * - Recalculates all prices on the server
     * - Snapshots unit prices and product names
     * - Deducts product stock safely
     * - Generates unique orderNumber and trackingNumber
     * - Sets initial status to PLACED (never immediately DELIVERED)
     * - Records initial OrderTrackingHistory entry & customer Notification
     */
    @Transactional
    public OrderDtos.OrderResponse placeOrder(Long userId, OrderDtos.PlaceOrderRequest request) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        List<CartItem> cartItems = cartItemDao.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your shopping cart is empty. Please add items before placing an order.");
        }

        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(generateUniqueOrderNumber(now));
        order.setTrackingNumber(generateUniqueTrackingNumber());
        order.setUser(user);
        order.setOrderDate(now);
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setContactPhone(request.getContactPhone().trim());
        order.setEstimatedDeliveryDate(now.toLocalDate().plusDays(5));
        order.setCurrentStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.SIMULATED_PAID);
        order.setPaymentMethod(request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? request.getPaymentMethod().trim()
                : "SIMULATED_LAB_PAYMENT");

        BigDecimal serverCalculatedTotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product lockedProduct = productDao.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product no longer exists."));

            if (!Boolean.TRUE.equals(lockedProduct.getActive())) {
                throw new BadRequestException("Product '" + lockedProduct.getName() + "' is no longer available.");
            }
            if (lockedProduct.getStockQuantity() == null || lockedProduct.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for '" + lockedProduct.getName()
                        + "'. Available: " + lockedProduct.getStockQuantity() + ", Requested: " + cartItem.getQuantity());
            }

            // Deduct inventory safely
            lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() - cartItem.getQuantity());
            productDao.save(lockedProduct);

            BigDecimal unitPrice = lockedProduct.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            serverCalculatedTotal = serverCalculatedTotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(lockedProduct);
            orderItem.setProductNameSnapshot(lockedProduct.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(lineTotal);
            order.getItems().add(orderItem);
        }

        order.setTotalAmount(serverCalculatedTotal);

        OrderTrackingHistory initialHistory = new OrderTrackingHistory();
        initialHistory.setOrder(order);
        initialHistory.setStatus(OrderStatus.PLACED);
        initialHistory.setDescription("Order placed by customer and payment verified via Simulated College Lab Gateway.");
        initialHistory.setLocationNote("Online Order Processing Center");
        initialHistory.setUpdatedAt(now);
        initialHistory.setUpdatedBy(user.getEmail());
        order.getTrackingHistory().add(initialHistory);

        CustomerOrder savedOrder = orderDao.save(order);
        cartItemDao.deleteAllByUserId(userId);

        createNotification(user, savedOrder,
                "Order " + savedOrder.getOrderNumber() + " has been placed! Tracking #: " + savedOrder.getTrackingNumber());

        return toOrderResponse(savedOrder);
    }

    /**
     * Updates order status enforcing legal transitions:
     * PLACED -> CONFIRMED -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
     * Also supports CANCELLED where permitted and restores product inventory on cancellation.
     */
    @Transactional
    public OrderDtos.OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus targetStatus,
            String customDescription,
            String locationNote,
            String updatedByActor) {

        CustomerOrder order = orderDao.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " was not found."));

        OrderStatus current = order.getCurrentStatus();
        if (current == targetStatus) {
            throw new BadRequestException("Order is already in status " + targetStatus + ".");
        }
        if (!current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus
                    + ". Allowed next statuses from " + current + ": " + current.getAllowedNextStatuses());
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            restoreInventoryForCancelledOrder(order);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order.setCurrentStatus(targetStatus);
        if (targetStatus == OrderStatus.SHIPPED) {
            order.setEstimatedDeliveryDate(LocalDate.now().plusDays(2));
        } else if (targetStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.setEstimatedDeliveryDate(LocalDate.now());
        } else if (targetStatus == OrderStatus.DELIVERED) {
            order.setEstimatedDeliveryDate(LocalDate.now());
        }

        String description = (customDescription != null && !customDescription.isBlank())
                ? customDescription.trim()
                : defaultStageDescription(targetStatus);

        String location = (locationNote != null && !locationNote.isBlank())
                ? locationNote.trim()
                : defaultStageLocation(targetStatus);

        OrderTrackingHistory history = new OrderTrackingHistory();
        history.setOrder(order);
        history.setStatus(targetStatus);
        history.setDescription(description);
        history.setLocationNote(location);
        history.setUpdatedAt(LocalDateTime.now());
        history.setUpdatedBy(updatedByActor != null ? updatedByActor : "Administrator");
        order.getTrackingHistory().add(history);

        CustomerOrder saved = orderDao.save(order);

        createNotification(
                saved.getUser(),
                saved,
                "Order " + saved.getOrderNumber() + " status updated to " + targetStatus.name().replace('_', ' ') + ": " + description);

        return toOrderResponse(saved);
    }

    @Transactional
    public OrderDtos.OrderResponse cancelOrderByCustomer(Long orderId, Long userId, String reason) {
        CustomerOrder order = orderDao.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are only authorized to cancel your own orders.");
        }
        if (!order.getCurrentStatus().isCancellableByCustomer()) {
            throw new BadRequestException("Order cannot be cancelled by customer once it has reached status: "
                    + order.getCurrentStatus() + ". Only PLACED or CONFIRMED orders can be self-cancelled.");
        }

        String desc = "Order cancelled by customer" + (reason != null && !reason.isBlank() ? " (Reason: " + reason.trim() + ")" : ".");
        return updateOrderStatus(orderId, OrderStatus.CANCELLED, desc, "Customer Portal", order.getUser().getEmail());
    }

    @Transactional
    public int advanceActiveOrdersInSimulationStep(String triggeredBy) {
        List<CustomerOrder> activeOrders = orderDao.findActiveOrdersForAutomation();
        int advancedCount = 0;
        for (CustomerOrder order : activeOrders) {
            OrderStatus next = order.getCurrentStatus().getNextProgressionStage();
            if (next != null) {
                updateOrderStatus(
                        order.getId(),
                        next,
                        "[Simulated Automated Progression] " + defaultStageDescription(next),
                        defaultStageLocation(next),
                        triggeredBy);
                advancedCount++;
            }
        }
        return advancedCount;
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getOrderForUserOrAdmin(Long orderId, Long requesterUserId, Role requesterRole) {
        CustomerOrder order = orderDao.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " was not found."));
        if (requesterRole != Role.ADMIN && !order.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("You do not have permission to view this order.");
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getOrderByTrackingNumber(String trackingNumber, Long requesterUserId, Role requesterRole) {
        CustomerOrder order = orderDao.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No order found matching tracking number: " + trackingNumber));
        if (requesterRole != Role.ADMIN && !order.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("Access denied: This tracking number belongs to another customer account.");
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PagedResponse<OrderDtos.OrderResponse> getCustomerOrders(Long userId, int page, int size) {
        List<OrderDtos.OrderResponse> list = orderDao.findByUserId(userId, page, size)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
        long total = orderDao.countByUserId(userId);
        return new CatalogDtos.PagedResponse<>(list, page, size, total);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PagedResponse<OrderDtos.OrderResponse> getAllOrdersForAdmin(OrderStatus statusFilter, int page, int size) {
        List<OrderDtos.OrderResponse> list = orderDao.findAllFiltered(statusFilter, page, size)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
        long total = orderDao.countAllFiltered(statusFilter);
        return new CatalogDtos.PagedResponse<>(list, page, size, total);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.NotificationResponse> getUserNotifications(Long userId) {
        return notificationDao.findByUserId(userId).stream()
                .map(n -> {
                    OrderDtos.NotificationResponse dto = new OrderDtos.NotificationResponse();
                    dto.setId(n.getId());
                    if (n.getOrder() != null) {
                        dto.setOrderId(n.getOrder().getId());
                        dto.setOrderNumber(n.getOrder().getOrderNumber());
                    }
                    dto.setMessage(n.getMessage());
                    dto.setCreatedAt(n.getCreatedAt());
                    dto.setReadStatus(Boolean.TRUE.equals(n.getReadStatus()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationsRead(Long userId) {
        notificationDao.markAllAsReadForUser(userId);
    }

    @Transactional(readOnly = true)
    public OrderDtos.AdminDashboardResponse getAdminDashboardStats() {
        OrderDtos.AdminDashboardResponse dash = new OrderDtos.AdminDashboardResponse();
        dash.setTotalProducts(productDao.countAll());
        dash.setTotalCustomers(userDao.countByRole(Role.CUSTOMER));
        dash.setTotalOrders(orderDao.countAllFiltered(null));
        dash.setPendingOrders(orderDao.countByStatuses(List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED)));
        dash.setShippedOrders(orderDao.countByStatuses(List.of(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY)));
        dash.setDeliveredOrders(orderDao.countByStatuses(List.of(OrderStatus.DELIVERED)));
        dash.setCancelledOrders(orderDao.countByStatuses(List.of(OrderStatus.CANCELLED)));
        dash.setTotalRevenue(orderDao.calculateTotalActiveRevenue());
        dash.setAutomationEnabled(isAutomationEnabled());

        dash.setRecentOrders(orderDao.findAllFiltered(null, 0, 8).stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList()));

        dash.setLowStockProducts(productDao.findLowStockProducts(5).stream()
                .map(catalogService::toProductResponse)
                .collect(Collectors.toList()));

        dash.setCacheMetrics(getCacheMetrics());
        return dash;
    }

    public OrderDtos.CacheMetricsDto getCacheMetrics() {
        OrderDtos.CacheMetricsDto metrics = new OrderDtos.CacheMetricsDto();
        try {
            SessionFactory sf = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sf.getStatistics();
            metrics.setSecondLevelCacheEnabled(sf.getSessionFactoryOptions().isSecondLevelCacheEnabled());
            metrics.setQueryCacheEnabled(sf.getSessionFactoryOptions().isQueryCacheEnabled());
            metrics.setSecondLevelCacheHitCount(stats.getSecondLevelCacheHitCount());
            metrics.setSecondLevelCacheMissCount(stats.getSecondLevelCacheMissCount());
            metrics.setSecondLevelCachePutCount(stats.getSecondLevelCachePutCount());
            metrics.setQueryCacheHitCount(stats.getQueryCacheHitCount());
            metrics.setQueryCacheMissCount(stats.getQueryCacheMissCount());
            metrics.setQueryCachePutCount(stats.getQueryCachePutCount());
        } catch (Exception ignored) {
        }
        return metrics;
    }

    private void restoreInventoryForCancelledOrder(CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            Product lockedProduct = productDao.findByIdForUpdate(item.getProduct().getId()).orElse(null);
            if (lockedProduct != null) {
                lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() + item.getQuantity());
                productDao.save(lockedProduct);
            }
        }
    }

    private void createNotification(User user, CustomerOrder order, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setOrder(order);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setReadStatus(false);
        notificationDao.save(notification);
    }

    private String generateUniqueOrderNumber(LocalDateTime now) {
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    private String generateUniqueTrackingNumber() {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "TRK-" + randomPart;
    }

    private String defaultStageDescription(OrderStatus status) {
        return switch (status) {
            case PLACED -> "Order has been received and logged in our fulfillment queue.";
            case CONFIRMED -> "Order details and inventory allocation have been verified.";
            case PACKED -> "Items have been quality-checked, cushioned, and sealed for dispatch.";
            case SHIPPED -> "Package has been handed over to the simulated courier line-haul hub.";
            case OUT_FOR_DELIVERY -> "Local delivery vehicle is out for final-mile delivery to your address.";
            case DELIVERED -> "Package has been delivered successfully at the destination address.";
            case CANCELLED -> "Order has been cancelled and reserved inventory has been returned to stock.";
        };
    }

    private String defaultStageLocation(OrderStatus status) {
        return switch (status) {
            case PLACED -> "Online Fulfillment System";
            case CONFIRMED -> "Central Inventory Verification Node";
            case PACKED -> "Regional Fulfillment Warehouse — Packaging Bay 3";
            case SHIPPED -> "National Distribution Sorting Hub";
            case OUT_FOR_DELIVERY -> "Local City Courier Dispatch Facility";
            case DELIVERED -> "Customer Shipping Destination";
            case CANCELLED -> "Order Management System";
        };
    }

    public OrderDtos.OrderResponse toOrderResponse(CustomerOrder order) {
        OrderDtos.OrderResponse dto = new OrderDtos.OrderResponse();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTrackingNumber(order.getTrackingNumber());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setCustomerName(order.getUser().getFullName());
            dto.setCustomerEmail(order.getUser().getEmail());
        }
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setContactPhone(order.getContactPhone());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        dto.setCurrentStatus(order.getCurrentStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCancellableByCustomer(order.getCurrentStatus().isCancellableByCustomer());
        dto.setAllowedNextStatuses(order.getCurrentStatus().getAllowedNextStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toSet()));

        List<OrderDtos.OrderItemResponse> itemDtos = order.getItems().stream().map(oi -> {
            OrderDtos.OrderItemResponse itemDto = new OrderDtos.OrderItemResponse();
            itemDto.setId(oi.getId());
            if (oi.getProduct() != null) {
                itemDto.setProductId(oi.getProduct().getId());
                itemDto.setImagePath(oi.getProduct().getImagePath());
            }
            itemDto.setProductNameSnapshot(oi.getProductNameSnapshot());
            itemDto.setUnitPrice(oi.getUnitPrice());
            itemDto.setQuantity(oi.getQuantity());
            itemDto.setLineTotal(oi.getLineTotal());
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        List<OrderDtos.TrackingEventResponse> historyDtos = order.getTrackingHistory().stream().map(th -> {
            OrderDtos.TrackingEventResponse thDto = new OrderDtos.TrackingEventResponse();
            thDto.setId(th.getId());
            thDto.setStatus(th.getStatus().name());
            thDto.setDescription(th.getDescription());
            thDto.setLocationNote(th.getLocationNote());
            thDto.setUpdatedAt(th.getUpdatedAt());
            thDto.setUpdatedBy(th.getUpdatedBy());
            return thDto;
        }).collect(Collectors.toList());
        dto.setTrackingHistory(historyDtos);

        return dto;
    }
}

