-- ==========================================================================
-- Migration: customer_order + order_item
-- DB: order-management-system (MySQL / Laragon)
--
-- Requires: user_customer, product
-- ==========================================================================

USE `order-management-system`;

DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `customer_order`;

CREATE TABLE `customer_order` (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    order_code          VARCHAR(20)   NOT NULL UNIQUE,          -- e.g. ORD-000123
    customer_id         INT           NOT NULL,
    status              ENUM('PENDING','PROCESSING','SHIPPED','DELIVERED','CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total               DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    shipping_address    VARCHAR(255)  NULL,
    contact_number      VARCHAR(20)   NULL,
    notes               VARCHAR(500)  NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id) REFERENCES `user_customer`(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_customer ON `customer_order`(customer_id);
CREATE INDEX idx_order_status   ON `customer_order`(status);

CREATE TABLE `order_item` (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT           NOT NULL,
    product_id      INT           NULL,                          -- nullable if product deleted
    product_name    VARCHAR(120)  NOT NULL,                      -- snapshot
    product_sku     VARCHAR(40)   NOT NULL,                      -- snapshot
    unit_price      DECIMAL(12,2) NOT NULL,                      -- snapshot
    quantity        INT           NOT NULL,
    line_total      DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_item_order
        FOREIGN KEY (order_id)   REFERENCES `customer_order`(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES `product`(id)        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_item_order   ON `order_item`(order_id);
CREATE INDEX idx_item_product ON `order_item`(product_id);
