-- 添加房产相关字段到annual_financial_summary表

ALTER TABLE annual_financial_summary
ADD COLUMN real_estate_net_worth DECIMAL(18,2) COMMENT '房产净资产（房产资产 - 房贷）',
ADD COLUMN non_real_estate_net_worth DECIMAL(18,2) COMMENT '非房产净资产',
ADD COLUMN yoy_real_estate_net_worth_change DECIMAL(18,2) COMMENT '房产净资产同比变化金额',
ADD COLUMN yoy_real_estate_net_worth_change_pct DECIMAL(5,2) COMMENT '房产净资产同比变化百分比',
ADD COLUMN yoy_non_real_estate_net_worth_change DECIMAL(18,2) COMMENT '非房产净资产同比变化金额',
ADD COLUMN yoy_non_real_estate_net_worth_change_pct DECIMAL(5,2) COMMENT '非房产净资产同比变化百分比',
ADD COLUMN real_estate_asset_ratio DECIMAL(5,2) COMMENT '房产资产占总资产比例',
ADD COLUMN real_estate_net_worth_ratio DECIMAL(5,2) COMMENT '房产净资产占总净资产比例';
