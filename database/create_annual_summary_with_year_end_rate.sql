-- 删除旧的存储过程
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

    -- 如果没有找到记录日期，使用12月31日
    IF v_summary_date IS NULL THEN
        SET v_summary_date = v_year_end_date;
    END IF;

    -- 计算总资产和资产分类汇总（使用年度最后一天的汇率）
    SELECT
        COALESCE(SUM(category_total), 0) AS total,
        COALESCE(
            JSON_OBJECTAGG(category_name, category_total),
            JSON_OBJECT()
        ) AS breakdown
    INTO v_total_assets, v_asset_breakdown
    FROM (
        SELECT
            ac.name AS category_name,
            SUM(
                -- 使用年度最后一天的汇率进行转换
                CASE
                    WHEN ar.currency = v_currency THEN ar.amount
                    WHEN ar.currency = 'USD' AND v_currency != 'USD' THEN
                        ar.amount / COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = v_currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                    WHEN ar.currency != 'USD' AND v_currency = 'USD' THEN
                        ar.amount * COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = ar.currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                    ELSE
                        -- 非USD货币之间的转换：先转到USD，再转到基准货币
                        ar.amount * COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = ar.currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        ) / COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = v_currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                END
            ) AS category_total
        FROM asset_records ar
        INNER JOIN users u ON ar.user_id = u.id
        INNER JOIN asset_accounts aa ON ar.account_id = aa.id
        INNER JOIN asset_categories ac ON aa.category_id = ac.id
        WHERE u.family_id = p_family_id
            AND ar.record_date = (
                SELECT MAX(ar2.record_date)
                FROM asset_records ar2
                WHERE ar2.account_id = ar.account_id
                    AND ar2.record_date <= v_summary_date
            )
            AND aa.is_active = TRUE
        GROUP BY ac.id, ac.name
    ) AS asset_summary;

    -- 计算房产资产总额（使用年度最后一天的汇率）
    SELECT
        COALESCE(SUM(
            CASE
                WHEN ar.currency = v_currency THEN ar.amount
                WHEN ar.currency = 'USD' AND v_currency != 'USD' THEN
                    ar.amount / COALESCE(
                        (SELECT rate_to_usd FROM exchange_rates
                         WHERE currency = v_currency
                         AND effective_date <= v_summary_date
                         AND is_active = TRUE
                         ORDER BY effective_date DESC LIMIT 1),
                        1
                    )
                WHEN ar.currency != 'USD' AND v_currency = 'USD' THEN
                    ar.amount * COALESCE(
                        (SELECT rate_to_usd FROM exchange_rates
                         WHERE currency = ar.currency
                         AND effective_date <= v_summary_date
                         AND is_active = TRUE
                         ORDER BY effective_date DESC LIMIT 1),
                        1
                    )
                ELSE
                    ar.amount * COALESCE(
                        (SELECT rate_to_usd FROM exchange_rates
                         WHERE currency = ar.currency
                         AND effective_date <= v_summary_date
                         AND is_active = TRUE
                         ORDER BY effective_date DESC LIMIT 1),
                        1
                    ) / COALESCE(
                        (SELECT rate_to_usd FROM exchange_rates
                         WHERE currency = v_currency
                         AND effective_date <= v_summary_date
                         AND is_active = TRUE
                         ORDER BY effective_date DESC LIMIT 1),
                        1
                    )
            END
        ), 0) INTO v_real_estate_assets
    FROM asset_records ar
    INNER JOIN users u ON ar.user_id = u.id
    INNER JOIN asset_accounts aa ON ar.account_id = aa.id
    INNER JOIN asset_categories ac ON aa.category_id = ac.id
    WHERE u.family_id = p_family_id
        AND ac.name = '房产'
        AND ar.record_date = (
            SELECT MAX(ar2.record_date)
            FROM asset_records ar2
            WHERE ar2.account_id = ar.account_id
                AND ar2.record_date <= v_summary_date
        )
        AND aa.is_active = TRUE;

    -- 计算总负债和负债分类汇总（使用年度最后一天的汇率）
    SELECT
        COALESCE(SUM(category_total), 0) AS total,
        COALESCE(
            JSON_OBJECTAGG(category_name, category_total),
            JSON_OBJECT()
        ) AS breakdown
    INTO v_total_liabilities, v_liability_breakdown
    FROM (
        SELECT
            lc.name AS category_name,
            SUM(
                CASE
                    WHEN lr.currency = v_currency THEN lr.outstanding_balance
                    WHEN lr.currency = 'USD' AND v_currency != 'USD' THEN
                        lr.outstanding_balance / COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = v_currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                    WHEN lr.currency != 'USD' AND v_currency = 'USD' THEN
                        lr.outstanding_balance * COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = lr.currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                    ELSE
                        lr.outstanding_balance * COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = lr.currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        ) / COALESCE(
                            (SELECT rate_to_usd FROM exchange_rates
                             WHERE currency = v_currency
                             AND effective_date <= v_summary_date
                             AND is_active = TRUE
                             ORDER BY effective_date DESC LIMIT 1),
                            1
                        )
                END
            ) AS category_total
        FROM liability_records lr
        INNER JOIN users u ON lr.user_id = u.id
        INNER JOIN liability_accounts la ON lr.account_id = la.id
        INNER JOIN liability_categories lc ON la.category_id = lc.id
        WHERE u.family_id = p_family_id
            AND lr.record_date = (
                SELECT MAX(lr2.record_date)
                FROM liability_records lr2
                WHERE lr2.account_id = lr.account_id
                    AND lr2.record_date <= v_summary_date
            )
            AND la.is_active = TRUE
        GROUP BY lc.id, lc.name
    ) AS liability_summary;

    -- 计算净资产
    SET v_net_worth = v_total_assets - v_total_liabilities;

    -- 计算房产净资产和非房产净资产
    SET v_real_estate_net_worth = v_real_estate_assets;
    SET v_non_real_estate_net_worth = v_net_worth - v_real_estate_net_worth;

    -- 插入或更新年度摘要
    INSERT INTO annual_financial_summary (
        family_id, year, summary_date,
        total_assets, total_liabilities, net_worth,
        asset_breakdown, liability_breakdown,
        currency,
        real_estate_assets, real_estate_net_worth, non_real_estate_net_worth
    ) VALUES (
        p_family_id, p_year, v_summary_date,
        v_total_assets, v_total_liabilities, v_net_worth,
        v_asset_breakdown, v_liability_breakdown,
        v_currency,
        v_real_estate_assets, v_real_estate_net_worth, v_non_real_estate_net_worth
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
        updated_at = CURRENT_TIMESTAMP;

    -- 计算同比数据
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
        current.yoy_real_estate_net_worth_change = current.real_estate_net_worth - COALESCE(previous.real_estate_net_worth, 0),
        current.yoy_real_estate_net_worth_change_pct = IF(previous.real_estate_net_worth > 0,
            ((current.real_estate_net_worth - previous.real_estate_net_worth) / previous.real_estate_net_worth) * 100,
            NULL),
        current.yoy_non_real_estate_net_worth_change = current.non_real_estate_net_worth - COALESCE(previous.non_real_estate_net_worth, 0),
        current.yoy_non_real_estate_net_worth_change_pct = IF(previous.non_real_estate_net_worth > 0,
            ((current.non_real_estate_net_worth - previous.non_real_estate_net_worth) / previous.non_real_estate_net_worth) * 100,
            NULL),
        current.real_estate_asset_ratio = IF(current.total_assets > 0,
            (current.real_estate_assets / current.total_assets) * 100,
            NULL),
        current.real_estate_net_worth_ratio = IF(current.net_worth > 0,
            (current.real_estate_net_worth / current.net_worth) * 100,
            NULL),
        current.real_estate_to_net_worth_ratio = IF(current.net_worth > 0,
            (current.real_estate_assets / current.net_worth) * 100,
            NULL)
    WHERE current.family_id = p_family_id AND current.year = p_year;

END //

DELIMITER ;
