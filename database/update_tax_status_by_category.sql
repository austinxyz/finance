-- 根据资产类别自动更新资产账号的税务状态
-- 执行此脚本前请确保已经添加了 tax_status 字段

USE finance;

-- 1. 现金类 -> TAX_FREE (免税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAX_FREE'
WHERE ac.type = 'CASH';

-- 2. 股票投资 -> TAXABLE (应税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAXABLE'
WHERE ac.type = 'STOCKS';

-- 3. 退休基金 - 需要根据账号名称判断
-- 3.1 Roth 401k 和 Roth IRA -> TAX_FREE (免税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAX_FREE'
WHERE ac.type = 'RETIREMENT_FUND'
  AND (
    aa.account_name LIKE '%Roth%401%'
    OR aa.account_name LIKE '%Roth%IRA%'
    OR aa.account_name LIKE '%roth%401%'
    OR aa.account_name LIKE '%roth%IRA%'
  );

-- 3.2 非 Roth 退休基金 -> TAX_DEFERRED (延税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAX_DEFERRED'
WHERE ac.type = 'RETIREMENT_FUND'
  AND aa.tax_status != 'TAX_FREE'  -- 排除已经被标记为 TAX_FREE 的 Roth 账户
  AND NOT (
    aa.account_name LIKE '%Roth%401%'
    OR aa.account_name LIKE '%Roth%IRA%'
    OR aa.account_name LIKE '%roth%401%'
    OR aa.account_name LIKE '%roth%IRA%'
  );

-- 4. 保险 -> TAX_FREE (免税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAX_FREE'
WHERE ac.type = 'INSURANCE';

-- 5. 房地产 -> TAXABLE (应税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAXABLE'
WHERE ac.type = 'REAL_ESTATE';

-- 6. 数字货币 -> TAXABLE (应税)
UPDATE asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
SET aa.tax_status = 'TAXABLE'
WHERE ac.type = 'CRYPTOCURRENCY';

-- 验证更新结果
SELECT
    ac.type AS '资产类别',
    aa.tax_status AS '税务状态',
    COUNT(*) AS '账户数量'
FROM asset_accounts aa
JOIN asset_categories ac ON aa.category_id = ac.id
GROUP BY ac.type, aa.tax_status
ORDER BY ac.type, aa.tax_status;
