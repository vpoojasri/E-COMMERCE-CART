-- ============================================================================
-- E-Commerce Engine with Automated Order Tracking
-- Sample Data Seed Script (Categories & Products)
-- Note: The application DataSeeder also seeds sample categories, products,
-- and safe configurable demo accounts automatically if the database is empty.
-- ============================================================================

USE ecommerce_tracking_db;

INSERT INTO categories (id, name, description) VALUES
(1, 'Electronics & Gadgets', 'High-performance computing, audio equipment, and smart electronics.'),
(2, 'Smart Home & IoT', 'Automated sensors, smart lighting, and connected home appliances.'),
(3, 'Developer Gear & Accessories', 'Mechanical keyboards, ergonomic stands, docks, and lab hardware.'),
(4, 'Technical Books & Media', 'Computer science textbooks, software architecture guides, and reference manuals.')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO products (id, name, description, price, stock_quantity, image_path, category_id, active, created_at, updated_at) VALUES
(1, 'Pro Wireless Noise-Canceling Headphones', 'Studio-grade active noise cancellation, 40-hour battery life, USB-C fast charging, and multipoint Bluetooth 5.3 connectivity.', 249.99, 25, '/api/uploads/sample-headphones.svg', 1, b'1', NOW(), NOW()),
(2, 'Tactile Mechanical Keyboard RGB 75%', 'Hot-swappable gasket mount mechanical keyboard with pre-lubed tactile switches, PBT keycaps, and aluminum CNC frame.', 139.50, 18, '/api/uploads/sample-keyboard.svg', 3, b'1', NOW(), NOW()),
(3, 'Ultra-Wide 34" WQHD Curved Monitor', '3440x1440 resolution, 144Hz refresh rate, 99% sRGB color accuracy, and built-in USB-C 90W power delivery hub.', 499.00, 8, '/api/uploads/sample-monitor.svg', 1, b'1', NOW(), NOW()),
(4, 'Smart Matter Thread Home Hub Pro', 'Local-first smart home automation hub supporting Zigbee 3.0, Matter, Thread, and real-time energy consumption monitoring.', 89.99, 32, '/api/uploads/sample-smarthub.svg', 2, b'1', NOW(), NOW()),
(5, 'Thunderbolt 4 12-in-1 Workstation Dock', 'Dual 4K 60Hz display output, 2.5GbE Ethernet, SD UHS-II card reader, and 100W host charging for developer laptops.', 199.00, 4, '/api/uploads/sample-dock.svg', 3, b'1', NOW(), NOW()),
(6, 'Enterprise Distributed Systems Architecture (Hardcover)', 'Comprehensive reference covering consensus algorithms, database replication, caching strategies, and resilient microservices.', 54.95, 45, '/api/uploads/sample-book.svg', 4, b'1', NOW(), NOW()),
(7, 'Smart Wi-Fi Ambient LED Light Bar Pair', 'Dynamic RGBIC ambient desk lighting with music sync, circadian rhythm automation, and voice assistant support.', 64.00, 15, '/api/uploads/sample-lightbar.svg', 2, b'1', NOW(), NOW()),
(8, 'Limited Edition FPGA Development Board', 'Cyclone V FPGA evaluation kit with onboard SDRAM, HDMI output, GPIO headers, and logic analyzer headers for digital design labs.', 175.00, 0, '/api/uploads/sample-fpga.svg', 1, b'1', NOW(), NOW())
ON DUPLICATE KEY UPDATE price = VALUES(price), stock_quantity = VALUES(stock_quantity);

