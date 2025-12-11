-- MySQL dump 10.13  Distrib 9.5.0, for macos15.4 (arm64)
--
-- Host: 10.0.0.7    Database: finance
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
mysqldump: Error: 'Access denied; you need (at least one of) the PROCESS privilege(s) for this operation' when trying to dump tablespaces

--
-- Dumping routines for database 'finance'
--
/*!50003 DROP PROCEDURE IF EXISTS `sp_calculate_annual_summary` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
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
        INNER JOIN asset_categories ac ON aa.category_id = ac.id
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
    INNER JOIN asset_categories ac ON aa.category_id = ac.id
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
        INNER JOIN liability_categories lc ON la.category_id = lc.id
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
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-10 12:27:42
