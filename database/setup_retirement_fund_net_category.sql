-- 检查净资产分类表的现有数据
SELECT * FROM net_asset_categories;

-- 检查资产类型映射
SELECT * FROM net_asset_category_asset_type_mappings;

-- 检查负债类型映射
SELECT * FROM net_asset_category_liability_type_mappings;

-- 插入退休基金净值分类（如果不存在）
INSERT INTO net_asset_categories (name, code, description, display_order, color, created_at, updated_at)
SELECT '退休基金净值', 'RETIREMENT_FUND_NET', '退休基金资产净值（不可即时取出）', 1, '#8b5cf6', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM net_asset_categories WHERE code = 'RETIREMENT_FUND_NET'
);

-- 获取刚插入的退休基金净值分类ID
SET @retirement_fund_net_category_id = (SELECT id FROM net_asset_categories WHERE code = 'RETIREMENT_FUND_NET');

-- 映射RETIREMENT_FUND资产类型到退休基金净值分类
INSERT INTO net_asset_category_asset_type_mappings (net_asset_category_id, asset_type, created_at)
SELECT @retirement_fund_net_category_id, 'RETIREMENT_FUND', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM net_asset_category_asset_type_mappings
    WHERE net_asset_category_id = @retirement_fund_net_category_id
    AND asset_type = 'RETIREMENT_FUND'
);

-- 退休基金净值没有对应的负债类型，所以不需要插入负债映射

-- 验证插入结果
SELECT
    nac.id,
    nac.name,
    nac.code,
    nac.display_order,
    nac.color,
    GROUP_CONCAT(DISTINCT natm.asset_type) as asset_types,
    GROUP_CONCAT(DISTINCT nltm.liability_type) as liability_types
FROM net_asset_categories nac
LEFT JOIN net_asset_category_asset_type_mappings natm ON nac.id = natm.net_asset_category_id
LEFT JOIN net_asset_category_liability_type_mappings nltm ON nac.id = nltm.net_asset_category_id
WHERE nac.code = 'RETIREMENT_FUND_NET'
GROUP BY nac.id, nac.name, nac.code, nac.display_order, nac.color;
