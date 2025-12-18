-- ============================================================
-- 家庭理财管理系统 - 存储过程
-- ============================================================
-- 包含财务计算相关的存储过程
--
-- 生成时间: $(date '+%Y-%m-%d %H:%M:%S')
-- ============================================================

DELIMITER //

mysqldump: Error: 'Access denied; you need (at least one of) the PROCESS privilege(s) for this operation' when trying to dump tablespaces

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


CREATE DEFINER=`austinxu`@`%` PROCEDURE `sp_calculate_annual_summary`(
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
    DECLARE v_net_asset_breakdown JSON;
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
    -- 计算总资产和资产分类汇总（按TYPE分组，使用年度最后一天的汇率）
    SELECT
        COALESCE(SUM(category_total), 0) AS total,
        COALESCE(
            JSON_OBJECTAGG(category_type, category_total),
            JSON_OBJECT()
        ) AS breakdown
    INTO v_total_assets, v_asset_breakdown
    FROM (
        SELECT
            ac.type AS category_type,
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
        INNER JOIN asset_type ac ON aa.asset_type_id = ac.id
        WHERE u.family_id = p_family_id
            AND ar.record_date = (
                SELECT MAX(ar2.record_date)
                FROM asset_records ar2
                WHERE ar2.account_id = ar.account_id
                    AND ar2.record_date <= v_summary_date
            )
            AND aa.is_active = TRUE
        GROUP BY ac.type
    ) AS asset_summary;
    -- 计算房产资产总额（REAL_ESTATE类型，使用年度最后一天的汇率）
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
    INNER JOIN asset_type ac ON aa.asset_type_id = ac.id
    WHERE u.family_id = p_family_id
        AND ac.type = 'REAL_ESTATE'
        AND ar.record_date = (
            SELECT MAX(ar2.record_date)
            FROM asset_records ar2
            WHERE ar2.account_id = ar.account_id
                AND ar2.record_date <= v_summary_date
        )
        AND aa.is_active = TRUE;
    -- 计算总负债和负债分类汇总（按TYPE分组，使用年度最后一天的汇率）
    SELECT
        COALESCE(SUM(category_total), 0) AS total,
        COALESCE(
            JSON_OBJECTAGG(category_type, category_total),
            JSON_OBJECT()
        ) AS breakdown
    INTO v_total_liabilities, v_liability_breakdown
    FROM (
        SELECT
            lc.type AS category_type,
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
        INNER JOIN liability_type lc ON la.liability_type_id = lc.id
        WHERE u.family_id = p_family_id
            AND lr.record_date = (
                SELECT MAX(lr2.record_date)
                FROM liability_records lr2
                WHERE lr2.account_id = lr.account_id
                    AND lr2.record_date <= v_summary_date
            )
            AND la.is_active = TRUE
        GROUP BY lc.type
    ) AS liability_summary;
    -- 计算净资产
    SET v_net_worth = v_total_assets - v_total_liabilities;
    -- 计算房产净资产和非房产净资产
    -- 房产净资产 = 房产资产 - 房贷
    SET v_real_estate_net_worth = v_real_estate_assets - COALESCE(
        JSON_EXTRACT(v_liability_breakdown, '$.MORTGAGE'),
        0
    );
    SET v_non_real_estate_net_worth = v_net_worth - v_real_estate_net_worth;
    -- 计算净资产分类明细（按net_asset_categories的逻辑）
    -- 这里我们创建一个简单的净资产分类，包含：
    -- 1. REAL_ESTATE_NET: 房地产净值 = REAL_ESTATE资产 - MORTGAGE负债
    -- 2. RETIREMENT_FUND_NET: 退休基金净值 = RETIREMENT_FUND资产
    -- 3. LIQUID_NET: 流动资产净值 = CASH资产 - CREDIT_CARD - OTHER负债
    -- 4. INVESTMENT_NET: 投资净值 = STOCKS + PRECIOUS_METALS + CRYPTOCURRENCY - PERSONAL_LOAN - STUDENT_LOAN
    -- 5. OTHER_NET: 其他净值 = 其他资产 - 其他负债（不包括上面已计算的）
    SET v_net_asset_breakdown = JSON_OBJECT(
        'REAL_ESTATE_NET', v_real_estate_assets - COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.MORTGAGE'), 0),
        'RETIREMENT_FUND_NET', COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.RETIREMENT_FUND'), 0),
        'LIQUID_NET', COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.CASH'), 0) - COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.CREDIT_CARD'), 0),
        'INVESTMENT_NET', (
            COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.STOCKS'), 0) +
            COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.PRECIOUS_METALS'), 0) +
            COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.CRYPTOCURRENCY'), 0)
        ) - (
            COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.PERSONAL_LOAN'), 0) +
            COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.STUDENT_LOAN'), 0)
        ),
        'OTHER_NET', (
            COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.INSURANCE'), 0) +
            COALESCE(JSON_EXTRACT(v_asset_breakdown, '$.OTHER'), 0)
        ) - (
            COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.AUTO_LOAN'), 0) +
            COALESCE(JSON_EXTRACT(v_liability_breakdown, '$.OTHER'), 0)
        )
    );
    -- 插入或更新年度摘要
    INSERT INTO annual_financial_summary (
        family_id, year, summary_date,
        total_assets, total_liabilities, net_worth,
        asset_breakdown, liability_breakdown, net_asset_breakdown,
        currency,
        real_estate_assets, real_estate_net_worth, non_real_estate_net_worth
    ) VALUES (
        p_family_id, p_year, v_summary_date,
        v_total_assets, v_total_liabilities, v_net_worth,
        v_asset_breakdown, v_liability_breakdown, v_net_asset_breakdown,
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
        net_asset_breakdown = v_net_asset_breakdown,
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
        -- 同比绝对值：如果没有上一年数据，则为NULL（基准年）
        current.yoy_asset_change = IF(previous.id IS NOT NULL, current.total_assets - previous.total_assets, NULL),
        current.yoy_liability_change = IF(previous.id IS NOT NULL, current.total_liabilities - previous.total_liabilities, NULL),
        current.yoy_net_worth_change = IF(previous.id IS NOT NULL, current.net_worth - previous.net_worth, NULL),
        -- 同比百分比：如果没有上一年数据或上一年为0，则为NULL；限制范围在-999.99到999.99之间
        current.yoy_asset_change_pct = IF(previous.id IS NOT NULL AND previous.total_assets > 0,
            LEAST(999.99, GREATEST(-999.99, ((current.total_assets - previous.total_assets) / previous.total_assets) * 100)),
            NULL),
        current.yoy_liability_change_pct = IF(previous.id IS NOT NULL AND previous.total_liabilities > 0,
            LEAST(999.99, GREATEST(-999.99, ((current.total_liabilities - previous.total_liabilities) / previous.total_liabilities) * 100)),
            NULL),
        current.yoy_net_worth_change_pct = IF(previous.id IS NOT NULL AND previous.net_worth > 0,
            LEAST(999.99, GREATEST(-999.99, ((current.net_worth - previous.net_worth) / previous.net_worth) * 100)),
            NULL),
        -- 房产净值同比
        current.yoy_real_estate_net_worth_change = IF(previous.id IS NOT NULL, current.real_estate_net_worth - previous.real_estate_net_worth, NULL),
        current.yoy_real_estate_net_worth_change_pct = IF(previous.id IS NOT NULL AND previous.real_estate_net_worth > 0,
            LEAST(999.99, GREATEST(-999.99, ((current.real_estate_net_worth - previous.real_estate_net_worth) / previous.real_estate_net_worth) * 100)),
            NULL),
        -- 非房产净值同比
        current.yoy_non_real_estate_net_worth_change = IF(previous.id IS NOT NULL, current.non_real_estate_net_worth - previous.non_real_estate_net_worth, NULL),
        current.yoy_non_real_estate_net_worth_change_pct = IF(previous.id IS NOT NULL AND previous.non_real_estate_net_worth > 0,
            LEAST(999.99, GREATEST(-999.99, ((current.non_real_estate_net_worth - previous.non_real_estate_net_worth) / previous.non_real_estate_net_worth) * 100)),
            NULL),
        -- 房产占比
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
END ;;


DELIMITER ;
