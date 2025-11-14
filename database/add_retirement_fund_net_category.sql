-- ============================================================================
-- 添加退休基金净值分类
-- 将退休基金从投资净值中分离出来，作为独立的净资产分类
-- ============================================================================

-- 1. 插入退休基金净值分类
INSERT INTO net_asset_categories (name, code, description, display_order, color, created_at, updated_at)
VALUES ('退休基金净值', 'RETIREMENT_FUND_NET', '退休基金资产净值（不可即时取出的长期投资）', 2, '#8b5cf6', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    display_order = VALUES(display_order),
    color = VALUES(color),
    updated_at = NOW();

-- 2. 获取退休基金净值分类ID
SET @retirement_fund_net_id = (SELECT id FROM net_asset_categories WHERE code = 'RETIREMENT_FUND_NET');

-- 3. 将RETIREMENT_FUND资产类型映射到退休基金净值分类
INSERT INTO net_asset_category_asset_type_mappings (net_asset_category_id, asset_type, created_at)
VALUES (@retirement_fund_net_id, 'RETIREMENT_FUND', NOW())
ON DUPLICATE KEY UPDATE
    net_asset_category_id = VALUES(net_asset_category_id);

-- 4. 从投资净值分类中移除RETIREMENT_FUND映射（如果存在）
DELETE FROM net_asset_category_asset_type_mappings
WHERE asset_type = 'RETIREMENT_FUND'
AND net_asset_category_id != @retirement_fund_net_id;

-- 5. 更新其他分类的display_order，为退休基金净值腾出位置
UPDATE net_asset_categories
SET display_order = display_order + 1, updated_at = NOW()
WHERE display_order >= 2 AND code != 'RETIREMENT_FUND_NET';

-- 6. 验证结果 - 查看所有净资产分类及其映射
SELECT
    nac.id,
    nac.name,
    nac.code,
    nac.display_order,
    nac.color,
    GROUP_CONCAT(DISTINCT natm.asset_type ORDER BY natm.asset_type SEPARATOR ', ') as asset_types,
    GROUP_CONCAT(DISTINCT nltm.liability_type ORDER BY nltm.liability_type SEPARATOR ', ') as liability_types
FROM net_asset_categories nac
LEFT JOIN net_asset_category_asset_type_mappings natm ON nac.id = natm.net_asset_category_id
LEFT JOIN net_asset_category_liability_type_mappings nltm ON nac.id = nltm.net_asset_category_id
GROUP BY nac.id, nac.name, nac.code, nac.display_order, nac.color
ORDER BY nac.display_order;
