-- ==========================================================================
-- Migration: avatar BLOB for `user_customer`
-- DB: order-management-system (MySQL 8 / Laragon)
--
-- Adds two columns the customer Settings page uses to store an uploaded
-- profile photo directly on the user row:
--   user_customer.avatar_data   MEDIUMBLOB  (raw image bytes, NULL by default)
--   user_customer.avatar_mime   VARCHAR(64) (e.g. "image/png" — used to round-
--                                            trip the file type for previews)
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

-- ── user_customer ──────────────────────────────────────────────────────────
CALL `__add_col_if_missing`('user_customer', 'avatar_data',
    '`avatar_data` MEDIUMBLOB NULL AFTER `phone`');

CALL `__add_col_if_missing`('user_customer', 'avatar_mime',
    '`avatar_mime` VARCHAR(64) NULL AFTER `avatar_data`');

DROP PROCEDURE IF EXISTS `__add_col_if_missing`;
