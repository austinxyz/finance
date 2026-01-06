-- 从 income_records 表中删除 user_id 字段
-- 收入记录不再关联特定用户，仅关联家庭

-- 先删除外键约束
ALTER TABLE income_records
DROP FOREIGN KEY income_records_ibfk_2;

-- 删除索引
ALTER TABLE income_records
DROP INDEX idx_user_period;

-- 删除唯一键约束（需要重建，去掉 user_id）
ALTER TABLE income_records
DROP INDEX uk_income;

-- 删除 user_id 字段
ALTER TABLE income_records
DROP COLUMN user_id;

-- 重新创建唯一键约束（不包含 user_id）
ALTER TABLE income_records
ADD UNIQUE KEY uk_income (family_id, period, major_category_id, minor_category_id, currency);
