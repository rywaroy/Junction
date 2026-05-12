-- Junction 模板初始 schema
-- 字符集统一 utf8mb4

CREATE DATABASE IF NOT EXISTS `junction` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `junction`;

CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `open_id`    VARCHAR(64)  NOT NULL COMMENT '微信 openid',
    `nickname`   VARCHAR(64)           DEFAULT '' COMMENT '昵称',
    `avatar_url` VARCHAR(512)          DEFAULT '' COMMENT '头像 URL',
    `phone`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';
