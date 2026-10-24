-- ==============================================================================
-- NovaStore Digital Product Store - MySQL Relational Database Schema
-- Optimized for PHP 7.4+ / 8.x and InfinityFree Shared Hosting (phpMyAdmin)
-- Engine: InnoDB | Charset: utf8mb4 | Collation: utf8mb4_unicode_ci
-- ==============================================================================

-- NOTE FOR INFINITYFREE USERS:
-- 1. Create your database in the InfinityFree Control Panel first.
-- 2. Open phpMyAdmin for that database and click "Import".
-- 3. Do NOT add CREATE DATABASE or USE statements here.
-- 4. Do NOT attempt to CREATE USER or GRANT privileges in SQL.

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET TIME_ZONE = "+00:00";

-- ------------------------------------------------------------------------------
-- Table 1: users
-- Stores registered customers and system administrators.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `firebase_uid` VARCHAR(128) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `display_name` VARCHAR(100) NOT NULL DEFAULT 'Customer',
  `password_hash` VARCHAR(255) DEFAULT NULL,
  `role` ENUM('user', 'admin') NOT NULL DEFAULT 'user',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_users_firebase_uid` (`firebase_uid`),
  UNIQUE KEY `uk_users_email` (`email`),
  INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Table 2: categories
-- Stores product categories for storefront pill filtering and admin creation.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `slug` VARCHAR(100) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_categories_name` (`name`),
  UNIQUE KEY `uk_categories_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Table 3: products
-- Stores all digital product listings, pricing in paise, files, and SuperProfile features.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `products` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(120) NOT NULL,
  `slug` VARCHAR(191) NOT NULL,
  `short_description` VARCHAR(250) NOT NULL,
  `description` TEXT NOT NULL,
  `cover_image_path` VARCHAR(255) NOT NULL,
  `private_file_path` VARCHAR(255) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL DEFAULT 'digital-product-file',
  `file_size` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `resource_url` VARCHAR(500) NOT NULL DEFAULT 'https://drive.google.com/',
  `price_in_paise` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `compare_at_price_in_paise` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `category` VARCHAR(100) NOT NULL,
  `support_email` VARCHAR(191) NOT NULL DEFAULT 'ravivadhiya46@gmail.com',
  `support_phone` VARCHAR(50) NOT NULL DEFAULT '+91 9574055968',
  `resource_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `resource_type` VARCHAR(100) NOT NULL DEFAULT 'File',
  `features` LONGTEXT DEFAULT NULL COMMENT 'JSON array of string highlights',
  `testimonials` LONGTEXT DEFAULT NULL COMMENT 'JSON array of review objects',
  `faqs` LONGTEXT DEFAULT NULL COMMENT 'JSON array of Q&A objects',
  `status` ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft',
  `archived_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_products_slug` (`slug`),
  INDEX `idx_products_status` (`status`),
  INDEX `idx_products_category` (`category`),
  INDEX `idx_products_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Table 4: purchases
-- Stores instant demo access grants and prevents duplicate product ownership.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchases` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL,
  `firebase_uid` VARCHAR(128) NOT NULL,
  `product_id` INT UNSIGNED NOT NULL,
  `user_email` VARCHAR(191) NOT NULL,
  `phone_number` VARCHAR(50) DEFAULT '',
  `purchase_type` ENUM('demo') NOT NULL DEFAULT 'demo',
  `status` ENUM('granted') NOT NULL DEFAULT 'granted',
  `price_snapshot_in_paise` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_product_purchase` (`user_id`, `product_id`),
  INDEX `idx_purchases_firebase_uid` (`firebase_uid`),
  INDEX `idx_purchases_product_id` (`product_id`),
  INDEX `idx_purchases_created_at` (`created_at`),
  CONSTRAINT `fk_purchases_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_purchases_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Table 5: admin_audit_logs (Optional Security Audit Trail)
-- Logs administrative actions (product additions, status toggles, category changes).
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `admin_audit_logs` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `admin_user_id` INT UNSIGNED DEFAULT NULL,
  `admin_email` VARCHAR(191) NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `target_type` VARCHAR(50) NOT NULL,
  `target_id` INT UNSIGNED DEFAULT NULL,
  `details` TEXT DEFAULT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_audit_admin_id` (`admin_user_id`),
  INDEX `idx_audit_action` (`action`),
  INDEX `idx_audit_created_at` (`created_at`),
  CONSTRAINT `fk_audit_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- SEED INITIAL DEFAULT CATEGORIES
-- ------------------------------------------------------------------------------
INSERT IGNORE INTO `categories` (`id`, `name`, `slug`) VALUES
(1, 'Prompts', 'prompts'),
(2, 'E-Books', 'e-books'),
(3, 'Templates', 'templates'),
(4, 'Courses', 'courses'),
(5, 'Software', 'software'),
(6, 'Graphics', 'graphics');

-- ------------------------------------------------------------------------------
-- SEED INITIAL DEMO ADMIN USER
-- Default Email: admin@novastore.com
-- Default Firebase UID: dev-uid-adminnovastorecom
-- ------------------------------------------------------------------------------
INSERT IGNORE INTO `users` (`id`, `firebase_uid`, `email`, `display_name`, `role`) VALUES
(1, 'dev-uid-adminnovastorecom', 'admin@novastore.com', 'System Admin', 'admin');

SET FOREIGN_KEY_CHECKS = 1;
