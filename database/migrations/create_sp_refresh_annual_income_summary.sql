-- 创建刷新年度收入汇总的存储过程
-- 用于在"分析-收入分析-年度收入"页面点击刷新按钮时调用

DROP PROCEDURE IF EXISTS sp_refresh_annual_income_summary;

DELIMITER //

CREATE PROCEDURE sp_refresh_annual_income_summary(
    IN p_family_id BIGINT,
    IN p_year INT,
    IN p_currency VARCHAR(10)
)
BEGIN
    DECLARE v_error_msg VARCHAR(500);
    DECLARE exit handler FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1
            v_error_msg = MESSAGE_TEXT;
        -- 回滚事务
        ROLLBACK;
        -- 返回错误信息
        SELECT 'ERROR' AS status, v_error_msg AS message;
    END;

    -- 开始事务
    START TRANSACTION;

    -- 1. 删除该家庭、该年份、该币种的现有数据
    DELETE FROM annual_income_summary
    WHERE family_id = p_family_id
      AND summary_year = p_year
      AND BINARY currency = BINARY p_currency;

    -- 2. 根据币种选择不同的汇总策略
    IF p_currency = 'USD' THEN
        -- USD模式：只汇总USD币种的收入

        -- 2.1 插入小类级别明细（有minor_category_id的记录）
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            i.minor_category_id,
            SUM(i.amount_usd) as base_income_amount,
            SUM(i.amount_usd) as actual_income_amount,
            p_currency as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
          AND i.currency = 'USD'
          AND i.minor_category_id IS NOT NULL
        GROUP BY i.family_id, i.major_category_id, i.minor_category_id;

        -- 2.2 插入大类级别汇总（minor_category_id = NULL）
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            NULL as minor_category_id,
            SUM(i.amount_usd) as base_income_amount,
            SUM(i.amount_usd) as actual_income_amount,
            p_currency as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
          AND i.currency = 'USD'
        GROUP BY i.family_id, i.major_category_id;

    ELSEIF p_currency = 'CNY' THEN
        -- CNY模式：只汇总CNY币种的收入

        -- 2.1 插入小类级别明细
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            i.minor_category_id,
            SUM(i.amount) as base_income_amount,
            SUM(i.amount) as actual_income_amount,
            p_currency as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
          AND i.currency = 'CNY'
          AND i.minor_category_id IS NOT NULL
        GROUP BY i.family_id, i.major_category_id, i.minor_category_id;

        -- 2.2 插入大类级别汇总
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            NULL as minor_category_id,
            SUM(i.amount) as base_income_amount,
            SUM(i.amount) as actual_income_amount,
            p_currency as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
          AND i.currency = 'CNY'
        GROUP BY i.family_id, i.major_category_id;

    ELSE
        -- All模式：汇总所有币种并转换为USD
        -- 需要先获取汇率

        -- 2.1 插入小类级别明细
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            i.minor_category_id,
            SUM(i.amount_usd) as base_income_amount,
            SUM(i.amount_usd) as actual_income_amount,
            'USD' as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
          AND i.minor_category_id IS NOT NULL
        GROUP BY i.family_id, i.major_category_id, i.minor_category_id;

        -- 2.2 插入大类级别汇总
        INSERT INTO annual_income_summary
            (family_id, summary_year, major_category_id, minor_category_id,
             base_income_amount, actual_income_amount, currency)
        SELECT
            i.family_id,
            p_year as summary_year,
            i.major_category_id,
            NULL as minor_category_id,
            SUM(i.amount_usd) as base_income_amount,
            SUM(i.amount_usd) as actual_income_amount,
            'USD' as currency
        FROM income_records i
        WHERE i.family_id = p_family_id
          AND i.period LIKE CONCAT(p_year, '-%')
        GROUP BY i.family_id, i.major_category_id;
    END IF;

    -- 提交事务
    COMMIT;

    -- 返回成功信息和统计数据
    SELECT
        'SUCCESS' AS status,
        '数据刷新成功' AS message,
        p_family_id AS family_id,
        p_year AS year,
        p_currency AS currency,
        COUNT(*) AS total_records,
        SUM(CASE WHEN minor_category_id IS NULL THEN 1 ELSE 0 END) AS major_category_records,
        SUM(CASE WHEN minor_category_id IS NOT NULL THEN 1 ELSE 0 END) AS minor_category_records,
        SUM(CASE WHEN minor_category_id IS NULL THEN actual_income_amount ELSE 0 END) AS total_income
    FROM annual_income_summary
    WHERE family_id = p_family_id
      AND summary_year = p_year
      AND BINARY currency = BINARY CASE WHEN p_currency = 'All' THEN 'USD' ELSE p_currency END;

END //

DELIMITER ;

-- 授予执行权限
-- GRANT EXECUTE ON PROCEDURE sp_refresh_annual_income_summary TO 'your_app_user'@'%';
