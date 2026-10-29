package com.ecommerce.controller;

import com.ecommerce.dto.OrderDtos;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SessionAuthUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<OrderDtos.CartResponse> getCart(HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(cartService.getCartForUser(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<OrderDtos.CartResponse> addItem(
            @Valid @RequestBody OrderDtos.AddToCartRequest request,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<OrderDtos.CartResponse> updateItem(
            @PathVariable("id") Long cartItemId,
            @Valid @RequestBody OrderDtos.UpdateCartItemRequest request,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, cartItemId, request.getQuantity()));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<OrderDtos.CartResponse> removeItem(
            @PathVariable("id") Long cartItemId,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        return ResponseEntity.ok(cartService.removeCartItem(userId, cartItemId));
    }
}

