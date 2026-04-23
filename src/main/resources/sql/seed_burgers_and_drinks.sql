-- ────────────────────────────────────────────────────────────────────────────
-- Seed data: Burgers + Drinks for the customer storefront
-- Run AFTER the schema migration that adds image_url / original_price /
-- rating / rating_count to `product`.
-- Idempotent: uses INSERT IGNORE on category names + ON DUPLICATE KEY UPDATE
-- on product SKUs so re-running won't duplicate rows.
-- ────────────────────────────────────────────────────────────────────────────

-- 1) Categories ─────────────────────────────────────────────────────────────
INSERT INTO category (name, description) VALUES
    ('Burgers', 'All burger variations'),
    ('Drinks',  'Soft drinks, juices and shakes')
ON DUPLICATE KEY UPDATE description = VALUES(description);

SET @cat_burgers = (SELECT id FROM category WHERE name = 'Burgers' LIMIT 1);
SET @cat_drinks  = (SELECT id FROM category WHERE name = 'Drinks'  LIMIT 1);

-- 2) Burgers ────────────────────────────────────────────────────────────────
INSERT INTO product
    (sku, name, description, category_id, price, original_price, stock, is_active,
     image_url, rating, rating_count)
VALUES
    ('BRG-001', 'Classic Cheeseburger',
     'Juicy beef patty, melted cheddar, fresh lettuce and tomato in a toasted brioche bun.',
     @cat_burgers, 149.00, 179.00, 50, TRUE,
     'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&q=80',
     4.7, 128),

    ('BRG-002', 'Double Bacon Burger',
     'Two beef patties, crispy bacon, double cheese, smoky BBQ sauce.',
     @cat_burgers, 229.00, 269.00, 35, TRUE,
     'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600&q=80',
     4.8, 96),

    ('BRG-003', 'Spicy Jalapeño Burger',
     'Beef patty with pickled jalapeños, pepperjack cheese and chipotle aioli.',
     @cat_burgers, 189.00, NULL, 40, TRUE,
     'https://images.unsplash.com/photo-1550547660-d9450f859349?w=600&q=80',
     4.5, 73),

    ('BRG-004', 'Mushroom Swiss Burger',
     'Sautéed mushrooms, melted Swiss cheese, garlic mayo on a sesame bun.',
     @cat_burgers, 199.00, 229.00, 28, TRUE,
     'https://images.unsplash.com/photo-1572802419224-296b0aeee0d9?w=600&q=80',
     4.6, 54),

    ('BRG-005', 'Crispy Chicken Burger',
     'Buttermilk-fried chicken fillet, slaw and honey mustard.',
     @cat_burgers, 159.00, 189.00, 60, TRUE,
     'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=600&q=80',
     4.4, 102),

    ('BRG-006', 'Veggie Garden Burger',
     'House-made veggie patty with avocado, tomato and lettuce.',
     @cat_burgers, 169.00, NULL, 25, TRUE,
     'https://images.unsplash.com/photo-1520072959219-c595dc870360?w=600&q=80',
     4.3, 41),

    ('BRG-007', 'Ultimate BBQ Burger',
     'Beef patty, onion rings, smoked cheddar and house BBQ sauce.',
     @cat_burgers, 219.00, 249.00, 30, TRUE,
     'https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600&q=80',
     4.9, 88),

    ('BRG-008', 'Mini Sliders (3 pcs)',
     'Three bite-sized cheeseburgers — perfect for sharing.',
     @cat_burgers, 179.00, 199.00, 45, TRUE,
     'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&q=80',
     4.5, 62)
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

-- 3) Drinks ─────────────────────────────────────────────────────────────────
INSERT INTO product
    (sku, name, description, category_id, price, original_price, stock, is_active,
     image_url, rating, rating_count)
VALUES
    ('DRK-001', 'Classic Cola',
     'Ice-cold cola — the perfect burger pairing.',
     @cat_drinks, 49.00, NULL, 100, TRUE,
     'https://images.unsplash.com/photo-1543253687-c931c8e01820?w=600&q=80',
     4.6, 211),

    ('DRK-002', 'Iced Lemon Tea',
     'Refreshing brewed black tea with fresh lemon.',
     @cat_drinks, 59.00, NULL, 80, TRUE,
     'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&q=80',
     4.4, 145),

    ('DRK-003', 'Chocolate Milkshake',
     'Thick chocolate shake topped with whipped cream.',
     @cat_drinks, 99.00, 119.00, 50, TRUE,
     'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=600&q=80',
     4.8, 178),

    ('DRK-004', 'Fresh Orange Juice',
     'Hand-squeezed oranges, no added sugar.',
     @cat_drinks, 79.00, NULL, 60, TRUE,
     'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=600&q=80',
     4.5, 64),

    ('DRK-005', 'Strawberry Smoothie',
     'Frozen strawberries blended with yogurt and honey.',
     @cat_drinks, 109.00, 129.00, 40, TRUE,
     'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600&q=80',
     4.7, 89),

    ('DRK-006', 'Iced Coffee',
     'Cold brew coffee over ice with a splash of milk.',
     @cat_drinks, 89.00, NULL, 70, TRUE,
     'https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600&q=80',
     4.5, 132),

    ('DRK-007', 'Sparkling Water',
     'Lightly carbonated, with a hint of lime.',
     @cat_drinks, 49.00, NULL, 90, TRUE,
     'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=600&q=80',
     4.2, 38),

    ('DRK-008', 'Mango Shake',
     'Sweet Philippine mangoes, ice and a touch of milk.',
     @cat_drinks, 109.00, 129.00, 45, TRUE,
     'https://images.unsplash.com/photo-1546173159-315724a31696?w=600&q=80',
     4.9, 156)
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
