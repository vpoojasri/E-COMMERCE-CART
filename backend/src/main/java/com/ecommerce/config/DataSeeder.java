package com.ecommerce.config;

import com.ecommerce.dao.CategoryDao;
import com.ecommerce.dao.ProductDao;
import com.ecommerce.dto.AuthDtos;
import com.ecommerce.dto.OrderDtos;
import com.ecommerce.entity.*;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AuthService authService;
    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final CartService cartService;
    private final OrderTrackingService orderTrackingService;

    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;
    private final AtomicBoolean seeded = new AtomicBoolean(false);

    public DataSeeder(
            AuthService authService,
            CategoryDao categoryDao,
            ProductDao productDao,
            CartService cartService,
            OrderTrackingService orderTrackingService,
            @Value("${admin.initial.email:admin@ecommerce.edu}") String adminEmail,
            @Value("${admin.initial.password:Admin@123}") String adminPassword,
            @Value("${admin.initial.name:Lab System Administrator}") String adminName) {
        this.authService = authService;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        this.cartService = cartService;
        this.orderTrackingService = orderTrackingService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void onApplicationEvent() {
        if (!seeded.compareAndSet(false, true)) {
            return;
        }
        try {
            authService.ensureBootstrapAdmin(adminEmail, adminPassword, adminName);
            if (productDao.countAll() == 0) {
                log.info("Seeding initial college lab catalog categories, products, and sample customer order...");
                seedCatalogAndSampleOrder();
            }
        } catch (Exception e) {
            log.warn("DataSeeder skipped or encountered non-fatal issue: {}", e.getMessage());
        }
    }

    private void seedCatalogAndSampleOrder() {
        Category electronics = createCat("Electronics & Gadgets", "High-performance computing, audio equipment, and smart electronics.");
        Category smartHome = createCat("Smart Home & IoT", "Automated sensors, smart lighting, and connected home appliances.");
        Category devGear = createCat("Developer Gear & Accessories", "Mechanical keyboards, ergonomic stands, docks, and lab hardware.");
        Category books = createCat("Technical Books & Media", "Computer science textbooks, software architecture guides, and reference manuals.");

        Product p1 = createProd("Pro Wireless Noise-Canceling Headphones",
                "Studio-grade active noise cancellation, 40-hour battery life, USB-C fast charging, and multipoint Bluetooth 5.3 connectivity.",
                new BigDecimal("249.99"), 25, "/api/uploads/sample-headphones.svg", electronics);

        Product p2 = createProd("Tactile Mechanical Keyboard RGB 75%",
                "Hot-swappable gasket mount mechanical keyboard with pre-lubed tactile switches, PBT keycaps, and aluminum CNC frame.",
                new BigDecimal("139.50"), 18, "/api/uploads/sample-keyboard.svg", devGear);

        createProd("Ultra-Wide 34\" WQHD Curved Monitor",
                "3440x1440 resolution, 144Hz refresh rate, 99% sRGB color accuracy, and built-in USB-C 90W power delivery hub.",
                new BigDecimal("499.00"), 8, "/api/uploads/sample-monitor.svg", electronics);

        createProd("Smart Matter Thread Home Hub Pro",
                "Local-first smart home automation hub supporting Zigbee 3.0, Matter, Thread, and real-time energy consumption monitoring.",
                new BigDecimal("89.99"), 32, "/api/uploads/sample-smarthub.svg", smartHome);

        createProd("Thunderbolt 4 12-in-1 Workstation Dock",
                "Dual 4K 60Hz display output, 2.5GbE Ethernet, SD UHS-II card reader, and 100W host charging for developer laptops.",
                new BigDecimal("199.00"), 4, "/api/uploads/sample-dock.svg", devGear);

        createProd("Enterprise Distributed Systems Architecture (Hardcover)",
                "Comprehensive reference covering consensus algorithms, database replication, caching strategies, and resilient microservices.",
                new BigDecimal("54.95"), 45, "/api/uploads/sample-book.svg", books);

        createProd("Smart Wi-Fi Ambient LED Light Bar Pair",
                "Dynamic RGBIC ambient desk lighting with music sync, circadian rhythm automation, and voice assistant support.",
                new BigDecimal("64.00"), 15, "/api/uploads/sample-lightbar.svg", smartHome);

        createProd("Limited Edition FPGA Development Board",
                "Cyclone V FPGA evaluation kit with onboard SDRAM, HDMI output, GPIO headers, and logic analyzer headers for digital design labs.",
                new BigDecimal("175.00"), 0, "/api/uploads/sample-fpga.svg", electronics);

        // Seed a demo customer and one sample multi-stage order so tracking is immediately demonstrable
        try {
            AuthDtos.RegisterRequest custReq = new AuthDtos.RegisterRequest();
            custReq.setFullName("Alex Rivera (Demo Customer)");
            custReq.setEmail("customer@ecommerce.edu");
            custReq.setPassword("Customer@123");
            custReq.setPhone("+1-555-234-5678");
            custReq.setShippingAddress("742 Evergreen Terrace, Apt 4B, Innovation Campus");
            User demoCustomer = authService.registerCustomer(custReq);

            OrderDtos.AddToCartRequest item1 = new OrderDtos.AddToCartRequest();
            item1.setProductId(p1.getId());
            item1.setQuantity(1);
            cartService.addItemToCart(demoCustomer.getId(), item1);

            OrderDtos.AddToCartRequest item2 = new OrderDtos.AddToCartRequest();
            item2.setProductId(p2.getId());
            item2.setQuantity(1);
            cartService.addItemToCart(demoCustomer.getId(), item2);

            OrderDtos.PlaceOrderRequest placeReq = new OrderDtos.PlaceOrderRequest();
            placeReq.setShippingAddress(demoCustomer.getShippingAddress());
            placeReq.setContactPhone(demoCustomer.getPhone());
            placeReq.setPaymentMethod("SIMULATED_COLLEGE_LAB_PAY");
            OrderDtos.OrderResponse placedOrder = orderTrackingService.placeOrder(demoCustomer.getId(), placeReq);

            orderTrackingService.updateOrderStatus(placedOrder.getId(), OrderStatus.CONFIRMED,
                    "Order verified by warehouse inventory control.", "Central Inventory Node", adminEmail);
            orderTrackingService.updateOrderStatus(placedOrder.getId(), OrderStatus.PACKED,
                    "Items packed in anti-static protective box and barcoded.", "Fulfillment Bay 2", adminEmail);
            orderTrackingService.updateOrderStatus(placedOrder.getId(), OrderStatus.SHIPPED,
                    "Dispatched via Simulated Express Line-Haul Carrier.", "Regional Sorting Hub", adminEmail);
        } catch (Exception ignored) {
        }
    }

    private Category createCat(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        return categoryDao.save(c);
    }

    private Product createProd(String name, String desc, BigDecimal price, int stock, String imagePath, Category cat) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setImagePath(imagePath);
        p.setCategory(cat);
        p.setActive(true);
        return productDao.save(p);
    }
}

