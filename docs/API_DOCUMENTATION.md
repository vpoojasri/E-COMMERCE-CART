# REST API Documentation — E-Commerce Engine with Automated Order Tracking

Base URL (Development): `http://localhost:8080/api`
Authentication Mechanism: Cookie-backed HTTP Session (`JSESSIONID`) with `credentials: 'include'`.

---

## 1. Authentication & Profile Endpoints

### `POST /api/auth/register`
- **Role**: Public
- **Description**: Registers a new customer account with BCrypt-hashed password and establishes an authenticated session.
- **Request Body**:
  ```json
  {
    "fullName": "Alex Rivera",
    "email": "alex@university.edu",
    "password": "Password123",
    "phone": "+1-555-234-5678",
    "shippingAddress": "742 Evergreen Terrace, Apt 4B"
  }
  ```
- **Responses**: `201 Created` (`UserResponse`), `400 Bad Request` (validation or duplicate email).

### `POST /api/auth/login`
- **Role**: Public
- **Description**: Authenticates user credentials and rotates the HTTP session ID to prevent session fixation.
- **Request Body**:
  ```json
  {
    "email": "customer@ecommerce.edu",
    "password": "Customer@123"
  }
  ```
- **Responses**: `200 OK` (`UserResponse`), `401 Unauthorized`.

### `POST /api/auth/logout`
- **Role**: Authenticated (`CUSTOMER` / `ADMIN`)
- **Description**: Invalidates the current HTTP session.
- **Responses**: `200 OK`.

### `GET /api/auth/me`
- **Role**: Authenticated (`CUSTOMER` / `ADMIN`)
- **Description**: Returns the currently logged-in user's profile.
- **Responses**: `200 OK` (`UserResponse`), `401 Unauthorized`.

### `PUT /api/auth/profile`
- **Role**: Authenticated (`CUSTOMER` / `ADMIN`)
- **Description**: Updates the user's full name, phone, and default shipping address.
- **Responses**: `200 OK` (`UserResponse`).

---

## 2. Product Catalog & Category Endpoints

### `GET /api/categories`
- **Role**: Public
- **Description**: Returns all product categories (cached via Hibernate Second-Level Cache and Query Cache).
- **Responses**: `200 OK` (`List<CategoryResponse>`).

### `GET /api/products`
- **Role**: Public
- **Query Parameters**:
  - `keyword` (optional): Search term for name/description
  - `categoryId` (optional): Filter by category ID
  - `minPrice`, `maxPrice` (optional): Price range filter
  - `sortBy` (optional): `newest`, `price_asc`, `price_desc`, `name_asc`, `name_desc`
  - `page` (default `0`), `size` (default `8`)
- **Responses**: `200 OK` (`PagedResponse<ProductResponse>`).

### `GET /api/products/{id}`
- **Role**: Public
- **Description**: Returns full details and stock availability status for a single product.
- **Responses**: `200 OK` (`ProductResponse`), `404 Not Found`.

### `GET /api/uploads/{filename}`
- **Role**: Public
- **Description**: Controlled static-resource endpoint that serves uploaded product images from outside the source tree or generates a fallback SVG illustration if missing.

---

## 3. Shopping Cart Endpoints

### `GET /api/cart`
- **Role**: Authenticated (`CUSTOMER` / `ADMIN`)
- **Description**: Retrieves the user's persisted shopping cart with server-recalculated subtotals and stock validation flags.
- **Responses**: `200 OK` (`CartResponse`).

### `POST /api/cart/items`
- **Role**: Authenticated
- **Request Body**: `{ "productId": 1, "quantity": 2 }`
- **Responses**: `200 OK` (`CartResponse`), `400 Bad Request` (if requested quantity exceeds available stock).

### `PUT /api/cart/items/{id}`
- **Role**: Authenticated (Owner only)
- **Request Body**: `{ "quantity": 3 }`
- **Responses**: `200 OK` (`CartResponse`), `400 Bad Request`, `403 Forbidden`.

### `DELETE /api/cart/items/{id}`
- **Role**: Authenticated (Owner only)
- **Responses**: `200 OK` (`CartResponse`).

---

## 4. Checkout, Orders & Automated Tracking Endpoints

### `POST /api/orders`
- **Role**: Authenticated
- **Description**: Places an order inside an atomic `@Transactional` boundary with pessimistic write locks on inventory, recalculates prices on the server, snapshots product unit prices, generates unique `orderNumber` (`ORD-...`) and `trackingNumber` (`TRK-...`), initializes status to `PLACED`, and records the initial `OrderTrackingHistory` entry.
- **Request Body**:
  ```json
  {
    "shippingAddress": "742 Evergreen Terrace, Innovation Campus",
    "contactPhone": "+1-555-234-5678",
    "paymentMethod": "SIMULATED_CARD_DEMO"
  }
  ```
- **Responses**: `201 Created` (`OrderResponse`), `400 Bad Request` (empty cart or insufficient stock).

### `GET /api/orders/my`
- **Role**: Authenticated
- **Query Parameters**: `page` (default `0`), `size` (default `10`)
- **Responses**: `200 OK` (`PagedResponse<OrderResponse>`).

### `GET /api/orders/{id}` & `GET /api/orders/{id}/tracking`
- **Role**: Authenticated (Order Owner or `ADMIN`)
- **Description**: Returns complete order details, snapshotted line items, estimated delivery date, and chronological `trackingHistory`.
- **Responses**: `200 OK` (`OrderResponse`), `403 Forbidden`, `404 Not Found`.

### `GET /api/tracking/{trackingNumber}`
- **Role**: Authenticated (Order Owner or `ADMIN`)
- **Description**: Looks up an order and its full tracking timeline by its unique tracking number (`TRK-...`).
- **Responses**: `200 OK` (`OrderResponse`), `403 Forbidden`, `404 Not Found`.

### `POST /api/orders/{id}/cancel`
- **Role**: Authenticated (Order Owner)
- **Description**: Cancels an order if its current status is `PLACED` or `CONFIRMED`, restores reserved stock quantities back to the product catalog, and logs a `CANCELLED` tracking event.
- **Request Body**: `{ "reason": "Ordered wrong item" }`
- **Responses**: `200 OK` (`OrderResponse`), `400 Bad Request` (if already packed/shipped/delivered).

### `GET /api/notifications` & `POST /api/notifications/read-all`
- **Role**: Authenticated
- **Description**: Retrieves and marks in-app order status change notifications as read.

---

## 5. Administrator Endpoints (`Role.ADMIN` Required)

Every `/api/admin/**` endpoint enforces server-side `SessionAuthUtil.requireAdmin(session)`.

- `GET /api/admin/dashboard`: Returns KPI counts, low-stock alerts, recent orders, automation toggle status, and live Hibernate L2/Query cache hit/miss metrics.
- `GET /api/admin/users`: Lists all registered users.
- `POST /api/admin/categories`, `PUT /api/admin/categories/{id}`, `DELETE /api/admin/categories/{id}`: Create, update, and delete categories.
- `POST /api/admin/products`, `PUT /api/admin/products/{id}`, `DELETE /api/admin/products/{id}`: Create, update, and delete/deactivate products.
- `POST /api/admin/products/{id}/image` & `POST /api/admin/uploads/image`: Multipart image upload (`file` parameter). Validates extension (`jpg`, `jpeg`, `png`, `webp`), MIME type, binary magic bytes signature, and 5 MB size limit.
- `GET /api/admin/orders`: Paginated list of all customer orders with optional `status` filter.
- `PATCH /api/admin/orders/{id}/status`: Advances or cancels an order. Validates legal state transitions (`PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`, plus `CANCELLED` prior to shipping) and records timestamp, location note, and administrator identity.
- `POST /api/admin/automation/toggle`: Enables or disables the background `@Scheduled` simulated delivery progression engine.
- `POST /api/admin/automation/step`: Immediately advances all active orders by one legal stage for live classroom demonstration.

