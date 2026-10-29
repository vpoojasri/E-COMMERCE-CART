package com.ecommerce;

import com.ecommerce.config.RootConfig;
import com.ecommerce.config.WebMvcConfig;
import com.ecommerce.dto.AuthDtos;
import com.ecommerce.dto.CatalogDtos;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ForbiddenException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.service.*;
import com.ecommerce.util.SessionAuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RootConfig.class, WebMvcConfig.class})
public class EcommerceEngineIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderTrackingService orderTrackingService;

    @Autowired
    private FileStorageService fileStorageService;

    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
    }

    @Test
    void testUserRegistrationAndAuthentication() {
        AuthDtos.RegisterRequest reg = new AuthDtos.RegisterRequest();
        reg.setFullName("Priya Sharma");
        reg.setEmail("priya." + uniqueSuffix + "@university.edu");
        reg.setPassword("SecurePass123!");
        reg.setPhone("9876543210");
        reg.setShippingAddress("Hostel Block C, Room 204");

        User created = authService.registerCustomer(reg);
        assertNotNull(created.getId());
        assertEquals(Role.CUSTOMER, created.getRole());
        assertNotEquals("SecurePass123!", created.getPasswordHash(), "Password must be BCrypt hashed, never plaintext");

        // Duplicate registration must fail
        assertThrows(BadRequestException.class, () -> authService.registerCustomer(reg));

        // Valid login
        AuthDtos.LoginRequest login = new AuthDtos.LoginRequest();
        login.setEmail(reg.getEmail());
        login.setPassword("SecurePass123!");
        User authenticated = authService.authenticate(login);
        assertEquals(created.getId(), authenticated.getId());

        // Invalid password must fail
        login.setPassword("WrongPassword");
        assertThrows(UnauthorizedException.class, () -> authService.authenticate(login));
    }

    @Test
    void testProductCreationRetrievalAndImageUploadValidation() {
        CatalogDtos.CategoryRequest catReq = new CatalogDtos.CategoryRequest();
        catReq.setName("Lab Instruments " + uniqueSuffix);
        catReq.setDescription("Precision Oscilloscopes and Logic Analyzers");
        CatalogDtos.CategoryResponse category = catalogService.createCategory(catReq);
        assertNotNull(category.getId());

        CatalogDtos.ProductRequest prodReq = new CatalogDtos.ProductRequest();
        prodReq.setName("Digital Storage Oscilloscope " + uniqueSuffix);
        prodReq.setDescription("100MHz 4-Channel DSO for Electronics Lab");
        prodReq.setPrice(new BigDecimal("350.00"));
        prodReq.setStockQuantity(10);
        prodReq.setCategoryId(category.getId());

        CatalogDtos.ProductResponse product = catalogService.createProduct(prodReq);
        assertNotNull(product.getId());
        assertTrue(product.isAvailable());
        assertEquals(0, new BigDecimal("350.00").compareTo(product.getPrice()));

        // Valid PNG file signature (89 50 4E 47 0D 0A 1A 0A)
        byte[] validPngHeader = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile validPng = new MockMultipartFile(
                "file", "oscilloscope.png", "image/png", validPngHeader);
        String savedPath = fileStorageService.storeProductImage(validPng);
        assertTrue(savedPath.startsWith("/api/uploads/prod-"));

        // Invalid executable disguised as PNG must be rejected by binary signature check
        byte[] fakeBytes = "MZ-executable-content-not-an-image".getBytes();
        MockMultipartFile disguisedFile = new MockMultipartFile(
                "file", "exploit.png", "image/png", fakeBytes);
        assertThrows(BadRequestException.class, () -> fileStorageService.storeProductImage(disguisedFile));

        // Path traversal filename must be rejected
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file", "../../evil.png", "image/png", validPngHeader);
        assertThrows(BadRequestException.class, () -> fileStorageService.storeProductImage(traversalFile));
    }

    @Test
    void testCartValidationOrderPlacementInventoryDeductionAndTrackingLifecycle() {
        User customer = createTestCustomer("orderuser." + uniqueSuffix + "@edu.in");
        CatalogDtos.ProductResponse product = createTestProduct("Smart Sensor Kit " + uniqueSuffix, new BigDecimal("120.00"), 5);

        // Exceeding stock in cart must fail
        OrderDtos.AddToCartRequest tooMany = new OrderDtos.AddToCartRequest();
        tooMany.setProductId(product.getId());
        tooMany.setQuantity(6);
        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(customer.getId(), tooMany));

        // Valid cart addition (2 units @ 120.00 = 240.00)
        OrderDtos.AddToCartRequest validAdd = new OrderDtos.AddToCartRequest();
        validAdd.setProductId(product.getId());
        validAdd.setQuantity(2);
        OrderDtos.CartResponse cart = cartService.addItemToCart(customer.getId(), validAdd);
        assertEquals(2, cart.getTotalItems());
        assertEquals(0, new BigDecimal("240.00").compareTo(cart.getTotalAmount()));

        // Place Order
        OrderDtos.PlaceOrderRequest orderReq = new OrderDtos.PlaceOrderRequest();
        orderReq.setShippingAddress("221B Baker Street, Academic Wing");
        orderReq.setContactPhone("9988776655");
        orderReq.setPaymentMethod("SIMULATED_LAB_PAYMENT");

        OrderDtos.OrderResponse placedOrder = orderTrackingService.placeOrder(customer.getId(), orderReq);
        assertNotNull(placedOrder.getId());
        assertTrue(placedOrder.getOrderNumber().startsWith("ORD-"));
        assertTrue(placedOrder.getTrackingNumber().startsWith("TRK-"));
        assertEquals("PLACED", placedOrder.getCurrentStatus(), "Order must start in PLACED status, never immediately DELIVERED");
        assertEquals(0, new BigDecimal("240.00").compareTo(placedOrder.getTotalAmount()));
        assertEquals(1, placedOrder.getTrackingHistory().size());

        // Verify inventory deducted from 5 to 3
        CatalogDtos.ProductResponse updatedProduct = catalogService.getProductById(product.getId());
        assertEquals(3, updatedProduct.getStockQuantity());

        // Invalid jump transition PLACED -> DELIVERED must be rejected
        assertThrows(BadRequestException.class, () ->
                orderTrackingService.updateOrderStatus(
                        placedOrder.getId(), OrderStatus.DELIVERED, "Illegal jump", "Hub", "admin@test.edu"));

        // Valid step-by-step lifecycle transitions: PLACED -> CONFIRMED -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
        OrderDtos.OrderResponse s1 = orderTrackingService.updateOrderStatus(
                placedOrder.getId(), OrderStatus.CONFIRMED, "Confirmed", "Warehouse", "admin@test.edu");
        assertEquals("CONFIRMED", s1.getCurrentStatus());

        OrderDtos.OrderResponse s2 = orderTrackingService.updateOrderStatus(
                placedOrder.getId(), OrderStatus.PACKED, "Packed", "Warehouse", "admin@test.edu");
        assertEquals("PACKED", s2.getCurrentStatus());

        // Customer cannot cancel once PACKED
        assertThrows(BadRequestException.class, () ->
                orderTrackingService.cancelOrderByCustomer(placedOrder.getId(), customer.getId(), "Changed mind"));

        OrderDtos.OrderResponse s3 = orderTrackingService.updateOrderStatus(
                placedOrder.getId(), OrderStatus.SHIPPED, "Shipped", "Sorting Hub", "admin@test.edu");
        assertEquals("SHIPPED", s3.getCurrentStatus());

        OrderDtos.OrderResponse s4 = orderTrackingService.updateOrderStatus(
                placedOrder.getId(), OrderStatus.OUT_FOR_DELIVERY, "Out for delivery", "Local Hub", "admin@test.edu");
        assertEquals("OUT_FOR_DELIVERY", s4.getCurrentStatus());

        OrderDtos.OrderResponse s5 = orderTrackingService.updateOrderStatus(
                placedOrder.getId(), OrderStatus.DELIVERED, "Delivered", "Customer Doorstep", "admin@test.edu");
        assertEquals("DELIVERED", s5.getCurrentStatus());
        assertEquals(6, s5.getTrackingHistory().size(), "All 6 transitions must be recorded in tracking history");
    }

    @Test
    void testOrderCancellationRestoresInventoryAndAuthorizationRules() {
        User customerA = createTestCustomer("custA." + uniqueSuffix + "@edu.in");
        User customerB = createTestCustomer("custB." + uniqueSuffix + "@edu.in");
        CatalogDtos.ProductResponse product = createTestProduct("Robotics Servo " + uniqueSuffix, new BigDecimal("45.00"), 4);

        OrderDtos.AddToCartRequest addReq = new OrderDtos.AddToCartRequest();
        addReq.setProductId(product.getId());
        addReq.setQuantity(3);
        cartService.addItemToCart(customerA.getId(), addReq);

        OrderDtos.PlaceOrderRequest placeReq = new OrderDtos.PlaceOrderRequest();
        placeReq.setShippingAddress("Lab 101");
        placeReq.setContactPhone("9123456789");
        OrderDtos.OrderResponse order = orderTrackingService.placeOrder(customerA.getId(), placeReq);

        // Inventory is now 4 - 3 = 1
        assertEquals(1, catalogService.getProductById(product.getId()).getStockQuantity());

        // Customer B must not be allowed to view or cancel Customer A's order
        assertThrows(ForbiddenException.class, () ->
                orderTrackingService.getOrderForUserOrAdmin(order.getId(), customerB.getId(), Role.CUSTOMER));
        assertThrows(ForbiddenException.class, () ->
                orderTrackingService.getOrderByTrackingNumber(order.getTrackingNumber(), customerB.getId(), Role.CUSTOMER));
        assertThrows(ForbiddenException.class, () ->
                orderTrackingService.cancelOrderByCustomer(order.getId(), customerB.getId(), "Unauthorized attempt"));

        // Customer A cancels while in PLACED status -> inventory restored to 4
        OrderDtos.OrderResponse cancelled = orderTrackingService.cancelOrderByCustomer(
                order.getId(), customerA.getId(), "Ordered wrong quantity");
        assertEquals("CANCELLED", cancelled.getCurrentStatus());
        assertEquals("REFUNDED", cancelled.getPaymentStatus());
        assertEquals(4, catalogService.getProductById(product.getId()).getStockQuantity());

        // Admin role check verification
        MockHttpSession customerSession = new MockHttpSession();
        SessionAuthUtil.setAuthenticatedUser(customerSession, customerA);
        assertThrows(ForbiddenException.class, () -> SessionAuthUtil.requireAdmin(customerSession));
    }

    @Test
    void testHibernateSecondLevelAndQueryCacheBehavior() {
        CatalogDtos.CategoryRequest req = new CatalogDtos.CategoryRequest();
        req.setName("Cached Category " + uniqueSuffix);
        req.setDescription("Testing L2 and Query Cache");
        catalogService.createCategory(req);

        List<CatalogDtos.CategoryResponse> firstCall = catalogService.getAllCategories();
        assertFalse(firstCall.isEmpty());

        OrderDtos.CacheMetricsDto before = orderTrackingService.getCacheMetrics();
        assertTrue(before.isSecondLevelCacheEnabled());
        assertTrue(before.isQueryCacheEnabled());

        // Second call hits query cache
        List<CatalogDtos.CategoryResponse> secondCall = catalogService.getAllCategories();
        assertEquals(firstCall.size(), secondCall.size());

        OrderDtos.CacheMetricsDto after = orderTrackingService.getCacheMetrics();
        assertTrue(after.getQueryCacheHitCount() > before.getQueryCacheHitCount(),
                "Hibernate Query Cache hit count should increment on repeated category queries");
    }

    private User createTestCustomer(String email) {
        AuthDtos.RegisterRequest req = new AuthDtos.RegisterRequest();
        req.setFullName("Test Customer");
        req.setEmail(email);
        req.setPassword("Pass@1234");
        req.setPhone("9000011111");
        req.setShippingAddress("Test Address");
        return authService.registerCustomer(req);
    }

    private CatalogDtos.ProductResponse createTestProduct(String name, BigDecimal price, int stock) {
        CatalogDtos.CategoryRequest catReq = new CatalogDtos.CategoryRequest();
        catReq.setName("Cat-" + name);
        catReq.setDescription("Test Category");
        CatalogDtos.CategoryResponse cat = catalogService.createCategory(catReq);

        CatalogDtos.ProductRequest prodReq = new CatalogDtos.ProductRequest();
        prodReq.setName(name);
        prodReq.setDescription("Test Description");
        prodReq.setPrice(price);
        prodReq.setStockQuantity(stock);
        prodReq.setCategoryId(cat.getId());
        return catalogService.createProduct(prodReq);
    }
}

