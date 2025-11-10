-- 迁移脚本：将基准货币从CNY改为USD
-- 注意：这个脚本会更新所有现有的资产记录

USE finance;

-- 1. 首先添加USD作为基准货币的汇率数据
-- 删除旧的CNY基准货币汇率
DELETE FROM exchange_rates WHERE to_currency = 'CNY';

-- 添加USD作为基准货币的汇率（1 USD = 1 USD）
INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) VALUES
('USD', 'USD', 1.000000, CURDATE(), 'system'),
('CNY', 'USD', 0.138889, CURDATE(), 'system'),  -- 1 CNY = 0.138889 USD (约1/7.2)
('EUR', 'USD', 1.080000, CURDATE(), 'system'),
('JPY', 'USD', 0.006667, CURDATE(), 'system'),  -- 1 JPY = 0.00667 USD (约1/150)
('GBP', 'USD', 1.260000, CURDATE(), 'system'),
('HKD', 'USD', 0.127778, CURDATE(), 'system'),  -- 1 HKD = 0.127778 USD (约1/7.82)
('AUD', 'USD', 0.660000, CURDATE(), 'system'),
('CAD', 'USD', 0.735000, CURDATE(), 'system')
ON DUPLICATE KEY UPDATE rate = VALUES(rate), source = VALUES(source);

-- 2. 更新所有现有的资产记录，将amount_in_base_currency从CNY转换为USD
-- 假设当前汇率为 1 CNY = 0.138889 USD
UPDATE asset_records
SET amount_in_base_currency = amount_in_base_currency * 0.138889
WHERE amount_in_base_currency IS NOT NULL;

-- 3. 更新exchange_rate字段，如果原来的记录是CNY，需要调整汇率
-- 原来: CNY * rate = CNY (基准货币)
-- 现在: CNY * new_rate = USD (基准货币)
UPDATE asset_records
SET exchange_rate = exchange_rate * 0.138889
WHERE currency = 'CNY';

-- 如果是USD记录，exchange_rate应该是1
UPDATE asset_records
SET exchange_rate = 1.0
WHERE currency = 'USD';

-- 4. 更新负债记录（如果表存在的话）
-- 由于liability_records表可能还未创建，暂时跳过

-- 5. 显示更新后的汇率数据
SELECT * FROM exchange_rates ORDER BY from_currency, to_currency, rate_date DESC;

-- 6. 显示更新后的资产记录示例（最近10条）
SELECT
    id,
    account_id,
    record_date,
    amount,
    currency,
    exchange_rate,
    amount_in_base_currency
FROM asset_records
ORDER BY record_date DESC, id DESC
LIMIT 10;
