-- ==========================================================================
-- Migration: storefront columns for `product` + VAT column for `customer_order`
-- DB: order-management-system (MySQL 8 / Laragon)
--
-- Adds the columns the new customer dashboard expects:
--   product.image_url       (URL for the product image)
--   product.original_price  (strike-through "was" price; NULL = no discount)
--   product.rating          (0.00 - 5.00 avg rating)
--   product.rating_count    (how many ratings contributed)
--   customer_order.tax      (12% VAT amount stored at checkout)
--
-- Safe to re-run: each ADD is guarded by an information_schema check, so
-- columns already present are skipped silently.
-- ==========================================================================

USE `order-management-system`;

DROP PROCEDURE IF EXISTS `__add_col_if_missing`;

DELIMITER $$

CREATE PROCEDURE `__add_col_if_missing`(
    IN p_table   VARCHAR(64),
    IN p_column  VARCHAR(64),
    IN p_ddl     VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME   = p_table
           AND COLUMN_NAME  = p_column
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- ── product ────────────────────────────────────────────────────────────────
CALL `__add_col_if_missing`('product', 'image_url',
    '`image_url` VARCHAR(500) NULL AFTER `description`');

CALL `__add_col_if_missing`('product', 'original_price',
    '`original_price` DECIMAL(12,2) NULL AFTER `price`');

CALL `__add_col_if_missing`('product', 'rating',
    '`rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00 AFTER `stock`');

CALL `__add_col_if_missing`('product', 'rating_count',
    '`rating_count` INT NOT NULL DEFAULT 0 AFTER `rating`');

-- ── customer_order ─────────────────────────────────────────────────────────
CALL `__add_col_if_missing`('customer_order', 'tax',
    '`tax` DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER `subtotal`');

DROP PROCEDURE IF EXISTS `__add_col_if_missing`;
