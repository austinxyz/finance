-- 添加房产资产总额字段
ALTER TABLE annual_financial_summary
ADD COLUMN real_estate_assets DECIMAL(18,2) COMMENT '房产资产总额（不含负债）';
