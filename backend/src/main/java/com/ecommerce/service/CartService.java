package com.ecommerce.service;

import com.ecommerce.dao.CartItemDao;
import com.ecommerce.dao.ProductDao;
import com.ecommerce.dao.UserDao;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ForbiddenException;
import com.ecommerce.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartItemDao cartItemDao;
    private final ProductDao productDao;
    private final UserDao userDao;

    public CartService(CartItemDao cartItemDao, ProductDao productDao, UserDao userDao) {
        this.cartItemDao = cartItemDao;
        this.productDao = productDao;
        this.userDao = userDao;
    }

    @Transactional(readOnly = true)
    public OrderDtos.CartResponse getCartForUser(Long userId) {
        List<CartItem> items = cartItemDao.findByUserId(userId);
        List<OrderDtos.CartItemDto> dtoList = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQty = 0;
        boolean allValid = true;

        for (CartItem ci : items) {
            Product p = ci.getProduct();
            OrderDtos.CartItemDto dto = new OrderDtos.CartItemDto();
            dto.setId(ci.getId());
            dto.setProductId(p.getId());
            dto.setProductName(p.getName());
            dto.setImagePath(p.getImagePath() != null ? p.getImagePath() : "/api/uploads/fallback-product.svg");
            dto.setUnitPrice(p.getPrice());
            dto.setQuantity(ci.getQuantity());
            dto.setAvailableStock(p.getStockQuantity());

            BigDecimal lineSubtotal = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            dto.setSubtotal(lineSubtotal);

            boolean valid = Boolean.TRUE.equals(p.getActive())
                    && p.getStockQuantity() != null
                    && p.getStockQuantity() >= ci.getQuantity();
            dto.setStockValid(valid);
            if (!valid) {
                allValid = false;
            }

            subtotal = subtotal.add(lineSubtotal);
            totalQty += ci.getQuantity();
            dtoList.add(dto);
        }

        OrderDtos.CartResponse response = new OrderDtos.CartResponse();
        response.setItems(dtoList);
        response.setTotalItems(totalQty);
        response.setSubtotal(subtotal);
        BigDecimal shipping = BigDecimal.ZERO;
        response.setShippingFee(shipping);
        response.setTotalAmount(subtotal.add(shipping));
        response.setAllItemsInStock(allValid && !dtoList.isEmpty());
        return response;
    }

    @Transactional
    public OrderDtos.CartResponse addItemToCart(Long userId, OrderDtos.AddToCartRequest request) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Product product = productDao.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (!Boolean.TRUE.equals(product.getActive()) || product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            throw new BadRequestException("This product is currently out of stock and cannot be added to the cart.");
        }

        Optional<CartItem> existingOpt = cartItemDao.findByUserAndProduct(userId, product.getId());
        int targetQuantity = request.getQuantity();
        if (existingOpt.isPresent()) {
            targetQuantity += existingOpt.get().getQuantity();
        }

        if (targetQuantity > product.getStockQuantity()) {
            throw new BadRequestException("Requested quantity (" + targetQuantity
                    + ") exceeds available stock (" + product.getStockQuantity() + ") for " + product.getName() + ".");
        }

        CartItem item = existingOpt.orElseGet(() -> {
            CartItem ci = new CartItem();
            ci.setUser(user);
            ci.setProduct(product);
            return ci;
        });
        item.setQuantity(targetQuantity);
        cartItemDao.save(item);

        return getCartForUser(userId);
    }

    @Transactional
    public OrderDtos.CartResponse updateCartItemQuantity(Long userId, Long cartItemId, int newQuantity) {
        CartItem item = cartItemDao.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));
        if (!item.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this cart item.");
        }
        if (newQuantity <= 0) {
            cartItemDao.delete(item);
            return getCartForUser(userId);
        }

        Product product = item.getProduct();
        if (!Boolean.TRUE.equals(product.getActive()) || product.getStockQuantity() < newQuantity) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of '"
                    + product.getName() + "' are available in stock.");
        }

        item.setQuantity(newQuantity);
        cartItemDao.save(item);
        return getCartForUser(userId);
    }

    @Transactional
    public OrderDtos.CartResponse removeCartItem(Long userId, Long cartItemId) {
        CartItem item = cartItemDao.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));
        if (!item.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to remove this cart item.");
        }
        cartItemDao.delete(item);
        return getCartForUser(userId);
    }

    @Transactional
    public void clearUserCart(Long userId) {
        cartItemDao.deleteAllByUserId(userId);
    }
}

