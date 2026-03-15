-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seckill;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    phone VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存表
CREATE TABLE IF NOT EXISTS inventories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    locked_stock INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_product_id (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化示例商品数据（用于 week2 缓存与压测）
INSERT INTO products (id, name, price, status, created_at, updated_at)
VALUES
    (1, 'iPhone 15 Pro', 7999.00, 'ON_SALE', NOW(), NOW()),
    (2, 'WHU T-Shirt', 99.00, 'ON_SALE', NOW(), NOW()),
    (3, 'Mechanical Keyboard', 299.00, 'ON_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    price = VALUES(price),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO inventories (product_id, total_stock, available_stock, locked_stock, updated_at)
VALUES
    (1, 1000, 1000, 0, NOW()),
    (2, 5000, 5000, 0, NOW()),
    (3, 2000, 2000, 0, NOW())
ON DUPLICATE KEY UPDATE
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock),
    locked_stock = VALUES(locked_stock),
    updated_at = VALUES(updated_at);
