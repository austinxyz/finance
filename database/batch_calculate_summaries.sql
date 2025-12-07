-- 批量计算所有家庭所有年份的年度财务摘要
USE finance;

-- 家庭1: 2022年
CALL sp_calculate_annual_summary(1, 2022);

-- 家庭1: 2023年
CALL sp_calculate_annual_summary(1, 2023);

-- 家庭1: 2024年
CALL sp_calculate_annual_summary(1, 2024);

-- 家庭1: 2025年
CALL sp_calculate_annual_summary(1, 2025);

-- 家庭2: 2025年
CALL sp_calculate_annual_summary(2, 2025);

-- 显示计算结果
SELECT
    family_id,
    year,
    summary_date,
    total_assets,
    total_liabilities,
    net_worth,
    currency,
    yoy_asset_change,
    yoy_liability_change,
    yoy_net_worth_change,
    yoy_asset_change_pct,
    yoy_liability_change_pct,
    yoy_net_worth_change_pct
FROM annual_financial_summary
ORDER BY family_id, year DESC;
