package com.ecommerce.controller;

import com.ecommerce.dto.AuthDtos;
import com.ecommerce.entity.User;
import com.ecommerce.service.AuthService;
import com.ecommerce.util.SessionAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.UserResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest httpRequest) {
        User user = authService.registerCustomer(request);
        HttpSession session = httpRequest.getSession(true);
        SessionAuthUtil.setAuthenticatedUser(session, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.toUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.UserResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest) {
        User user = authService.authenticate(request);
        HttpSession existing = httpRequest.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession newSession = httpRequest.getSession(true);
        SessionAuthUtil.setAuthenticatedUser(newSession, user);
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> getCurrentUser(HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthDtos.UserResponse> updateProfile(
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request,
            HttpSession session) {
        Long userId = SessionAuthUtil.requireUserId(session);
        User updated = authService.updateProfile(userId, request);
        return ResponseEntity.ok(authService.toUserResponse(updated));
    }
}

