-- V3: two extra physical-fit checks for ForgeAI.
--   1. PSU size: small ITX cases only take SFX power supplies.
--   2. Radiator fit: a liquid cooler's radiator must fit in the case.
-- Hand-filled values are listed in docs/data-sources.md ("V3 values").

-- ---------- 1. PSU size (ordered small -> large like FormFactor: SFX fits where ATX fits)
ALTER TABLE psu_specs  ADD COLUMN form_factor         VARCHAR(10) NOT NULL DEFAULT 'ATX';
ALTER TABLE case_specs ADD COLUMN max_psu_form_factor VARCHAR(10) NOT NULL DEFAULT 'ATX';

UPDATE psu_specs SET form_factor = 'SFX'
WHERE product_id = (SELECT id FROM products WHERE sku = 'FT-PSU-010');   -- Lian Li SP750 V2 (SFX)

UPDATE case_specs SET max_psu_form_factor = 'SFX'
WHERE product_id IN (SELECT id FROM products WHERE sku IN (
    'FT-CASE-014',   -- Fractal Design Ridge
    'FT-CASE-015'    -- Fractal Design Terra
));

-- ---------- 2. Radiator fit
-- Radiator length in mm for LIQUID coolers (NULL for air coolers).
ALTER TABLE cooler_specs ADD COLUMN radiator_mm INTEGER;
-- Largest radiator the case can mount; 0 = no radiator support.
ALTER TABLE case_specs ADD COLUMN max_radiator_mm INTEGER NOT NULL DEFAULT 0;

UPDATE cooler_specs SET radiator_mm = 360 WHERE product_id = (SELECT id FROM products WHERE sku = 'FT-COOL-008'); -- ARCTIC Liquid Freezer III Pro 360
UPDATE cooler_specs SET radiator_mm = 240 WHERE product_id = (SELECT id FROM products WHERE sku = 'FT-COOL-009'); -- NZXT Kraken 240

UPDATE case_specs SET max_radiator_mm = 360
WHERE product_id IN (SELECT id FROM products WHERE sku IN (
    'FT-CASE-001',   -- NZXT H7 Flow (2024)
    'FT-CASE-002',   -- Fractal Design Meshify 2 Compact
    'FT-CASE-004',   -- Lian Li O11 Dynamic EVO RGB
    'FT-CASE-005',   -- Corsair FRAME 4000D RS ARGB
    'FT-CASE-006',   -- NZXT H5 Flow
    'FT-CASE-007',   -- Lian Li Vector V100RX
    'FT-CASE-008',   -- Thermaltake View 270 Plus TG ARGB
    'FT-CASE-009',   -- Montech XR
    'FT-CASE-010',   -- be quiet! Pure Base 501
    'FT-CASE-012'    -- Lian Li A3-mATX
));
UPDATE case_specs SET max_radiator_mm = 280
WHERE product_id IN (SELECT id FROM products WHERE sku IN (
    'FT-CASE-003',   -- Fractal Design Pop Air Mini
    'FT-CASE-013'    -- Cooler Master NR200
));
UPDATE case_specs SET max_radiator_mm = 240
WHERE product_id = (SELECT id FROM products WHERE sku = 'FT-CASE-011');  -- Cooler Master MasterBox Q300L
-- Ridge (FT-CASE-014) and Terra (FT-CASE-015) keep 0: no radiator support.

-- A liquid cooler must have a radiator size, an air cooler must not.
ALTER TABLE cooler_specs ADD CONSTRAINT chk_cooler_radiator
    CHECK ((cooler_type = 'LIQUID' AND radiator_mm IS NOT NULL) OR (cooler_type = 'AIR' AND radiator_mm IS NULL));
