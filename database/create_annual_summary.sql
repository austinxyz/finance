-- 年度财务摘要表
-- 用于存储每年的资产、负债、净资产汇总数据，包含各个分类的明细

USE finance;

-- 创建年度财务摘要表（按家庭统计）
CREATE TABLE IF NOT EXISTS annual_financial_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    year INT NOT NULL COMMENT '年份',
    summary_date DATE NOT NULL COMMENT '摘要日期（该年度离12/31最近的记录日期）',

    -- 总计数据
    total_assets DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '总资产',
    total_liabilities DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '总负债',
    net_worth DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '净资产',

    -- 资产分类明细（JSON格式存储各分类数据）
    asset_breakdown JSON COMMENT '资产分类明细 {category_name: amount}',

    -- 负债分类明细（JSON格式存储各分类数据）
    liability_breakdown JSON COMMENT '负债分类明细 {category_name: amount}',

    -- 货币单位
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '货币单位',

    -- 同比数据
    yoy_asset_change DECIMAL(18, 2) COMMENT '资产同比变化',
    yoy_liability_change DECIMAL(18, 2) COMMENT '负债同比变化',
    yoy_net_worth_change DECIMAL(18, 2) COMMENT '净资产同比变化',
    yoy_asset_change_pct DECIMAL(5, 2) COMMENT '资产同比变化百分比',
    yoy_liability_change_pct DECIMAL(5, 2) COMMENT '负债同比变化百分比',
    yoy_net_worth_change_pct DECIMAL(5, 2) COMMENT '净资产同比变化百分比',

    -- 备注和元数据
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键和索引
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    UNIQUE KEY uk_family_year (family_id, year),
    INDEX idx_family_year (family_id, year DESC),
    INDEX idx_summary_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年度财务摘要表';

-- 创建用于计算年度摘要的存储过程
DELIMITER //

CREATE PROCEDURE sp_calculate_annual_summary(
    IN p_family_id BIGINT,
    IN p_year INT
)
BEGIN
    DECLARE v_summary_date DATE;
    DECLARE v_total_assets DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_total_liabilities DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_net_worth DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_asset_breakdown JSON;
    DECLARE v_liability_breakdown JSON;
    DECLARE v_currency VARCHAR(10);
    DECLARE v_year_end_date DATE;

    -- 计算该年度的12月31日
    SET v_year_end_date = DATE(CONCAT(p_year, '-12-31'));

    -- 获取家庭的基准货币（从家庭的第一个用户获取）
    SELECT up.base_currency INTO v_currency
    FROM user_preferences up
    INNER JOIN users u ON up.user_id = u.id
    WHERE u.family_id = p_family_id
    LIMIT 1;

    IF v_currency IS NULL THEN
        SET v_currency = 'USD';
    END IF;

    -- 查找距离12月31日最近的记录日期（家庭中所有用户的记录）
    SELECT MAX(record_date) INTO v_summary_date
    FROM (
        SELECT ar.record_date
        FROM asset_records ar
        INNER JOIN users u ON ar.user_id = u.id
        WHERE u.family_id = p_family_id AND YEAR(ar.record_date) = p_year
        UNION
        SELECT lr.record_date
        FROM liability_records lr
        INNER JOIN users u ON lr.user_id = u.id
        WHERE u.family_id = p_family_id AND YEAR(lr.record_date) = p_year
    ) AS all_dates
    WHERE record_date <= v_year_end_date;

    -- 如果没有找到记录日期，使用12月31日
    IF v_summary_date IS NULL THEN
        SET v_summary_date = v_year_end_date;
    END IF;

    -- 计算资产分类汇总（按家庭）
    SELECT
        COALESCE(SUM(ar.amount_in_base_currency), 0),
        JSON_OBJECTAGG(ac.name, category_total)
    INTO v_total_assets, v_asset_breakdown
    FROM (
        SELECT
            ar.account_id,
            ac.id AS category_id,
            ac.name,
            ar.amount_in_base_currency
        FROM asset_records ar
        INNER JOIN users u ON ar.user_id = u.id
        INNER JOIN asset_accounts aa ON ar.account_id = aa.id
        INNER JOIN asset_categories ac ON aa.category_id = ac.id
        WHERE u.family_id = p_family_id
            AND ar.record_date = (
                SELECT MAX(record_date)
                FROM asset_records
                WHERE account_id = ar.account_id
                    AND record_date <= v_summary_date
            )
            AND aa.is_active = TRUE
    ) AS asset_data
    INNER JOIN asset_categories ac ON asset_data.category_id = ac.id
    LEFT JOIN (
        SELECT
            category_id,
            SUM(amount_in_base_currency) AS category_total
        FROM (
            SELECT
                aa.category_id,
                ar.amount_in_base_currency
            FROM asset_records ar
            INNER JOIN users u ON ar.user_id = u.id
            INNER JOIN asset_accounts aa ON ar.account_id = aa.id
            WHERE u.family_id = p_family_id
                AND ar.record_date = (
                    SELECT MAX(record_date)
                    FROM asset_records
                    WHERE account_id = ar.account_id
                        AND record_date <= v_summary_date
                )
                AND aa.is_active = TRUE
        ) AS temp
        GROUP BY category_id
    ) AS category_sums ON ac.id = category_sums.category_id;

    -- 计算负债分类汇总（按家庭）
    SELECT
        COALESCE(SUM(lr.balance_in_base_currency), 0),
        JSON_OBJECTAGG(lc.name, category_total)
    INTO v_total_liabilities, v_liability_breakdown
    FROM (
        SELECT
            lr.account_id,
            lc.id AS category_id,
            lc.name,
            lr.balance_in_base_currency
        FROM liability_records lr
        INNER JOIN users u ON lr.user_id = u.id
        INNER JOIN liability_accounts la ON lr.account_id = la.id
        INNER JOIN liability_categories lc ON la.category_id = lc.id
        WHERE u.family_id = p_family_id
            AND lr.record_date = (
                SELECT MAX(record_date)
                FROM liability_records
                WHERE account_id = lr.account_id
                    AND record_date <= v_summary_date
            )
            AND la.is_active = TRUE
    ) AS liability_data
    INNER JOIN liability_categories lc ON liability_data.category_id = lc.id
    LEFT JOIN (
        SELECT
            category_id,
            SUM(balance_in_base_currency) AS category_total
        FROM (
            SELECT
                la.category_id,
                lr.balance_in_base_currency
            FROM liability_records lr
            INNER JOIN users u ON lr.user_id = u.id
            INNER JOIN liability_accounts la ON lr.account_id = la.id
            WHERE u.family_id = p_family_id
                AND lr.record_date = (
                    SELECT MAX(record_date)
                    FROM liability_records
                    WHERE account_id = lr.account_id
                        AND record_date <= v_summary_date
                )
                AND la.is_active = TRUE
        ) AS temp
        GROUP BY category_id
    ) AS category_sums ON lc.id = category_sums.category_id;

    -- 计算净资产
    SET v_net_worth = v_total_assets - v_total_liabilities;

    -- 插入或更新年度摘要（按家庭）
    INSERT INTO annual_financial_summary (
        family_id, year, summary_date,
        total_assets, total_liabilities, net_worth,
        asset_breakdown, liability_breakdown,
        currency
    ) VALUES (
        p_family_id, p_year, v_summary_date,
        v_total_assets, v_total_liabilities, v_net_worth,
        v_asset_breakdown, v_liability_breakdown,
        v_currency
    )
    ON DUPLICATE KEY UPDATE
        summary_date = v_summary_date,
        total_assets = v_total_assets,
        total_liabilities = v_total_liabilities,
        net_worth = v_net_worth,
        asset_breakdown = v_asset_breakdown,
        liability_breakdown = v_liability_breakdown,
        currency = v_currency,
        updated_at = CURRENT_TIMESTAMP;

    -- 计算同比数据（按家庭）
    UPDATE annual_financial_summary AS current
    LEFT JOIN annual_financial_summary AS previous
        ON current.family_id = previous.family_id
        AND previous.year = current.year - 1
    SET
        current.yoy_asset_change = current.total_assets - COALESCE(previous.total_assets, 0),
        current.yoy_liability_change = current.total_liabilities - COALESCE(previous.total_liabilities, 0),
        current.yoy_net_worth_change = current.net_worth - COALESCE(previous.net_worth, 0),
        current.yoy_asset_change_pct = IF(previous.total_assets > 0,
            ((current.total_assets - previous.total_assets) / previous.total_assets) * 100,
            NULL),
        current.yoy_liability_change_pct = IF(previous.total_liabilities > 0,
            ((current.total_liabilities - previous.total_liabilities) / previous.total_liabilities) * 100,
            NULL),
        current.yoy_net_worth_change_pct = IF(previous.net_worth > 0,
            ((current.net_worth - previous.net_worth) / previous.net_worth) * 100,
            NULL)
    WHERE current.family_id = p_family_id AND current.year = p_year;

END //

DELIMITER ;

-- 创建触发器：当资产或负债记录更新时，自动刷新相关年度的摘要（按家庭）
DELIMITER //

CREATE TRIGGER trg_after_asset_record_insert
AFTER INSERT ON asset_records
FOR EACH ROW
BEGIN
    DECLARE v_family_id BIGINT;
    SELECT family_id INTO v_family_id FROM users WHERE id = NEW.user_id;
    IF v_family_id IS NOT NULL THEN
        CALL sp_calculate_annual_summary(v_family_id, YEAR(NEW.record_date));
    END IF;
END //

CREATE TRIGGER trg_after_asset_record_update
AFTER UPDATE ON asset_records
FOR EACH ROW
BEGIN
    DECLARE v_family_id BIGINT;
    SELECT family_id INTO v_family_id FROM users WHERE id = NEW.user_id;
    IF v_family_id IS NOT NULL THEN
        CALL sp_calculate_annual_summary(v_family_id, YEAR(NEW.record_date));
        IF YEAR(OLD.record_date) != YEAR(NEW.record_date) THEN
            CALL sp_calculate_annual_summary(v_family_id, YEAR(OLD.record_date));
        END IF;
    END IF;
END //

CREATE TRIGGER trg_after_liability_record_insert
AFTER INSERT ON liability_records
FOR EACH ROW
BEGIN
    DECLARE v_family_id BIGINT;
    SELECT family_id INTO v_family_id FROM users WHERE id = NEW.user_id;
    IF v_family_id IS NOT NULL THEN
        CALL sp_calculate_annual_summary(v_family_id, YEAR(NEW.record_date));
    END IF;
END //

CREATE TRIGGER trg_after_liability_record_update
AFTER UPDATE ON liability_records
FOR EACH ROW
BEGIN
    DECLARE v_family_id BIGINT;
    SELECT family_id INTO v_family_id FROM users WHERE id = NEW.user_id;
    IF v_family_id IS NOT NULL THEN
        CALL sp_calculate_annual_summary(v_family_id, YEAR(NEW.record_date));
        IF YEAR(OLD.record_date) != YEAR(NEW.record_date) THEN
            CALL sp_calculate_annual_summary(v_family_id, YEAR(OLD.record_date));
        END IF;
    END IF;
END //

DELIMITER ;

-- 创建视图：年度财务趋势视图（按家庭）
CREATE OR REPLACE VIEW v_annual_financial_trend AS
SELECT
    family_id,
    year,
    summary_date,
    total_assets,
    total_liabilities,
    net_worth,
    asset_breakdown,
    liability_breakdown,
    currency,
    yoy_asset_change,
    yoy_liability_change,
    yoy_net_worth_change,
    yoy_asset_change_pct,
    yoy_liability_change_pct,
    yoy_net_worth_change_pct,
    -- 计算环比变化
    total_assets - LAG(total_assets) OVER (PARTITION BY family_id ORDER BY year) AS mom_asset_change,
    total_liabilities - LAG(total_liabilities) OVER (PARTITION BY family_id ORDER BY year) AS mom_liability_change,
    net_worth - LAG(net_worth) OVER (PARTITION BY family_id ORDER BY year) AS mom_net_worth_change
FROM annual_financial_summary
ORDER BY family_id, year DESC;
