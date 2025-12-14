-- ================================================================
-- 创建 asset_type 表 - 资产大类定义
-- ================================================================
-- 目的：将资产的8个大类从hard-coded的type字段改为数据库表管理
-- 优势：更灵活，可以动态添加/修改大类，支持国际化

CREATE TABLE IF NOT EXISTS `asset_type` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `type` VARCHAR(50) NOT NULL UNIQUE COMMENT '类型代码（英文大写，如STOCKS）',
  `name` VARCHAR(100) NOT NULL COMMENT '类型名称（英文）',
  `chinese_name` VARCHAR(100) NOT NULL COMMENT '中文名称',
  `description` VARCHAR(500) COMMENT '描述说明',
  `icon` VARCHAR(50) COMMENT '图标（emoji或图标类名）',
  `color` VARCHAR(20) COMMENT '颜色代码（十六进制，如#3B82F6）',
  `is_investment` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为投资类型',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_type (`type`),
  INDEX idx_display_order (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产大类定义表';

-- ================================================================
-- 插入8个资产大类数据
-- ================================================================

INSERT INTO `asset_type`
  (`type`, `name`, `chinese_name`, `description`, `icon`, `color`, `is_investment`, `display_order`, `created_at`, `updated_at`)
VALUES
  -- 1. 现金类 - 非投资
  ('CASH', 'Cash & Equivalents', '现金类',
   '银行存款、手头现金、货币基金、定期存款等流动性资产',
   '💵', '#10B981', FALSE, 1, NOW(), NOW()),

  -- 2. 股票投资 - 投资类
  ('STOCKS', 'Stocks & Funds', '股票投资',
   '国内外股票、基金、ETF、债券等证券投资',
   '📈', '#3B82F6', TRUE, 2, NOW(), NOW()),

  -- 3. 退休基金 - 投资类
  ('RETIREMENT_FUND', 'Retirement Funds', '退休基金',
   '养老保险、401k、IRA等退休账户',
   '🎯', '#8B5CF6', TRUE, 3, NOW(), NOW()),

  -- 4. 保险 - 投资类（有现金价值的保险）
  ('INSURANCE', 'Insurance', '保险',
   '人寿保险、年金保险等具有现金价值的保险产品',
   '🛡️', '#F59E0B', TRUE, 4, NOW(), NOW()),

  -- 5. 房地产 - 投资类
  ('REAL_ESTATE', 'Real Estate', '房地产',
   '自住房产、投资房产、商铺、车位等不动产',
   '🏠', '#EF4444', TRUE, 5, NOW(), NOW()),

  -- 6. 数字货币 - 投资类
  ('CRYPTOCURRENCY', 'Cryptocurrency', '数字货币',
   '比特币、以太坊、稳定币等数字资产',
   '₿', '#F97316', TRUE, 6, NOW(), NOW()),

  -- 7. 贵金属 - 投资类
  ('PRECIOUS_METALS', 'Precious Metals', '贵金属',
   '黄金、白银等贵金属投资',
   '🥇', '#F59E0B', TRUE, 7, NOW(), NOW()),

  -- 8. 其他 - 非投资
  ('OTHER', 'Other Assets', '其他',
   '汽车、收藏品、珠宝首饰、应收账款等其他资产',
   '📦', '#6B7280', FALSE, 8, NOW(), NOW());

-- ================================================================
-- 验证数据
-- ================================================================

-- 查看所有资产大类
SELECT
  id,
  type,
  chinese_name,
  name,
  icon,
  is_investment,
  display_order
FROM asset_type
ORDER BY display_order;

-- 统计投资类型数量
SELECT
  '总大类数' as category,
  COUNT(*) as count
FROM asset_type
UNION ALL
SELECT
  '投资类数',
  COUNT(*)
FROM asset_type
WHERE is_investment = TRUE
UNION ALL
SELECT
  '非投资类数',
  COUNT(*)
FROM asset_type
WHERE is_investment = FALSE;

-- ================================================================
-- 说明
-- ================================================================
-- 1. is_investment = TRUE 的大类（6个）：
--    - STOCKS (股票投资)
--    - RETIREMENT_FUND (退休基金)
--    - INSURANCE (保险)
--    - REAL_ESTATE (房地产)
--    - CRYPTOCURRENCY (数字货币)
--    - PRECIOUS_METALS (贵金属)
--
-- 2. is_investment = FALSE 的大类（2个）：
--    - CASH (现金类) - 流动资产，不算投资
--    - OTHER (其他) - 杂项资产，通常不算投资
--
-- 3. 下一步工作：
--    - 修改 asset_categories 表，添加外键关联到 asset_type
--    - 或者保持现状，asset_categories.type 作为字符串引用 asset_type.type
--    - 前端可以通过 asset_type 表获取大类列表和投资标记
-- ================================================================
