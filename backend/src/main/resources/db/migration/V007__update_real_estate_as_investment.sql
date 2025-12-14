-- V007: 将房地产标记为投资类资产
-- 说明：房地产应该显示在年度投资分析中

-- 更新房地产资产类型，将其标记为投资类
UPDATE asset_type
SET is_investment = TRUE,
    updated_at = NOW()
WHERE type = 'REAL_ESTATE';
