-- ============================================
-- 年度支出汇总系统 V2 - 抽象配置版本
-- ============================================

-- Step 1: 创建支出类别与资产/负债关联配置表
DROP TABLE IF EXISTS expense_category_adjustment_config;

CREATE TABLE expense_category_adjustment_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_category_id BIGINT NOT NULL COMMENT '支出大类ID',

    -- 关联类型: 'ASSET' 或 'LIABILITY'
    adjustment_type ENUM('ASSET', 'LIABILITY') NOT NULL COMMENT '调整类型',

    -- 资产类型code (如果adjustment_type='ASSET')
    asset_type_code VARCHAR(50) COMMENT '资产类型代码 (INSURANCE, REAL_ESTATE等)',

    -- 负债类型type (如果adjustment_type='LIABILITY')
    liability_type VARCHAR(50) COMMENT '负债类型 (MORTGAGE, AUTO_LOAN等)',

    -- 调整方向: 'ADD' 或 'SUBTRACT'
    -- ADD: 实际支出 = 基础支出 + 调整值 (例如: 房贷本金偿还应计入住房支出)
    -- SUBTRACT: 实际支出 = 基础支出 - 调整值 (例如: 保险资产增值应扣除)
    adjustment_direction ENUM('ADD', 'SUBTRACT') NOT NULL COMMENT '调整方向',

    description TEXT COMMENT '调整说明',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_category_type (major_category_id, adjustment_type, asset_type_code, liability_type),
    KEY idx_major_category (major_category_id),
    KEY idx_asset_type (asset_type_code),
    KEY idx_liability_type (liability_type)
) COMMENT='支出类别调整配置表';

-- Step 2: 插入默认配置
-- 配置1: 住房支出 + 房贷减少
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, liability_type, adjustment_direction, description)
SELECT
    id,
    'LIABILITY',
    'MORTGAGE',
    'ADD',
    '房贷本金偿还计入住房支出: 实际支出 = 月度支出 + 房贷减少额'
FROM expense_categories_major
WHERE code = 'HOUSING';

-- 配置2: 保险支出 - 保险资产增加
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, asset_type_code, adjustment_direction, description)
SELECT
    id,
    'ASSET',
    'INSURANCE',
    'SUBTRACT',
    '保险资产增值扣除: 实际支出 = 月度保费 - 保险现金价值增加'
FROM expense_categories_major
WHERE code = 'INSURANCE';

-- 可以继续添加其他配置...
-- 例如: 车贷也应该计入行的支出
-- INSERT INTO expense_category_adjustment_config
--     (major_category_id, adjustment_type, liability_type, adjustment_direction, description)
-- SELECT id, 'LIABILITY', 'AUTO_LOAN', 'ADD', '车贷本金偿还计入交通支出'
-- FROM expense_categories_major WHERE code = 'TRANSPORTATION';

-- Step 3: 创建年度支出汇总表 (通用版本)
DROP TABLE IF EXISTS annual_expense_summary;

CREATE TABLE annual_expense_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT COMMENT '用户ID',
    summary_year INT NOT NULL COMMENT '汇总年份',
    major_category_id BIGINT NOT NULL COMMENT '大类ID (0表示总计)',
    minor_category_id BIGINT COMMENT '小类ID (NULL表示大类汇总)',

    -- 基础支出金额
    base_expense_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '基础支出(expense_records汇总)',

    -- 通用调整字段
    asset_adjustment DECIMAL(18,2) DEFAULT 0 COMMENT '资产调整额 (资产增加为正, 减少为负)',
    liability_adjustment DECIMAL(18,2) DEFAULT 0 COMMENT '负债调整额 (负债减少为正, 增加为负)',

    -- 调整明细 (JSON格式存储详细信息)
    adjustment_details JSON COMMENT '调整详情 [{type, code, amount, direction}]',

    -- 实际年度支出
    actual_expense_amount DECIMAL(18,2) NOT NULL COMMENT '实际年度支出',

    currency VARCHAR(10) DEFAULT 'CNY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_summary (family_id, summary_year, major_category_id, minor_category_id),
    KEY idx_family_year (family_id, summary_year),
    KEY idx_major_category (major_category_id)
) COMMENT='年度支出汇总表 (通用可配置版本)';

-- Step 4: 创建通用存储过程
DELIMITER //

DROP PROCEDURE IF EXISTS calculate_annual_expense_summary_v2//

CREATE PROCEDURE calculate_annual_expense_summary_v2(
    IN p_family_id BIGINT,
    IN p_summary_year INT
)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_major_category_id BIGINT;
    DECLARE v_adjustment_type VARCHAR(20);
    DECLARE v_asset_type_code VARCHAR(50);
    DECLARE v_liability_type VARCHAR(50);
    DECLARE v_adjustment_direction VARCHAR(20);
    DECLARE v_adjustment_value DECIMAL(18,2);
    DECLARE v_current_year INT;
    DECLARE v_end_date DATE;

    -- 游标: 遍历所有启用的调整配置
    DECLARE config_cursor CURSOR FOR
        SELECT major_category_id, adjustment_type, asset_type_code,
               liability_type, adjustment_direction
        FROM expense_category_adjustment_config
        WHERE is_active = 1;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 确定截止日期: 历史年份用12-31, 当年用今天
    SET v_current_year = YEAR(CURDATE());
    IF p_summary_year < v_current_year THEN
        -- 历史年份: 使用12月31日
        SET v_end_date = CONCAT(p_summary_year, '-12-31');
    ELSE
        -- 当年或未来年份: 使用当前日期
        SET v_end_date = CURDATE();
    END IF;

    -- 清空当年的汇总数据
    DELETE FROM annual_expense_summary
    WHERE family_id = p_family_id AND summary_year = p_summary_year;

    -- ========================================
    -- Part 1: 计算基础支出 (expense_records汇总)
    -- ========================================

    -- 1.1 小类级别汇总
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        family_id, user_id, expense_year,
        major_category_id, minor_category_id,
        SUM(amount_in_base_currency),
        SUM(amount_in_base_currency),
        'CNY'
    FROM expense_records
    WHERE family_id = p_family_id AND expense_year = p_summary_year
    GROUP BY family_id, user_id, expense_year, major_category_id, minor_category_id;

    -- 1.2 大类级别汇总
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        family_id, MAX(user_id), expense_year,
        major_category_id, NULL,
        SUM(amount_in_base_currency),
        SUM(amount_in_base_currency),
        'CNY'
    FROM expense_records
    WHERE family_id = p_family_id AND expense_year = p_summary_year
    GROUP BY family_id, expense_year, major_category_id;

    -- ========================================
    -- Part 2: 根据配置表进行通用调整
    -- ========================================

    OPEN config_cursor;

    config_loop: LOOP
        FETCH config_cursor INTO
            v_major_category_id, v_adjustment_type,
            v_asset_type_code, v_liability_type, v_adjustment_direction;

        IF done THEN
            LEAVE config_loop;
        END IF;

        SET v_adjustment_value = 0;

        -- 根据调整类型计算调整值
        IF v_adjustment_type = 'ASSET' THEN
            -- 计算资产变化: 当年截止日 - 去年底
            -- 优先使用annual_financial_summary表（性能优化）
            SELECT COALESCE(
                (
                    SELECT SUM(ar.value)
                    FROM asset_accounts aa
                    JOIN asset_types at ON aa.type_id = at.id
                    LEFT JOIN asset_records ar ON aa.id = ar.account_id
                        AND ar.record_date = (
                            SELECT MAX(record_date)
                            FROM asset_records
                            WHERE account_id = aa.id
                              AND record_date <= v_end_date
                        )
                    WHERE at.code = v_asset_type_code
                      AND aa.is_active = 1
                      AND aa.family_id = p_family_id
                )
                -
                COALESCE(
                    (
                        -- 优先从annual_financial_summary获取去年数据
                        SELECT JSON_EXTRACT(asset_breakdown, CONCAT('$."', v_asset_type_code, '"'))
                        FROM annual_financial_summary
                        WHERE family_id = p_family_id
                          AND year = p_summary_year - 1
                        LIMIT 1
                    ),
                    (
                        -- 回退到asset_records（兼容旧数据）
                        SELECT SUM(ar.value)
                        FROM asset_accounts aa
                        JOIN asset_types at ON aa.type_id = at.id
                        LEFT JOIN asset_records ar ON aa.id = ar.account_id
                            AND ar.record_date = (
                                SELECT MAX(record_date)
                                FROM asset_records
                                WHERE account_id = aa.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE at.code = v_asset_type_code
                          AND aa.is_active = 1
                          AND aa.family_id = p_family_id
                    ),
                    0
                ),
                0
            ) INTO v_adjustment_value;

            -- 更新资产调整
            UPDATE annual_expense_summary
            SET
                asset_adjustment = CASE
                    WHEN v_adjustment_direction = 'SUBTRACT' THEN v_adjustment_value
                    ELSE -v_adjustment_value
                END,
                actual_expense_amount = CASE
                    WHEN v_adjustment_direction = 'SUBTRACT'
                    THEN base_expense_amount - v_adjustment_value
                    ELSE base_expense_amount + v_adjustment_value
                END,
                adjustment_details = JSON_ARRAY(
                    JSON_OBJECT(
                        'type', 'ASSET',
                        'code', v_asset_type_code,
                        'amount', v_adjustment_value,
                        'direction', v_adjustment_direction
                    )
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL;

        ELSEIF v_adjustment_type = 'LIABILITY' THEN
            -- 计算负债变化: 去年底 - 当年截止日 (减少为正)
            -- 优先使用annual_financial_summary表（性能优化）
            SELECT COALESCE(
                COALESCE(
                    (
                        -- 优先从annual_financial_summary获取去年数据
                        SELECT JSON_EXTRACT(liability_breakdown, CONCAT('$."', v_liability_type, '"'))
                        FROM annual_financial_summary
                        WHERE family_id = p_family_id
                          AND year = p_summary_year - 1
                        LIMIT 1
                    ),
                    (
                        -- 回退到liability_records（兼容旧数据）
                        SELECT SUM(lr.balance)
                        FROM liability_accounts la
                        JOIN liability_categories lc ON la.category_id = lc.id
                        LEFT JOIN liability_records lr ON la.id = lr.account_id
                            AND lr.record_date = (
                                SELECT MAX(record_date)
                                FROM liability_records
                                WHERE account_id = la.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE lc.type = v_liability_type
                          AND la.is_active = 1
                          AND la.family_id = p_family_id
                    ),
                    0
                )
                -
                (
                    SELECT SUM(lr.balance)
                    FROM liability_accounts la
                    JOIN liability_categories lc ON la.category_id = lc.id
                    LEFT JOIN liability_records lr ON la.id = lr.account_id
                        AND lr.record_date = (
                            SELECT MAX(record_date)
                            FROM liability_records
                            WHERE account_id = la.id
                              AND record_date <= v_end_date
                        )
                    WHERE lc.type = v_liability_type
                      AND la.is_active = 1
                      AND la.family_id = p_family_id
                ),
                0
            ) INTO v_adjustment_value;

            -- 更新负债调整
            UPDATE annual_expense_summary
            SET
                liability_adjustment = CASE
                    WHEN v_adjustment_direction = 'ADD' THEN v_adjustment_value
                    ELSE -v_adjustment_value
                END,
                actual_expense_amount = CASE
                    WHEN v_adjustment_direction = 'ADD'
                    THEN base_expense_amount + v_adjustment_value
                    ELSE base_expense_amount - v_adjustment_value
                END,
                adjustment_details = JSON_ARRAY(
                    JSON_OBJECT(
                        'type', 'LIABILITY',
                        'code', v_liability_type,
                        'amount', v_adjustment_value,
                        'direction', v_adjustment_direction
                    )
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL;
        END IF;

    END LOOP;

    CLOSE config_cursor;

    -- ========================================
    -- Part 3: 计算总计
    -- ========================================
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, asset_adjustment, liability_adjustment,
        actual_expense_amount, currency
    )
    SELECT
        p_family_id, NULL, p_summary_year,
        0, NULL,
        SUM(base_expense_amount),
        SUM(COALESCE(asset_adjustment, 0)),
        SUM(COALESCE(liability_adjustment, 0)),
        SUM(actual_expense_amount),
        'CNY'
    FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND minor_category_id IS NULL;

    -- 返回汇总结果
    SELECT
        aes.id,
        aes.summary_year,
        CASE WHEN aes.major_category_id = 0 THEN '总计' ELSE major.name END AS '大类',
        CASE WHEN aes.minor_category_id IS NULL THEN '小计' ELSE minor.name END AS '小类',
        aes.base_expense_amount AS '基础支出',
        aes.asset_adjustment AS '资产调整',
        aes.liability_adjustment AS '负债调整',
        aes.actual_expense_amount AS '实际支出',
        aes.adjustment_details AS '调整详情'
    FROM annual_expense_summary aes
    LEFT JOIN expense_categories_major major ON aes.major_category_id = major.id
    LEFT JOIN expense_categories_minor minor ON aes.minor_category_id = minor.id
    WHERE aes.family_id = p_family_id
      AND aes.summary_year = p_summary_year
    ORDER BY
        CASE WHEN aes.major_category_id = 0 THEN 999 ELSE aes.major_category_id END,
        CASE WHEN aes.minor_category_id IS NULL THEN 0 ELSE aes.minor_category_id END;

END//

DELIMITER ;

-- ============================================
-- 使用示例
-- ============================================

-- 1. 查看当前配置
-- SELECT
--     major.name AS '支出大类',
--     c.adjustment_type AS '调整类型',
--     COALESCE(c.asset_type_code, c.liability_type) AS '关联代码',
--     c.adjustment_direction AS '调整方向',
--     c.description AS '说明'
-- FROM expense_category_adjustment_config c
-- LEFT JOIN expense_categories_major major ON c.major_category_id = major.id
-- WHERE c.is_active = 1;

-- 2. 添加新配置 (例如: 车贷计入交通支出)
-- INSERT INTO expense_category_adjustment_config
--     (major_category_id, adjustment_type, liability_type, adjustment_direction, description)
-- SELECT
--     id, 'LIABILITY', 'AUTO_LOAN', 'ADD',
--     '车贷本金偿还计入交通支出: 实际支出 = 月度交通费 + 车贷减少额'
-- FROM expense_categories_major WHERE code = 'TRANSPORTATION';

-- 3. 禁用某个配置
-- UPDATE expense_category_adjustment_config
-- SET is_active = 0
-- WHERE major_category_id = (SELECT id FROM expense_categories_major WHERE code = 'INSURANCE');

-- 4. 调用存储过程
-- CALL calculate_annual_expense_summary_v2(1, 2024);
