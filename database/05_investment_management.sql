-- =====================================================
-- 投资管理模块数据库迁移脚本
-- 版本: V005
-- 创建日期: 2024-12-13
-- 描述: 为个人理财系统添加投资管理功能
--      1. 修改 asset_categories 表，添加 is_investment 字段
--      2. 更新现有数据，标记投资类资产
--      3. 创建 investment_transactions 表用于记录投资交易
-- =====================================================

-- -----------------------------------------------------
-- 1. 修改 asset_categories 表，添加 is_investment 字段
-- -----------------------------------------------------

-- 检查并添加 is_investment 字段（如果不存在）
SET @column_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'finance'
    AND TABLE_NAME = 'asset_categories'
    AND COLUMN_NAME = 'is_investment'
);

SET @sql_add_column = IF(@column_exists = 0,
  'ALTER TABLE `asset_categories` ADD COLUMN `is_investment` TINYINT(1) DEFAULT 0 COMMENT ''是否为投资类账户'' AFTER `is_system`',
  'SELECT ''Column is_investment already exists'' AS message'
);

PREPARE stmt FROM @sql_add_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加索引（如果不存在）
SET @index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = 'finance'
    AND TABLE_NAME = 'asset_categories'
    AND INDEX_NAME = 'idx_is_investment'
);

SET @sql_add_index = IF(@index_exists = 0,
  'ALTER TABLE `asset_categories` ADD INDEX `idx_is_investment` (`is_investment`)',
  'SELECT ''Index idx_is_investment already exists'' AS message'
);

PREPARE stmt FROM @sql_add_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------
-- 2. 更新现有数据，标记投资类资产
-- -----------------------------------------------------

-- 标记股票投资为投资类
UPDATE `asset_categories`
SET `is_investment` = 1
WHERE `type` = 'STOCKS';

-- 标记退休基金为投资类
UPDATE `asset_categories`
SET `is_investment` = 1
WHERE `type` = 'RETIREMENT_FUND';

-- 标记数字货币为投资类
UPDATE `asset_categories`
SET `is_investment` = 1
WHERE `type` = 'CRYPTOCURRENCY';

-- 其他类型保持默认值 0（非投资类）:
-- - CASH (现金)
-- - INSURANCE (保险)
-- - REAL_ESTATE (房产)
-- - PRECIOUS_METALS (贵金属) - 可根据需要调整
-- - OTHER (其他)

-- -----------------------------------------------------
-- 3. 创建 investment_transactions 表
-- -----------------------------------------------------

DROP TABLE IF EXISTS `investment_transactions`;

CREATE TABLE `investment_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_id` BIGINT NOT NULL COMMENT '资产账户ID',
  `transaction_period` VARCHAR(7) NOT NULL COMMENT '交易期间(YYYY-MM格式，如2024-01)',
  `transaction_type` ENUM('DEPOSIT', 'WITHDRAWAL') NOT NULL COMMENT '交易类型：DEPOSIT-投入(买入/追加投资), WITHDRAWAL-取出(卖出/提取资金)',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额（正数，方向由transaction_type决定）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '交易说明',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),

  -- 外键约束：账户删除时级联删除相关交易记录
  CONSTRAINT `investment_transactions_ibfk_1`
    FOREIGN KEY (`account_id`)
    REFERENCES `asset_accounts` (`id`)
    ON DELETE CASCADE,

  -- 组合索引：按账户和期间查询（最常用查询模式）
  INDEX `idx_account_period` (`account_id`, `transaction_period`),

  -- 索引：按期间查询（用于批量录入页面加载）
  INDEX `idx_period` (`transaction_period`),

  -- 索引：按交易类型查询
  INDEX `idx_transaction_type` (`transaction_type`),

  -- 唯一约束：同一账户、同一期间、同一交易类型只能有一条记录
  UNIQUE KEY `uk_account_period_type` (`account_id`, `transaction_period`, `transaction_type`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='投资交易记录表';

-- -----------------------------------------------------
-- 4. 数据完整性说明
-- -----------------------------------------------------

-- 注意：数据完整性验证在应用层面进行
-- 业务逻辑层将确保只有投资类账户（is_investment = 1）才能创建投资交易记录
-- 验证逻辑：
--   1. 在创建/更新投资交易前，查询账户对应的资产分类
--   2. 检查 asset_categories.is_investment 字段是否为 1
--   3. 如果不是投资类账户，拒绝操作并返回错误信息

-- -----------------------------------------------------
-- 5. 数据验证查询（用于测试和验证）
-- -----------------------------------------------------

-- 查询所有投资类资产分类
-- SELECT id, name, type, is_investment FROM asset_categories WHERE is_investment = 1;

-- 查询投资交易记录表结构
-- SHOW CREATE TABLE investment_transactions;

-- 验证索引
-- SHOW INDEX FROM investment_transactions;

-- 验证触发器
-- SHOW TRIGGERS LIKE 'investment_transactions';

-- =====================================================
-- 迁移脚本结束
-- =====================================================
