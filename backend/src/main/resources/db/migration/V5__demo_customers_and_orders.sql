-- V5: demo customers with order history, warranties, points and one saved build,
-- so the customer and admin pages have something to show.
--
-- * All demo customers use the password  Demo@2026  (stored only as a BCrypt hash).
--   These are throw-away demo accounts, not secrets. The ADMIN account is NOT created here:
--   it is created at startup from the ADMIN_EMAIL / ADMIN_PASSWORD environment variables.
-- * Points rule: 1 point per whole dollar of the order total (FLOOR(total)).
-- * Totals, warranty dates and point balances are CALCULATED below in SQL, not typed in,
--   so they always match the product prices from V2.

-- ---------- Customers
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, points_balance, created_at) VALUES
    ('lina.haddad@example.com', '$2a$10$8XyLMhsttZaN37pDZubVEex/8yB4fwHqAx/19dS187vIyD6wWL4WC', 'Lina', 'Haddad', '+961 70 000 001', 'CUSTOMER', 0, '2024-08-01 10:00:00'),
    ('omar.khalil@example.com', '$2a$10$8XyLMhsttZaN37pDZubVEex/8yB4fwHqAx/19dS187vIyD6wWL4WC', 'Omar', 'Khalil', '+961 70 000 002', 'CUSTOMER', 0, '2025-11-01 18:30:00'),
    ('sara.mansour@example.com', '$2a$10$8XyLMhsttZaN37pDZubVEex/8yB4fwHqAx/19dS187vIyD6wWL4WC', 'Sara', 'Mansour', '+961 70 000 003', 'CUSTOMER', 0, '2026-06-01 09:15:00');

-- ---------- Orders (total_amount starts at 0 and is calculated from the items further down)
INSERT INTO orders (user_id, status, total_amount, shipping_full_name, shipping_address, shipping_city, shipping_phone, created_at)
SELECT u.id, o.status, 0, u.first_name || ' ' || u.last_name, o.address, o.city, u.phone, o.created_at::timestamp
FROM (VALUES
    -- old order: its warranties have already expired (good for the warranty page)
    ('lina.haddad@example.com', 'DELIVERED', '12 Hamra Street',   'Beirut',  '2024-08-20 14:05:00'),
    ('lina.haddad@example.com', 'DELIVERED', '12 Hamra Street',   'Beirut',  '2026-03-02 11:40:00'),
    -- a full custom PC build bought as parts
    ('omar.khalil@example.com', 'DELIVERED', '5 Main Road',       'Jounieh', '2025-11-28 20:15:00'),
    ('omar.khalil@example.com', 'SHIPPED',   '5 Main Road',       'Jounieh', '2026-11-10 16:00:00'),
    ('sara.mansour@example.com', 'CANCELLED', '33 Riad El Solh',  'Beirut',  '2026-06-15 13:20:00'),
    ('sara.mansour@example.com', 'PLACED',    '33 Riad El Solh',  'Beirut',  '2026-11-12 09:45:00')
) AS o(email, status, address, city, created_at)
JOIN users u ON u.email = o.email;

-- ---------- Order items (unit_price copied from the product, like a real checkout)
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT o.id, p.id, i.qty, p.price
FROM (VALUES
    ('lina.haddad@example.com',  '2024-08-20 14:05:00', 'FT-LAPT-004', 1),  -- Acer Nitro V 15
    ('lina.haddad@example.com',  '2024-08-20 14:05:00', 'FT-PERI-001', 1),  -- Logitech G502 X
    ('lina.haddad@example.com',  '2026-03-02 11:40:00', 'FT-DESK-003', 1),  -- FutureTech Forge Pro
    ('lina.haddad@example.com',  '2026-03-02 11:40:00', 'FT-PERI-004', 1),  -- HyperX Cloud III
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-CPU-003',  1),  -- Ryzen 7 9800X3D
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-MOTH-002', 1),  -- MSI B850 Edge WiFi
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-RAM-001',  1),  -- Trident Z5 Neo 32GB
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-GPU-004',  1),  -- RTX 5070 Ti 16GB
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-STOR-001', 1),  -- WD Black SN850X 2TB
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-PSU-002',  1),  -- Corsair RM850x
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-CASE-001', 1),  -- NZXT H7 Flow
    ('omar.khalil@example.com',  '2025-11-28 20:15:00', 'FT-COOL-008', 1),  -- ARCTIC Liquid Freezer III 360
    ('omar.khalil@example.com',  '2026-11-10 16:00:00', 'FT-ACCE-001', 2),  -- ARCTIC MX-6 thermal paste x2
    ('omar.khalil@example.com',  '2026-11-10 16:00:00', 'FT-ACCE-002', 1),  -- SteelSeries QcK mouse pad
    ('sara.mansour@example.com', '2026-06-15 13:20:00', 'FT-GPU-012',  1),  -- ASUS Dual RTX 5060 (cancelled)
    ('sara.mansour@example.com', '2026-11-12 09:45:00', 'FT-LAPT-001', 1)   -- MacBook Air 13 M4
) AS i(email, created_at, sku, qty)
JOIN users u    ON u.email = i.email
JOIN orders o   ON o.user_id = u.id AND o.created_at = i.created_at::timestamp
JOIN products p ON p.sku = i.sku;

-- ---------- Order totals = sum of their lines
UPDATE orders o
SET total_amount = (SELECT SUM(oi.quantity * oi.unit_price) FROM order_items oi WHERE oi.order_id = o.id);

-- ---------- Warranties: one per order line, from the order date + the product's warranty period.
-- Cancelled orders get no warranty.
INSERT INTO warranties (order_item_id, start_date, expiry_date)
SELECT oi.id, o.created_at::date, (o.created_at + make_interval(months => p.warranty_months))::date
FROM order_items oi
JOIN orders o   ON o.id = oi.order_id
JOIN products p ON p.id = oi.product_id
WHERE o.status <> 'CANCELLED';

-- ---------- Points: earned at checkout for every order ...
INSERT INTO point_transactions (user_id, order_id, points, reason, created_at)
SELECT o.user_id, o.id, FLOOR(o.total_amount)::int, 'EARNED_PURCHASE', o.created_at
FROM orders o;

-- ... and taken back by an admin when an order is cancelled.
INSERT INTO point_transactions (user_id, order_id, points, reason, created_at)
SELECT o.user_id, o.id, -FLOOR(o.total_amount)::int, 'ADMIN_ADJUSTMENT', o.created_at + INTERVAL '1 day'
FROM orders o
WHERE o.status = 'CANCELLED';

-- Cached balance = sum of the user's ledger.
UPDATE users u
SET points_balance = COALESCE((SELECT SUM(pt.points) FROM point_transactions pt WHERE pt.user_id = u.id), 0);

-- ---------- One saved ForgeAI build (a mid-range gaming PC Sara is still thinking about)
INSERT INTO saved_builds (user_id, name, source, use_case, budget, created_at)
SELECT id, 'My 1440p gaming PC', 'MANUAL', 'gaming', 1500.00, '2026-11-01 20:00:00'
FROM users WHERE email = 'sara.mansour@example.com';

INSERT INTO build_items (build_id, product_id, quantity)
SELECT b.id, p.id, 1
FROM saved_builds b
JOIN users u ON u.id = b.user_id AND u.email = 'sara.mansour@example.com'
JOIN products p ON p.sku IN ('FT-CPU-001', 'FT-MOTH-014', 'FT-RAM-004', 'FT-GPU-001',
                             'FT-STOR-001', 'FT-PSU-012', 'FT-CASE-005', 'FT-COOL-002');
