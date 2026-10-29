# E-Commerce Engine with Automated Order Tracking

A complete, college-lab-ready full-stack web application built with **Java 21**, **Spring MVC (Non-Boot)**, **Hibernate ORM 6.5 (with Second-Level & Query Caching)**, **MySQL 8.0 (with automatic zero-config H2 MySQL-mode fallback)**, and **React 18 + React Router DOM 6**.

---

## Key Features Implemented

1. **User Authentication & Role-Based Authorization (Module A)**
   - Customer registration and login with **BCrypt** password hashing (`spring-security-crypto`).
   - Session fixation protection and server-side role verification (`CUSTOMER` vs `ADMIN`) on every administrative and customer endpoint.
   - Safe administrator bootstrap via environment variables (`ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`) — only created when no admin exists in the database.

2. **Product Catalog & Search/Filter Engine (Module B)**
   - Responsive product cards with stock badges (`In Stock`, `Low Stock`, `Out of Stock — Currently Unavailable`).
   - Search by keyword, filter by category and min/max price, sort by price or name, and server-side pagination.

3. **Persistent Shopping Cart & Transactional Checkout (Modules C & D)**
   - Database-persisted cart per user with real-time stock validation.
   - All subtotals and order totals are recalculated on the server (browser totals are never trusted).
   - `@Transactional` order placement with **pessimistic write locking** (`LockModeType.PESSIMISTIC_WRITE`) to prevent concurrent overselling.
   -Clearly labeled **Simulated College Lab Payment Gateway** for classroom demonstration.

4. **Automated Order Tracking Engine (Module E — Core Feature)**
   - Enforces the strict lifecycle:
     `PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED` (plus `CANCELLED` where permitted).
   - Generates a unique Order Number (`ORD-YYYYMMDD-XXXXXX`) and Tracking Number (`TRK-XXXXXXXXXX`) for every order.
   - Persists every status transition in `order_tracking_history` with timestamp, description, facility location note, and updated-by identity.
   - Generates in-app customer notifications on every status change.
   - Includes a configurable **Simulated Automated Progression Scheduler** (`@Scheduled`) that is **disabled by default** in normal use and can be toggled live or stepped manually from the Administrator Dashboard.

5. **Secure Multipart File Uploads (Module F)**
   - Uses Spring MVC `StandardServletMultipartResolver`.
   - Validates file extension (`jpg`, `jpeg`, `png`, `webp`), MIME type, **binary magic-byte file signature**, and maximum size (5 MB).
   - Generates collision-free UUID filenames, blocks path traversal (`..`), stores files outside the source tree (`./uploads/products`), and serves them via `/api/uploads/{filename}` with automatic SVG fallback.

6. **Hibernate ORM & Second-Level Caching (Module G)**
   - Configured with `hibernate-jcache` and **Caffeine JCache Provider**.
   - Caches `Category` (`categoriesCache`), `Product` (`productsCache`), and category lookup queries (`categoriesQueryCache`).
   - Live cache hit/miss telemetry is exposed on the Administrator Dashboard.

---

## Project Directory Structure

```text
project/
├── .env.example                 # Environment variable configuration template
├── .gitignore                   # Git ignore rules for Java, Maven, Node, and uploads
├── README.md                    # Complete setup, execution, and testing guide
├── database/
│   ├── schema.sql               # MySQL 8.0 DDL schema with primary/foreign keys & indexes
│   └── seed.sql                 # Sample categories and products SQL seed script
├── docs/
│   ├── API_DOCUMENTATION.md     # Complete REST endpoint reference
│   └── PROJECT_REPORT.md        # Architecture report & lab screenshot checklist
├── backend/                     # Spring MVC + Hibernate ORM + Embedded Tomcat WAR project
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ecommerce/
│       │   ├── TomcatServerLauncher.java
│       │   ├── config/          # RootConfig, WebMvcConfig, WebAppInitializer, DataSeeder
│       │   ├── controller/      # Auth, Product, Cart, Order, Admin, Upload controllers
│       │   ├── service/         # Auth, Catalog, Cart, OrderTracking, FileStorage services
│       │   ├── dao/             # Hibernate EntityManager DAOs
│       │   ├── entity/          # JPA/Hibernate entities & OrderStatus state machine
│       │   ├── dto/             # Validated request & response DTOs
│       │   ├── exception/       # GlobalExceptionHandler & custom HTTP exceptions
│       │   ├── scheduler/       # OrderTrackingAutomationScheduler
│       │   └── util/            # SessionAuthUtil
│       └── test/java/com/ecommerce/
│           └── EcommerceEngineIntegrationTest.java
└── frontend/                    # React 18 + Vite + React Router DOM 6 SPA
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api.js               # Centralized fetch client
        ├── App.jsx              # 18 client-side routes
        ├── context/AppContext.jsx
        ├── components/          # Navbar, Footer, ProductCard, TrackingTimeline
        └── pages/               # Catalog, Auth, Cart/Checkout, Customer, Admin pages
```

---

## Prerequisites & Installed Versions

- **Java JDK**: Java 21+ (Verified with OpenJDK 21)
- **Apache Maven**: 3.9+
- **Node.js & npm**: Node 18+ / 24+
- **Database**: MySQL 8.0+ (or automatic H2 MySQL-compatibility mode if MySQL credentials are not set)

---

## Database Setup Instructions (MySQL 8.0)

### Option A: Connect to Your Local MySQL 8.0 Server
1. Open MySQL Workbench or the MySQL CLI and execute the schema script (or let the app create the database automatically):
   ```sql
   SOURCE database/schema.sql;
   SOURCE database/seed.sql;
   ```
2. Set the environment variables in PowerShell before starting the backend:
   ```powershell
   $env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
   $env:DB_URL="jdbc:mysql://localhost:3306/ecommerce_tracking_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME="root"
   $env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
   $env:HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"
   ```

### Option B: Zero-Config Lab Mode (Automatic Fallback)
If `DB_URL` is not set and local MySQL requires a custom password not yet provided in the environment, `RootConfig` automatically falls back to a persistent file-based database (`./data/ecommerce_tracking_db`) in **MySQL Compatibility Mode** and seeds all sample categories, products, demo accounts, and a sample multi-stage tracked order automatically.

---

## How to Run the Application (Windows / VS Code)

### Step 1: Start the Spring MVC Backend (Port 8080)
From the project root (`c:\Users\vpooj\OneDrive\Desktop\project`):
```powershell
mvn -f backend/pom.xml compile exec:java
```
*(Alternatively, deploy `backend/target/ecommerce-order-tracking.war` to an external Apache Tomcat 10.1 `webapps/` folder.)*

### Step 2: Start the React Frontend (Port 5173)
Open a second terminal in the project root:
```powershell
npm.cmd --prefix frontend install
npm.cmd --prefix frontend run dev
```
Open **`http://localhost:5173`** in your browser.

---

## Safe Administrator & Demo Accounts

When the application starts on an empty database, `DataSeeder` safely provisions:
- **Administrator Account**:
  - **Email**: `admin@ecommerce.edu` (configurable via `ADMIN_INITIAL_EMAIL`)
  - **Password**: `Admin@123` (configurable via `ADMIN_INITIAL_PASSWORD`)
- **Demo Customer Account**:
  - **Email**: `customer@ecommerce.edu`
  - **Password**: `Customer@123`
  - Includes a pre-seeded order in `SHIPPED` status so the tracking timeline is immediately visible upon login.

---

## How to Run the Automated Tests

Run the full backend JUnit 5 + Spring Test integration suite:
```powershell
mvn -f backend/pom.xml test
```
Build and verify the production React frontend bundle:
```powershell
npm.cmd --prefix frontend run build
```

