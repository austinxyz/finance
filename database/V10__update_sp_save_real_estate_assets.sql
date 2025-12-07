-- 更新存储过程以保存房产资产总额

USE finance;

DROP PROCEDURE IF EXISTS sp_calculate_annual_summary;

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
    DECLARE v_real_estate_assets DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_mortgage_liabilities DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_real_estate_net_worth DECIMAL(18, 2) DEFAULT 0;
    DECLARE v_non_real_estate_net_worth DECIMAL(18, 2) DEFAULT 0;

    -- 计算该年度的12月31日
    SET v_year_end_date = DATE(CONCAT(p_year, '-12-31'));

    -- 获取家庭的基准货币
    SELECT up.base_currency INTO v_currency
    FROM user_preferences up
    INNER JOIN users u ON up.user_id = u.id
    WHERE u.family_id = p_family_id
    LIMIT 1;

    IF v_currency IS NULL THEN
        SET v_currency = 'USD';
    END IF;

    -- 查找距离12月31日最近的记录日期
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

    IF v_summary_date IS NULL THEN
        SET v_summary_date = v_year_end_date;
    END IF;

    -- 计算资产分类汇总
    SELECT
        SUM(COALESCE(total, 0)),
        JSON_OBJECTAGG(COALESCE(name, 'Unknown'), COALESCE(total, 0))
    INTO v_total_assets, v_asset_breakdown
    FROM (
        SELECT
            ac.name,
            SUM(temp.amount_in_base_currency) AS total
        FROM asset_categories ac
        LEFT JOIN (
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
        ) AS temp ON ac.id = temp.category_id
        GROUP BY ac.id, ac.name
    ) AS category_sums;

    -- 计算房产资产总额（假设类别名称为"房产"或"Real Estate"）
    SELECT COALESCE(SUM(ar.amount_in_base_currency), 0) INTO v_real_estate_assets
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
        AND (ac.name LIKE '%房产%' OR ac.name LIKE '%Real Estate%' OR ac.name LIKE '%Property%');

    -- 计算负债分类汇总
    SELECT
        SUM(COALESCE(total, 0)),
        JSON_OBJECTAGG(COALESCE(name, 'Unknown'), COALESCE(total, 0))
    INTO v_total_liabilities, v_liability_breakdown
    FROM (
        SELECT
            lc.name,
            SUM(temp.balance_in_base_currency) AS total
        FROM liability_categories lc
        LEFT JOIN (
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
        ) AS temp ON lc.id = temp.category_id
        GROUP BY lc.id, lc.name
    ) AS category_sums;

    -- 计算房贷负债总额（假设类别名称为"房贷"或"Mortgage"）
    SELECT COALESCE(SUM(lr.balance_in_base_currency), 0) INTO v_mortgage_liabilities
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
        AND (lc.name LIKE '%房贷%' OR lc.name LIKE '%Mortgage%');

    -- 计算净资产和房产相关指标
    SET v_net_worth = v_total_assets - v_total_liabilities;
    SET v_real_estate_net_worth = v_real_estate_assets - v_mortgage_liabilities;
    SET v_non_real_estate_net_worth = v_net_worth - v_real_estate_net_worth;

    -- 插入或更新年度摘要
    INSERT INTO annual_financial_summary (
        family_id, year, summary_date,
        total_assets, total_liabilities, net_worth,
        asset_breakdown, liability_breakdown,
        currency,
        real_estate_assets,
        real_estate_net_worth,
        non_real_estate_net_worth,
        real_estate_asset_ratio,
        real_estate_net_worth_ratio,
        real_estate_to_net_worth_ratio
    ) VALUES (
        p_family_id, p_year, v_summary_date,
        v_total_assets, v_total_liabilities, v_net_worth,
        v_asset_breakdown, v_liability_breakdown,
        v_currency,
        v_real_estate_assets,
        v_real_estate_net_worth,
        v_non_real_estate_net_worth,
        IF(v_total_assets > 0, (v_real_estate_assets / v_total_assets) * 100, NULL),
        IF(v_net_worth > 0, (v_real_estate_net_worth / v_net_worth) * 100, NULL),
        IF(v_net_worth > 0, (v_real_estate_assets / v_net_worth) * 100, NULL)
    )
    ON DUPLICATE KEY UPDATE
        summary_date = v_summary_date,
        total_assets = v_total_assets,
        total_liabilities = v_total_liabilities,
        net_worth = v_net_worth,
        asset_breakdown = v_asset_breakdown,
        liability_breakdown = v_liability_breakdown,
        currency = v_currency,
        real_estate_assets = v_real_estate_assets,
        real_estate_net_worth = v_real_estate_net_worth,
        non_real_estate_net_worth = v_non_real_estate_net_worth,
        real_estate_asset_ratio = IF(v_total_assets > 0, (v_real_estate_assets / v_total_assets) * 100, NULL),
        real_estate_net_worth_ratio = IF(v_net_worth > 0, (v_real_estate_net_worth / v_net_worth) * 100, NULL),
        real_estate_to_net_worth_ratio = IF(v_net_worth > 0, (v_real_estate_assets / v_net_worth) * 100, NULL),
        updated_at = CURRENT_TIMESTAMP;

    -- 计算同比数据（包括房产相关同比）
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
            NULL),
        -- 房产净资产同比
        current.yoy_real_estate_net_worth_change = current.real_estate_net_worth - COALESCE(previous.real_estate_net_worth, 0),
        current.yoy_real_estate_net_worth_change_pct = IF(previous.real_estate_net_worth > 0,
            ((current.real_estate_net_worth - previous.real_estate_net_worth) / previous.real_estate_net_worth) * 100,
            NULL),
        -- 非房产净资产同比
        current.yoy_non_real_estate_net_worth_change = current.non_real_estate_net_worth - COALESCE(previous.non_real_estate_net_worth, 0),
        current.yoy_non_real_estate_net_worth_change_pct = IF(previous.non_real_estate_net_worth > 0,
            ((current.non_real_estate_net_worth - previous.non_real_estate_net_worth) / previous.non_real_estate_net_worth) * 100,
            NULL)
    WHERE current.family_id = p_family_id AND current.year = p_year;

END //

DELIMITER ;
