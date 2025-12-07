-- 添加房产资产总值占净资产比例字段
ALTER TABLE annual_financial_summary
ADD COLUMN real_estate_to_net_worth_ratio DECIMAL(5,2) COMMENT '房产资产总值占净资产比例';
