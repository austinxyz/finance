-- ============================================
-- 更新年度支出汇总存储过程，支持房产购买年份的特殊计算逻辑
-- ============================================

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

    -- 房产相关变量
    DECLARE v_property_asset_id BIGINT;
    DECLARE v_down_payment DECIMAL(18,2);
    DECLARE v_mortgage_amount DECIMAL(18,2);
    DECLARE v_property_value DECIMAL(18,2);
    DECLARE v_property_currency VARCHAR(10);
    DECLARE v_current_property_value DECIMAL(18,2);
    DECLARE v_current_mortgage_balance DECIMAL(18,2);
    DECLARE v_property_expense_total DECIMAL(18,2);
    DECLARE v_mortgage_interest DECIMAL(18,2);
    DECLARE v_purchase_cost DECIMAL(18,2);
    DECLARE v_adjustment_amount DECIMAL(18,2);
    DECLARE v_investment_return DECIMAL(18,2);

    -- 游标: 遍历所有启用的调整配置
    DECLARE config_cursor CURSOR FOR
        SELECT major_category_id, adjustment_type, asset_type_code,
               liability_type, adjustment_direction
        FROM expense_category_adjustment_config
        WHERE is_active = 1;

    -- 游标: 遍历该家庭在该年份购买的房产
    DECLARE property_cursor CURSOR FOR
        SELECT pr.asset_account_id, pr.down_payment, pr.mortgage_amount,
               pr.property_value, pr.currency
        FROM property_records pr
        JOIN asset_accounts aa ON pr.asset_account_id = aa.id
        WHERE aa.user_id IN (
            SELECT user_id FROM family_members WHERE family_id = p_family_id
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
        SUM(amount),
        SUM(amount),
        currency
    FROM expense_records
    WHERE family_id = p_family_id AND expense_year = p_summary_year
    GROUP BY family_id, user_id, expense_year, major_category_id, minor_category_id, currency;

    -- 1.2 大类级别汇总
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        family_id, MAX(user_id), expense_year,
        major_category_id, NULL,
        SUM(amount),
        SUM(amount),
        currency
    FROM expense_records
    WHERE family_id = p_family_id AND expense_year = p_summary_year
    GROUP BY family_id, expense_year, major_category_id, currency;

    -- ========================================
    -- Part 2: 房产购买年份的特殊处理
    -- ========================================

    -- 检查该年份是否有房产购买
    SET done = FALSE;
    OPEN property_cursor;

    property_loop: LOOP
        FETCH property_cursor INTO
            v_property_asset_id, v_down_payment, v_mortgage_amount,
            v_property_value, v_property_currency;

        IF done THEN
            LEAVE property_loop;
        END IF;

        -- 2.1 获取当年末房产价值
        SELECT COALESCE(MAX(balance), v_property_value)
        INTO v_current_property_value
        FROM asset_records
        WHERE asset_account_id = v_property_asset_id
          AND record_date <= v_end_date;

        -- 2.2 获取当年末房贷余额
        SELECT COALESCE(MAX(lr.balance), v_mortgage_amount)
        INTO v_current_mortgage_balance
        FROM asset_accounts aa
        JOIN liability_accounts la ON aa.linked_liability_account_id = la.id
        LEFT JOIN liability_records lr ON la.id = lr.liability_account_id
        WHERE aa.id = v_property_asset_id
          AND lr.record_date <= v_end_date;

        -- 如果没有找到负债记录，使用初始贷款金额
        IF v_current_mortgage_balance IS NULL THEN
            SET v_current_mortgage_balance = v_mortgage_amount;
        END IF;

        -- 2.3 计算该房产相关的支出总和（首付 + 房贷月供）
        -- 查找与该房产关联的支出记录（通过支出描述或特定分类）
        -- 这里假设首付记录在支出表中，金额等于down_payment
        SELECT COALESCE(SUM(amount), 0)
        INTO v_property_expense_total
        FROM expense_records
        WHERE family_id = p_family_id
          AND expense_year = p_summary_year
          AND currency = v_property_currency
          AND (
              -- 首付支出（假设金额与down_payment相等）
              amount = v_down_payment
              OR
              -- 房贷月供（通过描述或分类识别，这里需要根据实际情况调整）
              description LIKE '%房贷%'
              OR description LIKE '%月供%'
              OR description LIKE '%mortgage%'
          );

        -- 2.4 计算指标
        -- 买房成本 = 首付 + 房贷金额 - 房产价值
        SET v_purchase_cost = v_down_payment + v_mortgage_amount - v_property_value;

        -- 累计房贷利息 = 房贷支出总和 - (初始房贷 - 当前房贷)
        SET v_mortgage_interest = v_property_expense_total - (v_mortgage_amount - v_current_mortgage_balance);

        -- 调整支出 = (房贷金额 - 房产价值) + 累计房贷利息
        SET v_adjustment_amount = (v_mortgage_amount - v_property_value) + v_mortgage_interest;

        -- 投资回报 = 当前房产价值 - 初始房产价值
        SET v_investment_return = v_current_property_value - v_property_value;

        -- 2.5 更新年度汇总表
        -- 这里需要找到首付所属的支出大类，并调整其实际支出
        -- 假设首付在"住房"或"大额资产购置"类别下
        UPDATE annual_expense_summary aes
        SET
            asset_adjustment = -v_investment_return,  -- 资产增值调整（减少支出）
            liability_adjustment = v_mortgage_amount - v_current_mortgage_balance,  -- 负债减少调整（增加支出）
            actual_expense_amount = base_expense_amount + v_adjustment_amount,
            adjustment_details = JSON_OBJECT(
                'type', 'PROPERTY_PURCHASE',
                'asset_account_id', v_property_asset_id,
                'down_payment', v_down_payment,
                'mortgage_amount', v_mortgage_amount,
                'property_value', v_property_value,
                'current_property_value', v_current_property_value,
                'current_mortgage_balance', v_current_mortgage_balance,
                'purchase_cost', v_purchase_cost,
                'mortgage_interest', v_mortgage_interest,
                'adjustment_amount', v_adjustment_amount,
                'investment_return', v_investment_return
            )
        WHERE family_id = p_family_id
          AND summary_year = p_summary_year
          AND currency = v_property_currency
          AND minor_category_id IS NULL
          AND major_category_id IN (
              -- 查找包含首付支出的大类
              SELECT DISTINCT major_category_id
              FROM expense_records
              WHERE family_id = p_family_id
                AND expense_year = p_summary_year
                AND amount = v_down_payment
          )
        LIMIT 1;

    END LOOP property_loop;

    CLOSE property_cursor;

    -- ========================================
    -- Part 3: 非房产购买年份的常规调整
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

        SET v_adjustment_value = 0;

        -- 根据调整类型计算调整值
        IF v_adjustment_type = 'ASSET' THEN
            -- 资产调整逻辑（与原存储过程相同）
            SELECT COALESCE(
                (
                    SELECT SUM(ar.balance)
                    FROM asset_accounts aa
                    JOIN asset_types at ON aa.asset_type_id = at.id
                    LEFT JOIN asset_records ar ON aa.id = ar.asset_account_id
                        AND ar.record_date = (
                            SELECT MAX(record_date)
                            FROM asset_records
                            WHERE asset_account_id = aa.id
                              AND record_date <= v_end_date
                        )
                    WHERE at.code = v_asset_type_code
                      AND aa.is_active = 1
                      AND aa.user_id IN (
                          SELECT user_id FROM family_members WHERE family_id = p_family_id
                      )
                )
                -
                COALESCE(
                    (
                        SELECT SUM(ar.balance)
                        FROM asset_accounts aa
                        JOIN asset_types at ON aa.asset_type_id = at.id
                        LEFT JOIN asset_records ar ON aa.id = ar.asset_account_id
                            AND ar.record_date = (
                                SELECT MAX(record_date)
                                FROM asset_records
                                WHERE asset_account_id = aa.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE at.code = v_asset_type_code
                          AND aa.is_active = 1
                          AND aa.user_id IN (
                              SELECT user_id FROM family_members WHERE family_id = p_family_id
                          )
                    ),
                    0
                ),
                0
            ) INTO v_adjustment_value;

            -- 更新资产调整（仅更新未被房产购买逻辑处理的记录）
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
                adjustment_details = JSON_OBJECT(
                    'type', 'ASSET',
                    'code', v_asset_type_code,
                    'amount', v_adjustment_value,
                    'direction', v_adjustment_direction
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL
              AND adjustment_details IS NULL;  -- 仅更新未处理的记录

        ELSEIF v_adjustment_type = 'LIABILITY' THEN
            -- 负债调整逻辑（与原存储过程相同）
            SELECT COALESCE(
                COALESCE(
                    (
                        SELECT SUM(lr.balance)
                        FROM liability_accounts la
                        JOIN liability_types lt ON la.liability_type_id = lt.id
                        LEFT JOIN liability_records lr ON la.id = lr.liability_account_id
                            AND lr.record_date = (
                                SELECT MAX(record_date)
                                FROM liability_records
                                WHERE liability_account_id = la.id
                                  AND record_date <= CONCAT(p_summary_year - 1, '-12-31')
                            )
                        WHERE lt.type = v_liability_type
                          AND la.is_active = 1
                          AND la.user_id IN (
                              SELECT user_id FROM family_members WHERE family_id = p_family_id
                          )
                    ),
                    0
                )
                -
                (
                    SELECT COALESCE(SUM(lr.balance), 0)
                    FROM liability_accounts la
                    JOIN liability_types lt ON la.liability_type_id = lt.id
                    LEFT JOIN liability_records lr ON la.id = lr.liability_account_id
                        AND lr.record_date = (
                            SELECT MAX(record_date)
                            FROM liability_records
                            WHERE liability_account_id = la.id
                              AND record_date <= v_end_date
                        )
                    WHERE lt.type = v_liability_type
                      AND la.is_active = 1
                      AND la.user_id IN (
                          SELECT user_id FROM family_members WHERE family_id = p_family_id
                      )
                ),
                0
            ) INTO v_adjustment_value;

            -- 更新负债调整（仅更新未被房产购买逻辑处理的记录）
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
                adjustment_details = JSON_MERGE_PRESERVE(
                    COALESCE(adjustment_details, JSON_OBJECT()),
                    JSON_OBJECT(
                        'liability_type', v_liability_type,
                        'amount', v_adjustment_value,
                        'direction', v_adjustment_direction
                    )
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL
              AND (
                  adjustment_details IS NULL
                  OR JSON_EXTRACT(adjustment_details, '$.type') != 'PROPERTY_PURCHASE'
              );

        END IF;

    END LOOP config_loop;

    CLOSE config_cursor;

    -- ========================================
    -- Part 4: 计算总计
    -- ========================================
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, asset_adjustment, liability_adjustment,
        actual_expense_amount, currency
    )
    SELECT
        family_id, MAX(user_id), summary_year,
        0, NULL,
        SUM(base_expense_amount),
        SUM(COALESCE(asset_adjustment, 0)),
        SUM(COALESCE(liability_adjustment, 0)),
        SUM(actual_expense_amount),
        currency
    FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND major_category_id > 0
      AND minor_category_id IS NULL
    GROUP BY family_id, summary_year, currency;

END//

DELIMITER ;
