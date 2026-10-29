package com.ecommerce.service;

import com.ecommerce.dao.UserDao;
import com.ecommerce.dto.AuthDtos;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional
    public User registerCustomer(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new BadRequestException("An account with this email address already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        user.setShippingAddress(request.getShippingAddress() != null ? request.getShippingAddress().trim() : "");

        return userDao.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(AuthDtos.LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userDao.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    @Transactional
    public User updateProfile(Long userId, AuthDtos.UpdateProfileRequest request) {
        User user = getUserById(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        user.setShippingAddress(request.getShippingAddress() != null ? request.getShippingAddress().trim() : "");
        return userDao.save(user);
    }

    @Transactional
    public User ensureBootstrapAdmin(String email, String rawPassword, String fullName) {
        if (userDao.countByRole(Role.ADMIN) > 0) {
            return userDao.findByEmail(email).orElse(null);
        }
        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);
        admin.setPhone("+1-800-555-0199");
        admin.setShippingAddress("University Computer Science Lab HQ, Room 402");
        return userDao.save(admin);
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UserResponse> getAllUsers() {
        return userDao.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public AuthDtos.UserResponse toUserResponse(User user) {
        AuthDtos.UserResponse res = new AuthDtos.UserResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole().name());
        res.setPhone(user.getPhone());
        res.setShippingAddress(user.getShippingAddress());
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }
}

