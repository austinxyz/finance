-- ============================================================
-- 添加特殊支出字段到 annual_expense_summary 表
-- ============================================================
-- 生成时间: 2025-12-17
-- 功能说明: 支持记录单笔>=10000 USD的大额支出
-- ============================================================

USE finance;

-- 添加特殊支出金额字段
ALTER TABLE annual_expense_summary
ADD COLUMN special_expense_amount DECIMAL(18,2) DEFAULT 0.00 AFTER base_expense_amount;

-- 添加特殊支出详情字段（JSON格式，存储小类信息）
ALTER TABLE annual_expense_summary
ADD COLUMN special_expense_details JSON NULL AFTER special_expense_amount;

-- 更新现有记录，确保新字段有默认值
UPDATE annual_expense_summary
SET special_expense_amount = 0.00
WHERE special_expense_amount IS NULL;
