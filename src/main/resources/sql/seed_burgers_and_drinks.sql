-- ────────────────────────────────────────────────────────────────────────────
-- Seed data: Classic Burgers + Premium Beef Burgers
-- Run AFTER `migration_storefront_columns.sql` (image_url / original_price /
-- rating / rating_count must exist on `product`).
--
-- Idempotent: clears the previous seed (BRG-* / DRK-*) and the old
-- 'Burgers' / 'Drinks' categories, then inserts the new menu using
-- ON DUPLICATE KEY UPDATE on SKU so re-runs won't duplicate rows.
--
-- Image paths are CLASSPATH references — JavaFX resolves them via
-- ProductImages.load() against /ordermanagementsystem/images/...
-- ────────────────────────────────────────────────────────────────────────────

-- 0) Clear out the old seed ─────────────────────────────────────────────────
DELETE FROM product  WHERE sku LIKE 'BRG-%' OR sku LIKE 'DRK-%';
DELETE FROM category WHERE name IN ('Burgers', 'Drinks');

-- 1) Categories ─────────────────────────────────────────────────────────────
INSERT INTO category (name, description) VALUES
    ('Classic Burgers',      'Everyday burger classics that never miss.'),
    ('Premium Beef Burgers', 'Hand-cut prime beef and chef specials.')
ON DUPLICATE KEY UPDATE description = VALUES(description);

SET @cat_classic = (SELECT id FROM category WHERE name = 'Classic Burgers'      LIMIT 1);
SET @cat_premium = (SELECT id FROM category WHERE name = 'Premium Beef Burgers' LIMIT 1);

-- 2) Classic Burgers ────────────────────────────────────────────────────────
INSERT INTO product
    (sku, name, description, category_id, price, original_price, stock, is_active,
     image_url, rating, rating_count)
VALUES
    ('CLB-001', 'Hamburger',
     'Juicy beef patty, fresh lettuce and tomato in a soft toasted bun.',
     @cat_classic, 99.00, NULL, 60, TRUE,
     '/ordermanagementsystem/images/Classic Burgers/Hamburger.png',
     4.4, 120),

    ('CLB-002', 'Cheeseburger',
     'Beef patty with melted cheddar — the classic everyone loves.',
     @cat_classic, 119.00, 139.00, 55, TRUE,
     '/ordermanagementsystem/images/Classic Burgers/Cheeseburger.png',
     4.6, 184),

    ('CLB-003', 'Double Cheeseburger',
     'Two beef patties, double cheddar, pickles and house sauce.',
     @cat_classic, 169.00, 199.00, 40, TRUE,
     '/ordermanagementsystem/images/Classic Burgers/Double Cheeseburger.png',
     4.7, 142),

    ('CLB-004', 'Bacon Burger',
     'Beef patty stacked with crispy bacon and smoky BBQ.',
     @cat_classic, 159.00, 179.00, 45, TRUE,
     '/ordermanagementsystem/images/Classic Burgers/Bacon Burger.png',
     4.7, 98),

    ('CLB-005', 'Deluxe Burger',
     'Beef patty, cheese, lettuce, tomato, onion and special sauce.',
     @cat_classic, 189.00, 219.00, 35, TRUE,
     '/ordermanagementsystem/images/Classic Burgers/Deluxe Burger.png',
     4.5, 76)
ON DUPLICATE KEY UPDATE
    name           = VALUES(name),
    description    = VALUES(description),
    category_id    = VALUES(category_id),
    price          = VALUES(price),
    original_price = VALUES(original_price),
    stock          = VALUES(stock),
    is_active      = VALUES(is_active),
    image_url      = VALUES(image_url),
    rating         = VALUES(rating),
    rating_count   = VALUES(rating_count);

-- 3) Premium Beef Burgers ───────────────────────────────────────────────────
INSERT INTO product
    (sku, name, description, category_id, price, original_price, stock, is_active,
     image_url, rating, rating_count)
VALUES
    ('PBB-001', 'Angus Burger',
     'Premium Angus beef patty with caramelised onions and aged cheddar.',
     @cat_premium, 289.00, 329.00, 30, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/Angus Burger.png',
     4.8, 134),

    ('PBB-002', 'Wagyu Burger',
     'Buttery Wagyu patty, smoked gouda, truffle aioli on a brioche bun.',
     @cat_premium, 449.00, 499.00, 18, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/Wagyu Burger.png',
     4.9, 87),

    ('PBB-003', 'Steak Burger',
     'Hand-chopped ribeye steak patty, peppered & seared, served pink.',
     @cat_premium, 379.00, NULL, 22, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/Steak Burger.png',
     4.8, 64),

    ('PBB-004', 'BBQ Burger',
     'Premium beef, smoked cheddar, crispy onions and slow-cooked BBQ glaze.',
     @cat_premium, 269.00, 299.00, 28, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/BBQ Burger.png',
     4.7, 92),

    ('PBB-005', 'Mushroom Swiss Burger',
     'Premium beef topped with sautéed mushrooms and Swiss cheese.',
     @cat_premium, 259.00, 289.00, 26, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/Mushroom Swiss Burger.png',
     4.6, 71),

    ('PBB-006', 'Truffle Burger',
     'Prime beef, truffle butter, parmesan crisp and shaved black truffle.',
     @cat_premium, 469.00, NULL, 15, TRUE,
     '/ordermanagementsystem/images/Premium Beef Burgers/Truffle Burger.png',
     4.9, 58)
ON DUPLICATE KEY UPDATE
    name           = VALUES(name),
    description    = VALUES(description),
    category_id    = VALUES(category_id),
    price          = VALUES(price),
    original_price = VALUES(original_price),
    stock          = VALUES(stock),
    is_active      = VALUES(is_active),
    image_url      = VALUES(image_url),
    rating         = VALUES(rating),
    rating_count   = VALUES(rating_count);
