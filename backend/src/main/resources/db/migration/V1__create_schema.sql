-- V1: initial FutureTech schema.
-- Flyway runs this once and records it in flyway_schema_history.
-- Never edit this file after it has run; add a new V2__..., V3__... file instead.
-- Enum-like columns (role, product_type, status, ...) are VARCHAR holding the Java enum name.

-- ============================================================
-- Customers and admins (one table, told apart by role)
-- ============================================================
CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(30),
    role           VARCHAR(20)  NOT NULL,              -- CUSTOMER / ADMIN
    points_balance INTEGER      NOT NULL DEFAULT 0,    -- cached sum of point_transactions
    created_at     TIMESTAMP    NOT NULL
);

-- ============================================================
-- Catalog
-- ============================================================
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,          -- URL-friendly name, e.g. "graphics-cards"
    description TEXT
);

CREATE TABLE products (
    id                BIGSERIAL     PRIMARY KEY,
    sku               VARCHAR(50)   NOT NULL UNIQUE,
    name              VARCHAR(200)  NOT NULL,
    brand             VARCHAR(100)  NOT NULL,
    description       TEXT,
    image_url         VARCHAR(500),
    price             NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock_quantity    INTEGER       NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category_id       BIGINT        NOT NULL REFERENCES categories(id),
    product_type      VARCHAR(30)   NOT NULL,          -- CPU, GPU, LAPTOP, ACCESSORY, ...
    warranty_months   INTEGER       NOT NULL DEFAULT 12 CHECK (warranty_months >= 0),
    performance_score INTEGER       CHECK (performance_score BETWEEN 0 AND 100),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,  -- false = hidden (never delete products)
    created_at        TIMESTAMP     NOT NULL
);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_type     ON products(product_type);

-- ============================================================
-- PC part specs: one table per part type, 1:1 with products.
-- product_id is both the primary key and the foreign key.
-- ============================================================
CREATE TABLE cpu_specs (
    product_id          BIGINT           PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    socket              VARCHAR(20)      NOT NULL,     -- e.g. AM5, LGA1700
    cores               INTEGER          NOT NULL,
    threads             INTEGER          NOT NULL,
    base_clock_ghz      DOUBLE PRECISION NOT NULL,
    boost_clock_ghz     DOUBLE PRECISION,
    tdp_watts           INTEGER          NOT NULL,
    integrated_graphics BOOLEAN          NOT NULL
);

CREATE TABLE motherboard_specs (
    product_id  BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    socket      VARCHAR(20) NOT NULL,
    chipset     VARCHAR(30) NOT NULL,
    form_factor VARCHAR(20) NOT NULL,                  -- MINI_ITX / MICRO_ATX / ATX
    ram_type    VARCHAR(10) NOT NULL,                  -- DDR4 / DDR5
    ram_slots   INTEGER     NOT NULL,
    max_ram_gb  INTEGER     NOT NULL,
    m2_slots    INTEGER     NOT NULL
);

CREATE TABLE ram_specs (
    product_id  BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    ram_type    VARCHAR(10) NOT NULL,
    capacity_gb INTEGER     NOT NULL,                  -- total for the kit
    modules     INTEGER     NOT NULL,                  -- number of sticks
    speed_mhz   INTEGER     NOT NULL
);

CREATE TABLE gpu_specs (
    product_id BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    chipset    VARCHAR(50) NOT NULL,                   -- e.g. GeForce RTX 4070
    vram_gb    INTEGER     NOT NULL,
    length_mm  INTEGER     NOT NULL,
    tdp_watts  INTEGER     NOT NULL
);

CREATE TABLE storage_specs (
    product_id   BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    storage_type VARCHAR(20) NOT NULL,                 -- NVME_SSD / SATA_SSD / HDD
    capacity_gb  INTEGER     NOT NULL
);

CREATE TABLE psu_specs (
    product_id        BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    wattage           INTEGER     NOT NULL,
    efficiency_rating VARCHAR(30) NOT NULL,            -- e.g. 80+ Gold
    modular           BOOLEAN     NOT NULL
);

CREATE TABLE case_specs (
    product_id                  BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    max_motherboard_form_factor VARCHAR(20) NOT NULL,  -- largest board that fits
    max_gpu_length_mm           INTEGER     NOT NULL,
    max_cooler_height_mm        INTEGER     NOT NULL
);

CREATE TABLE cooler_specs (
    product_id  BIGINT      PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    cooler_type VARCHAR(10) NOT NULL,                  -- AIR / LIQUID
    height_mm   INTEGER                                -- only for AIR coolers
);

-- A cooler supports several sockets, so they live in their own table.
CREATE TABLE cooler_supported_sockets (
    cooler_id BIGINT      NOT NULL REFERENCES cooler_specs(product_id) ON DELETE CASCADE,
    socket    VARCHAR(20) NOT NULL,
    PRIMARY KEY (cooler_id, socket)
);

-- Pre-built laptops and desktops (display and filtering only, no compatibility rules).
CREATE TABLE computer_specs (
    product_id         BIGINT           PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    cpu_model          VARCHAR(100)     NOT NULL,
    gpu_model          VARCHAR(100)     NOT NULL,
    ram_gb             INTEGER          NOT NULL,
    storage_gb         INTEGER          NOT NULL,
    screen_size_inches DOUBLE PRECISION,               -- laptops only
    operating_system   VARCHAR(50)
);

-- ============================================================
-- Orders (fake checkout: no payment data is stored)
-- ============================================================
CREATE TABLE orders (
    id                 BIGSERIAL     PRIMARY KEY,
    user_id            BIGINT        NOT NULL REFERENCES users(id),
    status             VARCHAR(20)   NOT NULL,         -- PLACED / SHIPPED / DELIVERED / CANCELLED
    total_amount       NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
    -- shipping details are copied at checkout so old orders never change
    shipping_full_name VARCHAR(200)  NOT NULL,
    shipping_address   VARCHAR(300)  NOT NULL,
    shipping_city      VARCHAR(100)  NOT NULL,
    shipping_phone     VARCHAR(30)   NOT NULL,
    created_at         TIMESTAMP     NOT NULL
);
CREATE INDEX idx_orders_user ON orders(user_id);

CREATE TABLE order_items (
    id         BIGSERIAL     PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT        NOT NULL REFERENCES products(id),
    quantity   INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0)  -- price at time of purchase
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ============================================================
-- Warranty: one per order line. Status (active/expired) is computed from expiry_date.
-- ============================================================
CREATE TABLE warranties (
    id            BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT    NOT NULL UNIQUE REFERENCES order_items(id) ON DELETE CASCADE,
    start_date    DATE      NOT NULL,
    expiry_date   DATE      NOT NULL,
    CHECK (expiry_date >= start_date)
);

-- ============================================================
-- Points ledger: every change to a user's points is one row.
-- ============================================================
CREATE TABLE point_transactions (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    order_id   BIGINT      REFERENCES orders(id),     -- null for manual admin adjustments
    points     INTEGER     NOT NULL,                  -- positive = earned, negative = removed
    reason     VARCHAR(30) NOT NULL,                  -- EARNED_PURCHASE / ADMIN_ADJUSTMENT
    created_at TIMESTAMP   NOT NULL
);
CREATE INDEX idx_point_transactions_user ON point_transactions(user_id);
