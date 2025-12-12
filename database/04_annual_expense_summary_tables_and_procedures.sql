-- ============================================================================
-- 年度支出汇总功能 - 表结构、初始数据和存储过程
-- 版本: v2 (USD基准货币)
-- 创建日期: 2025-12-12
-- ============================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;

-- ============================================================================
-- 1. 支出类别调整配置表
-- ============================================================================

CREATE TABLE IF NOT EXISTS `expense_category_adjustment_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `major_category_id` bigint NOT NULL COMMENT '支出大类ID',
  `adjustment_type` enum('ASSET','LIABILITY') NOT NULL COMMENT '调整类型',
  `asset_type_code` varchar(50) DEFAULT NULL COMMENT '资产类型代码 (INSURANCE, REAL_ESTATE等)',
  `liability_type` varchar(50) DEFAULT NULL COMMENT '负债类型 (MORTGAGE, AUTO_LOAN等)',
  `adjustment_direction` enum('ADD','SUBTRACT') NOT NULL COMMENT '调整方向',
  `description` text COMMENT '调整说明',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_type` (`major_category_id`,`adjustment_type`,`asset_type_code`,`liability_type`),
  KEY `idx_major_category` (`major_category_id`),
  KEY `idx_asset_type` (`asset_type_code`),
  KEY `idx_liability_type` (`liability_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出类别调整配置表';

-- ============================================================================
-- 2. 年度支出汇总表
-- ============================================================================

CREATE TABLE IF NOT EXISTS `annual_expense_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `summary_year` int NOT NULL COMMENT '汇总年份',
  `major_category_id` bigint NOT NULL COMMENT '大类ID (0表示总计)',
  `minor_category_id` bigint DEFAULT NULL COMMENT '小类ID (NULL表示大类汇总)',
  `base_expense_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '基础支出(expense_records汇总)',
  `asset_adjustment` decimal(18,2) DEFAULT '0.00' COMMENT '资产调整额 (资产增加为正, 减少为负)',
  `liability_adjustment` decimal(18,2) DEFAULT '0.00' COMMENT '负债调整额 (负债减少为正, 增加为负)',
  `adjustment_details` json DEFAULT NULL COMMENT '调整详情 [{type, code, amount, direction}]',
  `actual_expense_amount` decimal(18,2) NOT NULL COMMENT '实际年度支出',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币代码',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary` (`family_id`,`summary_year`,`major_category_id`,`minor_category_id`),
  KEY `idx_family_year` (`family_id`,`summary_year`),
  KEY `idx_major_category` (`major_category_id`),
  KEY `idx_minor_category` (`minor_category_id`),
  CONSTRAINT `fk_annual_summary_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `fk_annual_summary_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_annual_summary_minor` FOREIGN KEY (`minor_category_id`) REFERENCES `expense_categories_minor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度支出汇总表 (通用可配置版本)';

-- ============================================================================
-- 3. 初始化配置数据
-- ============================================================================

-- 注意: 需要先确认major_category_id值，以下假设:
--   4 = 住 (HOUSING)
--   6 = 保险 (INSURANCE)

-- 配置1: 住房类别 - 房贷本金偿还(负债减少)从基础支出中扣除
INSERT INTO `expense_category_adjustment_config`
  (`major_category_id`, `adjustment_type`, `asset_type_code`, `liability_type`, `adjustment_direction`, `description`, `is_active`)
VALUES
  (4, 'LIABILITY', NULL, 'MORTGAGE', 'SUBTRACT', '房贷本金偿还属于资产转换而非消费，从基础支出中扣除', 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `is_active` = VALUES(`is_active`);

-- 配置2: 保险类别 - 保险现金价值增加(资产增加)从基础支出中扣除
INSERT INTO `expense_category_adjustment_config`
  (`major_category_id`, `adjustment_type`, `asset_type_code`, `liability_type`, `adjustment_direction`, `description`, `is_active`)
VALUES
  (6, 'ASSET', 'INSURANCE', NULL, 'SUBTRACT', '保险现金价值增加属于资产积累而非消费，从基础支出中扣除', 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `is_active` = VALUES(`is_active`);

-- ============================================================================
-- 4. 存储过程 - 年度支出汇总计算 (USD基准货币版本)
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS `calculate_annual_expense_summary_v2`//

CREATE PROCEDURE `calculate_annual_expense_summary_v2`(
    IN p_family_id BIGINT,
    IN p_summary_year INT
)
BEGIN
    DECLARE v_major_category_id BIGINT;
    DECLARE v_adjustment_type VARCHAR(20);
    DECLARE v_asset_type_code VARCHAR(50);
    DECLARE v_liability_type VARCHAR(50);
    DECLARE v_adjustment_direction VARCHAR(20);
    DECLARE v_adjustment_value_usd DECIMAL(18,2);
    DECLARE v_current_year_value DECIMAL(18,2);
    DECLARE v_last_year_value DECIMAL(18,2);
    DECLARE done INT DEFAULT FALSE;

    DECLARE config_cursor CURSOR FOR
        SELECT major_category_id, adjustment_type, asset_type_code,
               liability_type, adjustment_direction
        FROM expense_category_adjustment_config
        WHERE is_active = 1;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 清空当年的汇总数据
    DELETE FROM annual_expense_summary
    WHERE family_id = p_family_id AND summary_year = p_summary_year;

    -- ========================================================================
    -- Step 1: 计算基础支出 (使用amount和currency字段，通过汇率转换为USD)
    -- ========================================================================

    -- 1.1 小类级别
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        er.family_id, er.user_id, er.expense_year,
        er.major_category_id, er.minor_category_id,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd FROM exchange_rates
                     WHERE currency = er.currency
                       AND is_active = 1
                       AND effective_date <= CONCAT(er.expense_year, '-', LPAD(er.expense_month, 2, '0'), '-01')
                     ORDER BY effective_date DESC LIMIT 1),
                    0
                )
            END
        ) AS total_usd,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd FROM exchange_rates
                     WHERE currency = er.currency
                       AND is_active = 1
                       AND effective_date <= CONCAT(er.expense_year, '-', LPAD(er.expense_month, 2, '0'), '-01')
                     ORDER BY effective_date DESC LIMIT 1),
                    0
                )
            END
        ) AS total_usd,
        'USD'
    FROM expense_records er
    WHERE er.family_id = p_family_id AND er.expense_year = p_summary_year
    GROUP BY er.family_id, er.user_id, er.expense_year, er.major_category_id, er.minor_category_id;

    -- 1.2 大类级别
    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, actual_expense_amount, currency
    )
    SELECT
        er.family_id, MAX(er.user_id), er.expense_year,
        er.major_category_id, NULL,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd FROM exchange_rates
                     WHERE currency = er.currency
                       AND is_active = 1
                       AND effective_date <= CONCAT(er.expense_year, '-', LPAD(er.expense_month, 2, '0'), '-01')
                     ORDER BY effective_date DESC LIMIT 1),
                    0
                )
            END
        ) AS total_usd,
        SUM(
            CASE
                WHEN er.currency = 'USD' THEN er.amount
                ELSE er.amount * COALESCE(
                    (SELECT rate_to_usd FROM exchange_rates
                     WHERE currency = er.currency
                       AND is_active = 1
                       AND effective_date <= CONCAT(er.expense_year, '-', LPAD(er.expense_month, 2, '0'), '-01')
                     ORDER BY effective_date DESC LIMIT 1),
                    0
                )
            END
        ) AS total_usd,
        'USD'
    FROM expense_records er
    WHERE er.family_id = p_family_id AND er.expense_year = p_summary_year
    GROUP BY er.family_id, er.expense_year, er.major_category_id;

    -- ========================================================================
    -- Step 2: 根据annual_financial_summary调整 (已经是USD，直接使用)
    -- ========================================================================

    OPEN config_cursor;

    config_loop: LOOP
        FETCH config_cursor INTO
            v_major_category_id, v_adjustment_type,
            v_asset_type_code, v_liability_type, v_adjustment_direction;

        IF done THEN
            LEAVE config_loop;
        END IF;

        SET v_adjustment_value_usd = 0;
        SET v_current_year_value = 0;
        SET v_last_year_value = 0;

        IF v_adjustment_type = 'ASSET' THEN
            -- 从annual_financial_summary的asset_breakdown中提取当年值（USD）
            SELECT COALESCE(
                CAST(JSON_EXTRACT(asset_breakdown, CONCAT('$."', v_asset_type_code, '"')) AS DECIMAL(18,2)),
                0
            ) INTO v_current_year_value
            FROM annual_financial_summary
            WHERE family_id = p_family_id AND year = p_summary_year
            LIMIT 1;

            -- 从annual_financial_summary的asset_breakdown中提取去年值（USD）
            SELECT COALESCE(
                CAST(JSON_EXTRACT(asset_breakdown, CONCAT('$."', v_asset_type_code, '"')) AS DECIMAL(18,2)),
                0
            ) INTO v_last_year_value
            FROM annual_financial_summary
            WHERE family_id = p_family_id AND year = p_summary_year - 1
            LIMIT 1;

            -- 计算资产变化（USD）
            SET v_adjustment_value_usd = v_current_year_value - v_last_year_value;

            -- 更新汇总表: 资产增加从支出中扣除
            UPDATE annual_expense_summary
            SET
                asset_adjustment = v_adjustment_value_usd,
                actual_expense_amount = base_expense_amount - v_adjustment_value_usd,
                adjustment_details = JSON_ARRAY(
                    JSON_OBJECT(
                        'type', 'ASSET',
                        'code', v_asset_type_code,
                        'current_year', v_current_year_value,
                        'last_year', v_last_year_value,
                        'change', v_adjustment_value_usd,
                        'direction', 'SUBTRACT'
                    )
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL;

        ELSEIF v_adjustment_type = 'LIABILITY' THEN
            -- 从annual_financial_summary的liability_breakdown中提取当年值（USD）
            SELECT COALESCE(
                CAST(JSON_EXTRACT(liability_breakdown, CONCAT('$."', v_liability_type, '"')) AS DECIMAL(18,2)),
                0
            ) INTO v_current_year_value
            FROM annual_financial_summary
            WHERE family_id = p_family_id AND year = p_summary_year
            LIMIT 1;

            -- 从annual_financial_summary的liability_breakdown中提取去年值（USD）
            SELECT COALESCE(
                CAST(JSON_EXTRACT(liability_breakdown, CONCAT('$."', v_liability_type, '"')) AS DECIMAL(18,2)),
                0
            ) INTO v_last_year_value
            FROM annual_financial_summary
            WHERE family_id = p_family_id AND year = p_summary_year - 1
            LIMIT 1;

            -- 计算负债变化（USD）（负债减少为正数）
            SET v_adjustment_value_usd = v_last_year_value - v_current_year_value;

            -- 更新汇总表: 负债减少从支出中扣除
            UPDATE annual_expense_summary
            SET
                liability_adjustment = v_adjustment_value_usd,
                actual_expense_amount = base_expense_amount - v_adjustment_value_usd,
                adjustment_details = JSON_ARRAY(
                    JSON_OBJECT(
                        'type', 'LIABILITY',
                        'code', v_liability_type,
                        'current_year', v_current_year_value,
                        'last_year', v_last_year_value,
                        'change', v_adjustment_value_usd,
                        'direction', 'SUBTRACT'
                    )
                )
            WHERE family_id = p_family_id
              AND summary_year = p_summary_year
              AND major_category_id = v_major_category_id
              AND minor_category_id IS NULL;
        END IF;

    END LOOP;

    CLOSE config_cursor;

    -- ========================================================================
    -- Step 3: 计算总计（使用COALESCE处理NULL值）
    -- ========================================================================

    INSERT INTO annual_expense_summary (
        family_id, user_id, summary_year,
        major_category_id, minor_category_id,
        base_expense_amount, asset_adjustment, liability_adjustment,
        actual_expense_amount, currency
    )
    SELECT
        p_family_id, NULL, p_summary_year,
        0, NULL,
        COALESCE(SUM(base_expense_amount), 0),
        COALESCE(SUM(asset_adjustment), 0),
        COALESCE(SUM(liability_adjustment), 0),
        COALESCE(SUM(actual_expense_amount), 0),
        'USD'
    FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND minor_category_id IS NULL
      AND major_category_id != 0;

    -- 返回汇总结果
    SELECT
        aes.id,
        aes.summary_year AS '年份',
        CASE WHEN aes.major_category_id = 0 THEN '总计' ELSE major.name END AS '大类',
        CASE WHEN aes.minor_category_id IS NULL THEN '小计' ELSE minor.name END AS '小类',
        aes.base_expense_amount AS '基础支出(USD)',
        COALESCE(aes.asset_adjustment, 0) AS '资产调整(USD)',
        COALESCE(aes.liability_adjustment, 0) AS '负债调整(USD)',
        aes.actual_expense_amount AS '实际支出(USD)',
        aes.currency AS '货币'
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

-- ============================================================================
-- 使用说明
-- ============================================================================
--
-- 调用存储过程:
--   CALL calculate_annual_expense_summary_v2(1, 2025);
--
-- 查询汇总结果:
--   SELECT * FROM annual_expense_summary
--   WHERE family_id = 1 AND summary_year = 2025
--   ORDER BY major_category_id, minor_category_id;
--
-- 配置新的调整规则:
--   INSERT INTO expense_category_adjustment_config
--     (major_category_id, adjustment_type, asset_type_code, liability_type,
--      adjustment_direction, description, is_active)
--   VALUES
--     (7, 'ASSET', 'REAL_ESTATE', NULL, 'SUBTRACT', '房产增值扣除', 1);
--
-- ============================================================================

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
