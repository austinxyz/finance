-- ============================================================
-- 家庭理财管理系统 - 存储过程（优化版）
-- ============================================================
-- 优化说明：
-- 1. 支出记录汇率优化：使用每笔支出当年年末的汇率进行折算
--    - 原逻辑：所有支出统一使用汇总年份年末汇率
--    - 新逻辑：2023年的支出使用2023年末汇率，2024年的支出使用2024年末汇率
--    - 提高了跨年份支出汇总的准确性
--
-- 2. 资产/负债记录汇率优化：使用记录所在年份年末的汇率
--
-- 生成时间: 2025-12-17
-- ============================================================

SET NAMES utf8mb4;
DELIMITER ;;

DROP PROCEDURE IF EXISTS `calculate_annual_expense_summary_v3`;;

CREATE DEFINER=`austinxu`@`%` PROCEDURE `calculate_annual_expense_summary_v3`(
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

    -- 房产购买相关变量
    DECLARE v_property_record_id BIGINT;
    DECLARE v_asset_account_id BIGINT;
    DECLARE v_purchase_year INT;
    DECLARE v_down_payment DECIMAL(18,2);
    DECLARE v_mortgage_amount DECIMAL(18,2);
    DECLARE v_initial_property_value DECIMAL(18,2);
    DECLARE v_current_property_value DECIMAL(18,2);
    DECLARE v_current_mortgage_balance DECIMAL(18,2);
    DECLARE v_previous_year_mortgage_balance DECIMAL(18,2);
    DECLARE v_total_mortgage_expense DECIMAL(18,2);
    DECLARE v_mortgage_principal_paid DECIMAL(18,2);
    DECLARE v_accumulated_interest DECIMAL(18,2);
    DECLARE v_purchase_cost DECIMAL(18,2);
    DECLARE v_adjusted_expense DECIMAL(18,2);
    DECLARE v_investment_return DECIMAL(18,2);
    DECLARE v_housing_category_id BIGINT;
    DECLARE v_property_currency VARCHAR(10);
    DECLARE v_property_exchange_rate DECIMAL(10,6);

    -- 游标: 遍历所有启用的调整配置
    DECLARE config_cursor CURSOR FOR
        SELECT major_category_id, adjustment_type, asset_type_code,
               liability_type, adjustment_direction
        FROM expense_category_adjustment_config
        WHERE is_active = 1;

    -- 游标: 遍历当年购买的房产
    DECLARE property_cursor CURSOR FOR
        SELECT
            pr.id,
            pr.asset_account_id,
            YEAR(pr.purchase_date),
            pr.down_payment,
            pr.mortgage_amount,
            pr.property_value,
            pr.currency
        FROM property_records pr
        JOIN asset_accounts aa ON pr.asset_account_id = aa.id
        WHERE aa.user_id IN (
            SELECT id FROM users WHERE family_id = p_family_id
        )
        AND YEAR(pr.purchase_date) = p_summary_year;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 确定截止日期: 历史年份用12-31, 当年用今天
    SET v_current_year = YEAR(CURDATE());
    IF p_summary_year < v_current_year THEN
        SET v_end_date = CONCAT(p_summary_year, '-12-31');
    ELSE
        SET v_end_date = CURDATE();
    END IF;

    -- 获取住房大类ID
    SELECT id INTO v_housing_category_id
    FROM expense_categories_major
    WHERE code = 'HOUSING'
    LIMIT 1;

    -- 清空当年的汇总数据
    DELETE FROM annual_expense_summary
    WHERE family_id = p_family_id AND summary_year = p_summary_year;

    -- ========================================
    -- Part 1: 计算基础支出 (expense_records汇总)
    -- 优化：使用每笔支出当年的年末汇率
    -- 排除：单笔>=10000 USD的特殊支出
    -- ========================================

    -- 1.1 小类级别汇总（转换为USD基准货币，排除特殊支出）
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        er.family_id,
        er.user_id,
        er.expense_year,
        er.major_category_id,
        er.minor_category_id,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = er.currency
                       AND effective_date <= CONCAT(er.expense_year, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END
        ),
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = er.currency
                       AND effective_date <= CONCAT(er.expense_year, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END
        ),
        'USD'
    FROM expense_records er
    WHERE er.family_id = p_family_id
      AND er.expense_year = p_summary_year
      -- 排除特殊支出（单笔>=10000 USD）
      AND (
          CASE
              WHEN er.currency = 'USD' THEN er.amount
              ELSE er.amount * COALESCE(
                  (SELECT rate_to_usd
                   FROM exchange_rates
                   WHERE currency = er.currency
                     AND effective_date <= CONCAT(er.expense_year, '-12-31')
                   ORDER BY effective_date DESC
                   LIMIT 1),
                  1.0
              )
          END
      ) < 10000
    GROUP BY er.family_id, er.user_id, er.expense_year, er.major_category_id, er.minor_category_id;

    -- 1.2 大类级别汇总（转换为USD基准货币，排除特殊支出）
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        er.family_id,
        MAX(er.user_id),
        er.expense_year,
        er.major_category_id,
        NULL,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = er.currency
                       AND effective_date <= CONCAT(er.expense_year, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END
        ),
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = er.currency
                       AND effective_date <= CONCAT(er.expense_year, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END
        ),
        'USD'
    FROM expense_records er
    WHERE er.family_id = p_family_id
      AND er.expense_year = p_summary_year
      -- 排除特殊支出（单笔>=10000 USD）
      AND (
          CASE
              WHEN er.currency = 'USD' THEN er.amount
              ELSE er.amount * COALESCE(
                  (SELECT rate_to_usd
                   FROM exchange_rates
                   WHERE currency = er.currency
                     AND effective_date <= CONCAT(er.expense_year, '-12-31')
                   ORDER BY effective_date DESC
                   LIMIT 1),
                  1.0
              )
          END
      ) < 10000
    GROUP BY er.family_id, er.expense_year, er.major_category_id;

    -- ========================================
    -- Part 1.5: 计算特殊支出（单笔>=10000 USD）
    -- ========================================

    -- 1.5.1 小类级别的特殊支出汇总（用于构建详情JSON）
    -- 创建临时表存储小类级别的特殊支出
    DROP TEMPORARY TABLE IF EXISTS temp_special_expenses_minor;
    CREATE TEMPORARY TABLE temp_special_expenses_minor (
        major_category_id BIGINT,
        minor_category_id BIGINT,
        minor_category_name VARCHAR(100),
        special_amount DECIMAL(18,2)
    );

    INSERT INTO temp_special_expenses_minor (major_category_id, minor_category_id, minor_category_name, special_amount)
    SELECT
        er.major_category_id,
        er.minor_category_id,
        ecm.name,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = er.currency
                       AND effective_date <= CONCAT(er.expense_year, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END
        ) AS special_amount
    FROM expense_records er
    JOIN expense_categories_minor ecm ON er.minor_category_id = ecm.id
    WHERE er.family_id = p_family_id
      AND er.expense_year = p_summary_year
      -- 仅包含特殊支出（单笔>=10000 USD）
      AND (
          CASE
              WHEN er.currency = 'USD' THEN er.amount
              ELSE er.amount * COALESCE(
                  (SELECT rate_to_usd
                   FROM exchange_rates
                   WHERE currency = er.currency
                     AND effective_date <= CONCAT(er.expense_year, '-12-31')
                   ORDER BY effective_date DESC
                   LIMIT 1),
                  1.0
              )
          END
      ) >= 10000
    GROUP BY er.major_category_id, er.minor_category_id, ecm.name;

    -- 1.5.2 大类级别的特殊支出汇总并更新到annual_expense_summary
    -- 首先为那些只有特殊支出没有基础支出的大类创建记录
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        p_family_id,
        (SELECT id FROM users WHERE family_id = p_family_id LIMIT 1),
        p_summary_year,
        special_summary.major_category_id,
        NULL,
        0,  -- 基础支出为0
        0,  -- 实际支出先设为0，稍后UPDATE时会更新
        'USD'
    FROM (
        SELECT major_category_id
        FROM temp_special_expenses_minor
        GROUP BY major_category_id
    ) AS special_summary
    WHERE NOT EXISTS (
        SELECT 1 FROM annual_expense_summary
        WHERE family_id = p_family_id
          AND summary_year = p_summary_year
          AND major_category_id = special_summary.major_category_id
          AND minor_category_id IS NULL
    );

    -- 然后更新所有大类的特殊支出数据
    UPDATE annual_expense_summary aes
    JOIN (
        SELECT
            major_category_id,
            SUM(special_amount) AS total_special_amount,
            -- 构建JSON详情：包含所有小类的名称和金额
            JSON_ARRAYAGG(
                JSON_OBJECT(
                    'minorCategoryId', minor_category_id,
                    'minorCategoryName', minor_category_name,
                    'amount', special_amount
                )
            ) AS special_details
        FROM temp_special_expenses_minor
        GROUP BY major_category_id
    ) AS special_summary ON aes.major_category_id = special_summary.major_category_id
    SET
        aes.special_expense_amount = special_summary.total_special_amount,
        aes.special_expense_details = special_summary.special_details,
        -- 更新实际支出公式：基础支出 + 特殊支出 + 资产调整 + 负债调整
        -- 注意：资产调整可能为负（资产减少），负债调整为正（负债减少）
        aes.actual_expense_amount = aes.base_expense_amount
                                   + special_summary.total_special_amount
                                   + COALESCE(aes.asset_adjustment, 0)
                                   + COALESCE(aes.liability_adjustment, 0)
    WHERE aes.family_id = p_family_id
      AND aes.summary_year = p_summary_year
      AND aes.minor_category_id IS NULL;  -- 仅更新大类级别的记录

    -- 清理临时表
    DROP TEMPORARY TABLE IF EXISTS temp_special_expenses_minor;

    -- ========================================
    -- Part 2: 处理房产购买年份的特殊调整
    -- ========================================

    SET done = FALSE;
    OPEN property_cursor;

    property_loop: LOOP
        FETCH property_cursor INTO
            v_property_record_id,
            v_asset_account_id,
            v_purchase_year,
            v_down_payment,
            v_mortgage_amount,
            v_initial_property_value,
            v_property_currency;

        IF done THEN
            LEAVE property_loop;
        END IF;

        -- 获取房产货币的汇率 (转换为USD，使用购买年份年末汇率)
        IF v_property_currency = 'USD' THEN
            SET v_property_exchange_rate = 1.0;
        ELSE
            SELECT COALESCE(rate_to_usd, 1.0) INTO v_property_exchange_rate
            FROM exchange_rates
            WHERE currency = v_property_currency
              AND effective_date <= CONCAT(v_purchase_year, '-12-31')
            ORDER BY effective_date DESC
            LIMIT 1;
        END IF;

        -- 将房产相关金额转换为USD
        SET v_down_payment = v_down_payment * v_property_exchange_rate;
        SET v_mortgage_amount = v_mortgage_amount * v_property_exchange_rate;
        SET v_initial_property_value = v_initial_property_value * v_property_exchange_rate;

        -- 1. 买房成本 = 首付 + 房贷金额 - 房产价值
        SET v_purchase_cost = v_down_payment + v_mortgage_amount - v_initial_property_value;

        -- 2. 获取当年最新房贷余额 (转换为USD，使用汇总年份年末汇率)
        SELECT COALESCE(
            CASE
                WHEN la.currency = 'USD' THEN lr.outstanding_balance
                ELSE lr.outstanding_balance * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = la.currency
                       AND effective_date <= v_end_date
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END,
            0
        ) INTO v_current_mortgage_balance
        FROM asset_accounts aa
        JOIN liability_records lr ON lr.account_id = aa.linked_liability_account_id
        JOIN liability_accounts la ON lr.account_id = la.id
        WHERE aa.id = v_asset_account_id
          AND lr.record_date <= v_end_date
        ORDER BY lr.record_date DESC
        LIMIT 1;

        -- 2.1 获取前一年（p_summary_year - 1）年底的房贷余额 (转换为USD，使用前一年年末汇率)
        SELECT COALESCE(
            CASE
                WHEN la.currency = 'USD' THEN lr.outstanding_balance
                ELSE lr.outstanding_balance * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = la.currency
                       AND effective_date <= CONCAT(p_summary_year - 1, '-12-31')
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END,
            0
        ) INTO v_previous_year_mortgage_balance
        FROM asset_accounts aa
        JOIN liability_records lr ON lr.account_id = aa.linked_liability_account_id
        JOIN liability_accounts la ON lr.account_id = la.id
        WHERE aa.id = v_asset_account_id
          AND lr.record_date <= CONCAT(p_summary_year - 1, '-12-31')
        ORDER BY lr.record_date DESC
        LIMIT 1;

        -- 3. 计算房贷支出年度总和（从expense_records中统计，使用当年年末汇率）
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN er.currency = 'USD' THEN er.amount
                    ELSE er.amount * COALESCE(
                        (SELECT rate_to_usd
                         FROM exchange_rates
                         WHERE currency = er.currency
                           AND effective_date <= CONCAT(er.expense_year, '-12-31')
                         ORDER BY effective_date DESC
                         LIMIT 1),
                        1.0
                    )
                END
            ),
            0
        ) INTO v_total_mortgage_expense
        FROM expense_records er
        JOIN expense_categories_minor ecm ON er.minor_category_id = ecm.id
        WHERE er.family_id = p_family_id
          AND er.expense_year = p_summary_year
          AND (ecm.name LIKE '%房贷%' OR ecm.name = '租房还贷')
          AND ecm.major_category_id = v_housing_category_id;

        -- 4. 累计房贷本金偿还 = (初始房贷金额 - 当年最新房贷余额) + 前一年房贷余额
        SET v_mortgage_principal_paid = (v_mortgage_amount - v_current_mortgage_balance) + v_previous_year_mortgage_balance;

        -- 5. 累计房贷利息 = 房贷支出年度总和 - 累计房贷本金偿还
        SET v_accumulated_interest = v_total_mortgage_expense - v_mortgage_principal_paid;

        -- 6. 调整支出 = (房贷金额 - 房产价值) - 累计房贷本金偿还
        SET v_adjusted_expense = (v_mortgage_amount - v_initial_property_value) - v_mortgage_principal_paid;

        -- 7. 获取当年最新房产价值（从asset_records获取，使用汇总年份年末汇率）
        SELECT COALESCE(
            CASE
                WHEN aa.currency = 'USD' THEN ar.amount
                ELSE ar.amount * COALESCE(
                    (SELECT rate_to_usd
                     FROM exchange_rates
                     WHERE currency = aa.currency
                       AND effective_date <= v_end_date
                     ORDER BY effective_date DESC
                     LIMIT 1),
                    1.0
                )
            END,
            v_initial_property_value
        ) INTO v_current_property_value
        FROM asset_records ar
        JOIN asset_accounts aa ON ar.account_id = aa.id
        WHERE ar.account_id = v_asset_account_id
          AND ar.record_date <= v_end_date
        ORDER BY ar.record_date DESC
        LIMIT 1;

        -- 8. 投资回报 = 当年最新房产价值 - 初始房产价值
        SET v_investment_return = v_current_property_value - v_initial_property_value;

        -- 9. 更新住房大类的调整数据
        UPDATE annual_expense_summary
        SET
            asset_adjustment = v_investment_return,
            liability_adjustment = v_adjusted_expense,
            actual_expense_amount = base_expense_amount + COALESCE(special_expense_amount, 0) + v_adjusted_expense,
            adjustment_details = JSON_ARRAY(
                JSON_OBJECT(
                    'type', 'PROPERTY_PURCHASE',
                    'property_record_id', v_property_record_id,
                    'asset_account_id', v_asset_account_id,
                    'purchase_year', v_purchase_year,
                    'down_payment', v_down_payment,
                    'mortgage_amount', v_mortgage_amount,
                    'initial_property_value', v_initial_property_value,
                    'current_property_value', v_current_property_value,
                    'current_mortgage_balance', v_current_mortgage_balance,
                    'previous_year_mortgage_balance', v_previous_year_mortgage_balance,
                    'total_mortgage_expense', v_total_mortgage_expense,
                    'mortgage_principal_paid', v_mortgage_principal_paid,
                    'accumulated_interest', v_accumulated_interest,
                    'purchase_cost', v_purchase_cost,
                    'adjusted_expense', v_adjusted_expense,
                    'investment_return', v_investment_return
                )
            )
        WHERE family_id = p_family_id
          AND summary_year = p_summary_year
          AND major_category_id = v_housing_category_id
          AND minor_category_id IS NULL;

    END LOOP;

    CLOSE property_cursor;

    -- ========================================
    -- Part 3: 根据配置表进行通用调整（非购买年份使用现有逻辑）
    -- ========================================

    SET done = FALSE;
    OPEN config_cursor;

    config_loop: LOOP
        FETCH config_cursor INTO
            v_major_category_id, v_adjustment_type,
            v_asset_type_code, v_liability_type, v_adjustment_direction;

        IF done THEN
            LEAVE config_loop;
        END IF;

        -- 跳过住房类别的房贷调整（如果当年有房产购买）
        IF v_major_category_id = v_housing_category_id
           AND v_adjustment_type = 'LIABILITY'
           AND v_liability_type = 'MORTGAGE' THEN
            IF EXISTS (
                SELECT 1 FROM annual_expense_summary
                WHERE family_id = p_family_id
                  AND summary_year = p_summary_year
                  AND major_category_id = v_housing_category_id
                  AND adjustment_details IS NOT NULL
                  AND JSON_EXTRACT(adjustment_details, '$[0].type') = 'PROPERTY_PURCHASE'
            ) THEN
                ITERATE config_loop;
            END IF;
        END IF;

        SET v_adjustment_value = 0;

        -- 根据调整类型计算调整值
        IF v_adjustment_type = 'ASSET' THEN
            -- 计算资产变化: 当年截止日 - 去年底 (转换为USD，使用记录所在年份年末汇率)
            SELECT COALESCE(
                (
                    SELECT SUM(
                        CASE
                            WHEN aa.currency = 'USD' THEN ar.amount
                            ELSE ar.amount * COALESCE(
                                (SELECT rate_to_usd
                                 FROM exchange_rates
                                 WHERE currency = aa.currency
                                   AND effective_date <= v_end_date
                                 ORDER BY effective_date DESC
                                 LIMIT 1),
                                1.0
                            )
                        END
                    )
                    FROM asset_accounts aa
                    JOIN asset_type at ON aa.asset_type_id = at.id
                    LEFT JOIN asset_records ar ON aa.id = ar.account_id
                        AND ar.record_date = (
                            SELECT MAX(record_date)
                            FROM asset_records
                            WHERE account_id = aa.id
                              AND record_date <= v_end_date
                        )
                    WHERE at.type = v_asset_type_code
                      AND aa.is_active = 1
                      AND aa.user_id IN (
                          SELECT id FROM users WHERE family_id = p_family_id
                      )
                )
                -
                COALESCE(
                    (
                        SELECT JSON_EXTRACT(asset_breakdown, CONCAT('$."', v_asset_type_code, '"'))
                        FROM annual_financial_summary
                        WHERE family_id = p_family_id
                          AND year = p_summary_year - 1
                        LIMIT 1
                    ),
                    (
                        SELECT SUM(
                            CASE
                                WHEN aa.currency = 'USD' THEN ar.amount
                                ELSE ar.amount * COALESCE(
                                    (SELECT rate_to_usd
                                     FROM exchange_rates
                                     WHERE currency = aa.currency
                                       AND effective_date <= CONCAT(p_summary_year - 1, '-12-31')
                                     ORDER BY effective_date DESC
                                     LIMIT 1),
                                    1.0
                                )
                            END
                        )
                        FROM asset_accounts aa
                        JOIN asset_type at ON aa.asset_type_id = at.id
                        LEFT JOIN asset_records ar ON aa.id = ar.account_id
                            AND ar.record_date = (
                                SELECT MAX(record_date)
                                FROM asset_records
                                WHERE account_id = aa.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE at.type = v_asset_type_code
                          AND aa.is_active = 1
                          AND aa.user_id IN (
                              SELECT id FROM users WHERE family_id = p_family_id
                          )
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
                    THEN base_expense_amount + COALESCE(special_expense_amount, 0) - v_adjustment_value
                    ELSE base_expense_amount + COALESCE(special_expense_amount, 0) + v_adjustment_value
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
            -- 计算负债变化: 去年底 - 当年截止日 (转换为USD，使用记录所在年份年末汇率)
            SELECT COALESCE(
                COALESCE(
                    (
                        SELECT JSON_EXTRACT(liability_breakdown, CONCAT('$."', v_liability_type, '"'))
                        FROM annual_financial_summary
                        WHERE family_id = p_family_id
                          AND year = p_summary_year - 1
                        LIMIT 1
                    ),
                    (
                        SELECT SUM(
                            CASE
                                WHEN la.currency = 'USD' THEN lr.outstanding_balance
                                ELSE lr.outstanding_balance * COALESCE(
                                    (SELECT rate_to_usd
                                     FROM exchange_rates
                                     WHERE currency = la.currency
                                       AND effective_date <= CONCAT(p_summary_year - 1, '-12-31')
                                     ORDER BY effective_date DESC
                                     LIMIT 1),
                                    1.0
                                )
                            END
                        )
                        FROM liability_accounts la
                        JOIN liability_type lt ON la.liability_type_id = lt.id
                        LEFT JOIN liability_records lr ON la.id = lr.account_id
                            AND lr.record_date = (
                                SELECT MAX(record_date)
                                FROM liability_records
                                WHERE account_id = la.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE lt.type = v_liability_type
                          AND la.is_active = 1
                          AND la.user_id IN (
                              SELECT id FROM users WHERE family_id = p_family_id
                          )
                    ),
                    0
                )
                -
                (
                    SELECT SUM(
                        CASE
                            WHEN la.currency = 'USD' THEN lr.outstanding_balance
                            ELSE lr.outstanding_balance * COALESCE(
                                (SELECT rate_to_usd
                                 FROM exchange_rates
                                 WHERE currency = la.currency
                                   AND effective_date <= v_end_date
                                 ORDER BY effective_date DESC
                                 LIMIT 1),
                                1.0
                            )
                        END
                    )
                    FROM liability_accounts la
                    JOIN liability_type lt ON la.liability_type_id = lt.id
                    LEFT JOIN liability_records lr ON la.id = lr.account_id
                        AND lr.record_date = (
                            SELECT MAX(record_date)
                            FROM liability_records
                            WHERE account_id = la.id
                              AND record_date <= v_end_date
                        )
                    WHERE lt.type = v_liability_type
                      AND la.is_active = 1
                      AND la.user_id IN (
                          SELECT id FROM users WHERE family_id = p_family_id
                      )
                ),
                0
            ) INTO v_adjustment_value;

            -- 更新负债调整
            -- 负债调整值 = 去年底负债 - 当年底负债（负债减少为正数）
            -- 实际支出 = 基础支出 + 特殊支出 - 负债减少（扣除负债的减少）
            UPDATE annual_expense_summary
            SET
                liability_adjustment = v_adjustment_value,
                actual_expense_amount = base_expense_amount + COALESCE(special_expense_amount, 0) - v_adjustment_value,
                adjustment_details = JSON_ARRAY(
                    JSON_OBJECT(
                        'type', 'LIABILITY',
                        'code', v_liability_type,
                        'amount', v_adjustment_value
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
    -- Part 4: 计算总计
    -- ========================================
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, special_expense_amount, asset_adjustment, liability_adjustment,
        actual_expense_amount, currency
    )
    SELECT
        p_family_id, NULL, p_summary_year,
        0, NULL,
        SUM(base_expense_amount),
        SUM(COALESCE(special_expense_amount, 0)),
        SUM(COALESCE(asset_adjustment, 0)),
        SUM(COALESCE(liability_adjustment, 0)),
        SUM(actual_expense_amount),
        'USD'
    FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND minor_category_id IS NULL;

    -- ========================================
    -- Part 4.5: 汇总调整详情到总计记录
    -- ========================================
    -- 使用临时表收集所有大类的adjustment_details并合并为一个JSON数组
    DROP TEMPORARY TABLE IF EXISTS temp_adjustment_details;
    CREATE TEMPORARY TABLE temp_adjustment_details AS
    SELECT JSON_ARRAYAGG(JSON_EXTRACT(adjustment_details, '$[0]')) AS aggregated_details
    FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND major_category_id != 0
      AND minor_category_id IS NULL
      AND adjustment_details IS NOT NULL
      AND JSON_LENGTH(adjustment_details) > 0;

    UPDATE annual_expense_summary
    SET adjustment_details = (SELECT aggregated_details FROM temp_adjustment_details)
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND major_category_id = 0;

    DROP TEMPORARY TABLE IF EXISTS temp_adjustment_details;

    -- 返回汇总结果
    SELECT
        aes.id,
        aes.summary_year,
        CASE WHEN aes.major_category_id = 0 THEN '总计' ELSE major.name END AS '大类',
        CASE WHEN aes.minor_category_id IS NULL THEN '小计' ELSE minor.name END AS '小类',
        aes.base_expense_amount AS '基础支出',
        aes.special_expense_amount AS '特殊支出',
        aes.asset_adjustment AS '资产调整',
        aes.liability_adjustment AS '负债调整',
        aes.actual_expense_amount AS '实际支出',
        aes.adjustment_details AS '调整详情',
        aes.special_expense_details AS '特殊支出详情'
    FROM annual_expense_summary aes
    LEFT JOIN expense_categories_major major ON aes.major_category_id = major.id
    LEFT JOIN expense_categories_minor minor ON aes.minor_category_id = minor.id
    WHERE aes.family_id = p_family_id
      AND aes.summary_year = p_summary_year
    ORDER BY
        CASE WHEN aes.major_category_id = 0 THEN 999 ELSE aes.major_category_id END,
        CASE WHEN aes.minor_category_id IS NULL THEN 0 ELSE aes.minor_category_id END;

END ;;

DELIMITER ;
