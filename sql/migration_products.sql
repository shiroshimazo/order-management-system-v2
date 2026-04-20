-- ==========================================================================
-- Migration: products + categories
-- DB: order-management-system (MySQL / Laragon)
--
-- Creates `category` and `product` tables for Phase 4.
-- Safe to re-run: DROPs first.
-- ==========================================================================

USE `order-management-system`;

-- 1) Clean slate ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;

-- 2) Category ---------------------------------------------------------------
CREATE TABLE `category` (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Product ----------------------------------------------------------------
CREATE TABLE `product` (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sku         VARCHAR(40)   NOT NULL UNIQUE,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(500)  NULL,
    category_id INT           NULL,
    price       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    stock       INT           NOT NULL DEFAULT 0,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES `category`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_category ON `product`(category_id);
CREATE INDEX idx_product_name     ON `product`(name);

-- 4) Seed starter categories ------------------------------------------------
INSERT INTO `category` (name, description) VALUES
    ('Electronics',  'Phones, laptops, accessories'),
    ('Accessories',  'Cables, cases, small items'),
    ('Office',       'Office supplies and stationery'),
    ('Home',         'Home and kitchen goods');
