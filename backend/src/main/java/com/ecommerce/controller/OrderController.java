package com.ecommerce.controller;

import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.Role;
import com.ecommerce.service.OrderTrackingService;
import com.ecommerce.util.SessionAuthUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderTrackingService orderTrackingService;

    public OrderController(OrderTrackingService orderTrackingService) {
        this.orderTrackingService = orderTrackingService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDtos.OrderResponse> placeOrder(
            @Valid @RequestBody OrderDtos.PlaceOrderRequest request,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        OrderDtos.OrderResponse created = orderTrackingService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/orders/my")
    public ResponseEntity<CatalogDtos.PagedResponse<OrderDtos.OrderResponse>> getMyOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(orderTrackingService.getCustomerOrders(userId, page, size));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDtos.OrderResponse> getOrderById(
            @PathVariable("id") Long orderId,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        Role role = SessionAuthUtil.requireRole(session);
        return ResponseEntity.ok(orderTrackingService.getOrderForUserOrAdmin(orderId, userId, role));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OrderDtos.OrderResponse> cancelOrder(
            @PathVariable("id") Long orderId,
            @RequestBody(required = false) OrderDtos.CancelOrderRequest request,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(orderTrackingService.cancelOrderByCustomer(orderId, userId, reason));
    }

    @GetMapping("/orders/{id}/tracking")
    public ResponseEntity<OrderDtos.OrderResponse> getOrderTracking(
            @PathVariable("id") Long orderId,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        Role role = SessionAuthUtil.requireRole(session);
        return ResponseEntity.ok(orderTrackingService.getOrderForUserOrAdmin(orderId, userId, role));
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<OrderDtos.OrderResponse> getTrackingByNumber(
            @PathVariable("trackingNumber") String trackingNumber,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        Role role = SessionAuthUtil.requireRole(session);
        return ResponseEntity.ok(orderTrackingService.getOrderByTrackingNumber(trackingNumber, userId, role));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<OrderDtos.NotificationResponse>> getMyNotifications(HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(orderTrackingService.getUserNotifications(userId));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, String>> markNotificationsRead(HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        orderTrackingService.markNotificationsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }
}

