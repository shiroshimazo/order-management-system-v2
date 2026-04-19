-- ==========================================================================
-- Migration: split `users` into `user_customer` + `admin`
-- DB: order-management-system (MySQL / Laragon)
--
-- DEV-SAFE: drops old `users` table. If you have data to keep, export first.
-- ==========================================================================

USE `order-management-system`;

-- 1) Clean slate ------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `user_customer`;

-- 2) Customer table (sign-up flow populates this) ---------------------------
CREATE TABLE `user_customer` (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,                 -- BCrypt hash
    full_name   VARCHAR(120) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    phone       VARCHAR(20)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Admin table (internal, seeded by SeedAdmin.java) -----------------------
CREATE TABLE `admin` (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    admin_code      VARCHAR(20)  NOT NULL UNIQUE,       -- e.g. ADMIN-001
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,              -- BCrypt hash
    email           VARCHAR(120) NOT NULL UNIQUE,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(20)  NULL,
    admin_level     ENUM('SUPER_ADMIN','MANAGER','STAFF') NOT NULL DEFAULT 'STAFF',
    position        VARCHAR(60)  NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME     NULL,
    created_by      INT          NULL,
    CONSTRAINT fk_admin_created_by
        FOREIGN KEY (created_by) REFERENCES `admin`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Seed the first SUPER_ADMIN ---------------------------------------------
-- Run SeedAdmin.java (tools package) ONCE after creating the tables.
-- It inserts:
--   admin_code   = ADMIN-001
--   username     = superadmin
--   password     = Admin@123   (BCrypt-hashed)
--   email        = shiroshiimazo@gmail.com
--   full_name    = Super Admin
--   admin_level  = SUPER_ADMIN
--   position     = System Administrator
