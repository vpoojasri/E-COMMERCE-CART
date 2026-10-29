# Project Report & Lab Demonstration Checklist

**Project Title**: E-Commerce Engine with Automated Order Tracking  
**Architecture**: Layered Spring Web MVC + Hibernate ORM 6.5 (L2 Cache) + MySQL / H2 + React 18 SPA

---

## 1. Project Objectives

1. Build a complete, secure, transactional e-commerce platform suitable for university laboratory evaluation.
2. Implement real database-backed **Automated Order Tracking** across a strict state machine (`PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`, plus `CANCELLED`) with audit timestamps, actor attribution, and customer notifications.
3. Demonstrate enterprise backend patterns using pure **Spring MVC** (non-Boot), **Hibernate ORM** with **Second-Level Caching (JCache/Caffeine)**, pessimistic locking for inventory deduction, and secure multipart image validation.

---

## 2. System Architecture

The backend is organized into clean, decoupled packages under `com.ecommerce`:
- `controller`: Thin `@RestController` classes (`AuthController`, `ProductController`, `CartController`, `OrderController`, `AdminController`, `UploadResourceController`).
- `service`: Transactional business logic (`AuthService`, `CatalogService`, `CartService`, `OrderTrackingService`, `FileStorageService`).
- `dao`: Hibernate `EntityManager` data access repositories (`UserDao`, `CategoryDao`, `ProductDao`, `CartItemDao`, `OrderDao`, `NotificationDao`).
- `entity`: Annotated JPA/Hibernate entities (`User`, `Category`, `Product`, `CartItem`, `CustomerOrder`, `OrderItem`, `OrderTrackingHistory`, `Notification`).
- `dto`: Request/response Data Transfer Objects with Jakarta Bean Validation annotations.
- `config`: Java-based Spring MVC (`WebMvcConfig`, `WebAppInitializer`), Hibernate & HikariCP (`RootConfig`), and safe initial bootstrap (`DataSeeder`).
- `scheduler`: Configurable demo automation (`OrderTrackingAutomationScheduler`).
- `exception`: Centralized `@RestControllerAdvice` (`GlobalExceptionHandler`).

---

## 3. Database Schema & ER Summary

- **`users`**: Stores customer and administrator accounts with BCrypt password hashes. Indexed on `email` (unique) and `user_role`.
- **`categories`**: Product categories cached in Hibernate L2 region `categoriesCache`.
- **`products`**: Catalog items with foreign key `category_id`, `price`, `stock_quantity`, `image_path`, and `active` flag. Indexed on `name`, `category_id`, `price`, and `active`.
- **`cart_items`**: Persistent shopping cart items linked to `user_id` and `product_id`.
- **`customer_orders`**: Master order records containing unique `order_number`, unique `tracking_number`, `current_status`, `estimated_delivery_date`, and `payment_status`.
- **`order_items`**: Immutable purchase snapshots (`product_name_snapshot`, `unit_price`, `quantity`, `line_total`).
- **`order_tracking_history`**: Dedicated audit log recording every status transition (`order_id`, `order_status`, `description`, `location_note`, `updated_at`, `updated_by`).
- **`notifications`**: In-app customer alerts generated whenever an order is placed or transitions to a new delivery stage.

---

## 4. Hibernate Second-Level Caching & Verification

- **Configuration**: Configured in `RootConfig.java` using `org.hibernate.orm:hibernate-jcache` and `com.github.ben-manes.caffeine:jcache`.
- **What is Cached**: `Category` (`@Cacheable`, region `categoriesCache`) and category lookup queries (`categoriesQueryCache`), as well as `Product` entity lookups (`productsCache`) with `CacheConcurrencyStrategy.READ_WRITE`.
- **Consistency**: Mutable stock updates and order status changes occur inside `@Transactional` methods through Hibernate, which automatically invalidates/updates `READ_WRITE` cache entries upon commit.
- **Verification**:
  1. Automated JUnit test `testHibernateSecondLevelAndQueryCacheBehavior` in `EcommerceEngineIntegrationTest` asserts that `queryCacheHitCount` increments on repeated category fetches.
  2. Live cache telemetry (`L2 Cache Hits/Misses`, `Query Cache Hits/Misses`) is displayed directly on the **Administrator Dashboard** (`/admin`) and via `GET /api/admin/cache-stats`.

---

## 5. Testing Summary

The integration test suite (`EcommerceEngineIntegrationTest.java`) verifies:
1. **User Registration & BCrypt Authentication**: Rejects duplicate emails, verifies password hashing, and rejects invalid credentials.
2. **Product Creation & Multipart Upload Security**: Verifies valid PNG binary magic-byte headers (`89 50 4E 47`), rejects executables disguised with `.png` extensions, and blocks `../` path traversal filenames.
3. **Cart, Checkout & Pessimistic Inventory Deduction**: Verifies server-side total calculation, stock deduction after order placement, and initial `PLACED` status.
4. **Order Tracking State Machine**: Verifies valid progression through all 6 stages (`PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`) and rejects illegal state jumps (e.g., `PLACED → DELIVERED`).
5. **Cancellation & Authorization**: Verifies inventory restoration when a `PLACED` order is cancelled, prevents cancellation after `PACKED`, and blocks Customer B from viewing or cancelling Customer A's order (`403 Forbidden`).

---

## 6. Screenshot & Lab Evaluation Checklist

Use this checklist during your college lab evaluation or to capture report screenshots:

- [ ] **1. Home Page (`/`)**: Hero banner, quick tracking lookup bar, category tiles, and featured products.
- [ ] **2. Product Catalog (`/products`)**: Search by keyword, filter by category and price range, sort dropdown, stock badges, and pagination.
- [ ] **3. Product Details (`/products/1`)**: High-contrast product view, stock availability banner, and quantity selector.
- [ ] **4. Login & Registration (`/login`, `/register`)**: Role-based login with quick-fill lab demo credentials.
- [ ] **5. Shopping Cart (`/cart`)**: Quantity increment/decrement, stock validation, and server-calculated totals.
- [ ] **6. Checkout & Simulated Payment (`/checkout`)**: Shipping address form and clearly labeled College Lab Simulated Payment option.
- [ ] **7. Order Confirmation (`/order-confirmation/{id}`)**: Generated `ORD-...` number and `TRK-...` tracking number.
- [ ] **8. Live Visual Tracking Timeline (`/orders/{id}/tracking` & `/track-order`)**: 6-stage horizontal progress bar + chronological vertical audit log with timestamps and locations.
- [ ] **9. Customer Dashboard (`/dashboard`)**: Profile editor, in-app status notification feed, and recent orders.
- [ ] **10. Administrator Dashboard (`/admin`)**: Store KPIs, Low-Stock alerts, Hibernate L2 Cache Hit/Miss telemetry, and Automated Tracking Engine toggle.
- [ ] **11. Admin Product & Multipart Image Upload (`/admin/products/new`)**: Creating/editing a product and uploading a verified JPEG/PNG/WebP image.
- [ ] **12. Admin Order Lifecycle Inspector (`/admin/orders`)**: Advancing an order to its next legal status or triggering a 1-step simulation across active orders.

---

## 7. Future Enhancements

- Email/SMS webhook dispatch alongside in-app notifications.
- PDF invoice generation for delivered orders.
- Multi-warehouse geolocation routing for estimated delivery calculations.

