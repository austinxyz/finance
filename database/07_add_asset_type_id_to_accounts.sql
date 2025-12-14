-- ================================================================
-- 为 asset_accounts 表添加 asset_type_id 字段
-- ================================================================
-- 目的：直接引用 asset_type 表，避免通过 category_id 间接查询
-- 关系链：asset_accounts.category_id -> asset_categories.type -> asset_type.id

-- 第一步：添加 asset_type_id 字段（允许为NULL，稍后填充数据）
ALTER TABLE `asset_accounts`
ADD COLUMN `asset_type_id` BIGINT NULL COMMENT '资产大类ID，引用asset_type表'
AFTER `category_id`;

-- 第二步：添加外键索引（先不添加约束，等数据填充后再加）
ALTER TABLE `asset_accounts`
ADD INDEX `idx_asset_type_id` (`asset_type_id`);

-- 第三步：根据现有的 category_id 计算并填充 asset_type_id
-- 逻辑：asset_accounts -> asset_categories (通过category_id) -> asset_type (通过type字段匹配)
UPDATE `asset_accounts` aa
INNER JOIN `asset_categories` ac ON aa.category_id = ac.id
INNER JOIN `asset_type` at ON ac.type = at.type
SET aa.asset_type_id = at.id;

-- 第四步：验证数据填充情况
SELECT
  '总账户数' as description,
  COUNT(*) as count
FROM asset_accounts
UNION ALL
SELECT
  '已填充asset_type_id的账户数',
  COUNT(*)
FROM asset_accounts
WHERE asset_type_id IS NOT NULL
UNION ALL
SELECT
  'asset_type_id为NULL的账户数',
  COUNT(*)
FROM asset_accounts
WHERE asset_type_id IS NULL;

-- 第五步：查看每个资产大类的账户分布
SELECT
  at.chinese_name as 资产大类,
  at.type as 类型代码,
  at.is_investment as 是否投资类,
  COUNT(aa.id) as 账户数量
FROM asset_type at
LEFT JOIN asset_accounts aa ON at.id = aa.asset_type_id
GROUP BY at.id, at.chinese_name, at.type, at.is_investment
ORDER BY at.display_order;

-- 第六步（可选）：如果所有数据都已填充，可以将字段改为NOT NULL并添加外键约束
-- 注意：只有在确认所有账户都有asset_type_id后再执行此步骤
-- ALTER TABLE `asset_accounts`
-- MODIFY COLUMN `asset_type_id` BIGINT NOT NULL COMMENT '资产大类ID，引用asset_type表';
--
-- ALTER TABLE `asset_accounts`
-- ADD CONSTRAINT `fk_asset_accounts_asset_type`
-- FOREIGN KEY (`asset_type_id`) REFERENCES `asset_type` (`id`)
-- ON DELETE RESTRICT ON UPDATE CASCADE;

-- ================================================================
-- 说明
-- ================================================================
-- 1. asset_type_id 字段提供了直接访问资产大类的途径
-- 2. 不需要通过 category_id -> asset_categories.type -> asset_type.type 的间接查询
-- 3. 查询性能提升，尤其是在需要按大类统计时
-- 4. 前端可以直接使用 asset_type_id 筛选投资类账户
--
-- 示例查询：
-- -- 获取所有投资类账户
-- SELECT aa.*
-- FROM asset_accounts aa
-- INNER JOIN asset_type at ON aa.asset_type_id = at.id
-- WHERE at.is_investment = TRUE;
-- ================================================================
