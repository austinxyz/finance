-- 更新房地产资产类型，将其标记为投资类
-- 这样房地产就会显示在年度投资分析中

UPDATE asset_type
SET is_investment = TRUE,
    updated_at = NOW()
WHERE type = 'REAL_ESTATE';

-- 验证更新结果
SELECT id, type, name, chinese_name, is_investment, display_order
FROM asset_type
WHERE type = 'REAL_ESTATE';
