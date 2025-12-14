-- ================================================================
-- 删除 AssetCategory 相关内容
-- ================================================================
-- 步骤1：删除 asset_accounts 表中的 category_id 字段和关联
-- 步骤2：删除 asset_categories 表（谨慎！）
-- 步骤3：清理 net_asset_category 相关的映射表

-- ================================================================
-- 步骤1：删除 asset_accounts 中的 category_id 字段
-- ================================================================

-- 1.1 删除外键约束（如果存在）
-- ALTER TABLE asset_accounts DROP FOREIGN KEY fk_asset_accounts_category;

-- 1.2 删除索引
ALTER TABLE asset_accounts DROP INDEX idx_category_id IF EXISTS;

-- 1.3 删除 category_id 字段
ALTER TABLE asset_accounts DROP COLUMN category_id;

-- ================================================================
-- 步骤2：删除 asset_categories 表
-- ================================================================

DROP TABLE IF EXISTS asset_categories;

-- ================================================================
-- 步骤3：验证删除结果
-- ================================================================

-- 验证 asset_accounts 表结构
SHOW COLUMNS FROM asset_accounts;

-- 验证 asset_categories 表已删除
SHOW TABLES LIKE 'asset_categories';

-- 验证账户数据完整性
SELECT
  COUNT(*) as total_accounts,
  COUNT(asset_type_id) as accounts_with_type
FROM asset_accounts;

-- 按资产大类统计账户
SELECT
  at.chinese_name as 资产大类,
  at.type as 类型代码,
  COUNT(aa.id) as 账户数量
FROM asset_type at
LEFT JOIN asset_accounts aa ON at.id = aa.asset_type_id
GROUP BY at.id, at.chinese_name, at.type
ORDER BY at.display_order;

-- ================================================================
-- 说明
-- ================================================================
-- 删除 asset_categories 表后：
-- 1. asset_accounts 只保留 asset_type_id 字段
-- 2. 账户创建时直接选择 asset_type（8个大类）
-- 3. 不再支持子分类和用户自定义分类
-- 4. 数据库结构更简洁，查询更快
-- ================================================================
