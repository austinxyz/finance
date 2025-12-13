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
-- Table structure for table `annual_financial_summary`
--

DROP TABLE IF EXISTS `annual_financial_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `annual_financial_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `year` int NOT NULL COMMENT '年份',
  `summary_date` date NOT NULL COMMENT '摘要日期（该年度离12/31最近的记录日期）',
  `total_assets` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总资产',
  `total_liabilities` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总负债',
  `net_worth` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '净资产',
  `asset_breakdown` json DEFAULT NULL COMMENT '资产分类明细 {category_name: amount}',
  `liability_breakdown` json DEFAULT NULL COMMENT '负债分类明细 {category_name: amount}',
  `net_asset_breakdown` json DEFAULT NULL COMMENT '净资产分类明细（按净资产分类）',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币单位',
  `yoy_asset_change` decimal(18,2) DEFAULT NULL COMMENT '资产同比变化',
  `yoy_liability_change` decimal(18,2) DEFAULT NULL COMMENT '负债同比变化',
  `yoy_net_worth_change` decimal(18,2) DEFAULT NULL COMMENT '净资产同比变化',
  `yoy_asset_change_pct` decimal(5,2) DEFAULT NULL COMMENT '资产同比变化百分比',
  `yoy_liability_change_pct` decimal(5,2) DEFAULT NULL COMMENT '负债同比变化百分比',
  `yoy_net_worth_change_pct` decimal(5,2) DEFAULT NULL COMMENT '净资产同比变化百分比',
  `notes` text COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `real_estate_net_worth` decimal(18,2) DEFAULT NULL COMMENT '房产净资产（房产资产 - 房贷）',
  `non_real_estate_net_worth` decimal(18,2) DEFAULT NULL COMMENT '非房产净资产',
  `yoy_real_estate_net_worth_change` decimal(18,2) DEFAULT NULL COMMENT '房产净资产同比变化金额',
  `yoy_real_estate_net_worth_change_pct` decimal(5,2) DEFAULT NULL COMMENT '房产净资产同比变化百分比',
  `yoy_non_real_estate_net_worth_change` decimal(18,2) DEFAULT NULL COMMENT '非房产净资产同比变化金额',
  `yoy_non_real_estate_net_worth_change_pct` decimal(5,2) DEFAULT NULL COMMENT '非房产净资产同比变化百分比',
  `real_estate_asset_ratio` decimal(5,2) DEFAULT NULL COMMENT '房产资产占总资产比例',
  `real_estate_net_worth_ratio` decimal(5,2) DEFAULT NULL COMMENT '房产净资产占总净资产比例',
  `real_estate_assets` decimal(18,2) DEFAULT NULL COMMENT '房产资产总额（不含负债）',
  `real_estate_to_net_worth_ratio` decimal(5,2) DEFAULT NULL COMMENT '房产资产总值占净资产比例',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_year` (`family_id`,`year`),
  KEY `idx_family_year` (`family_id`,`year` DESC),
  KEY `idx_summary_date` (`summary_date`),
  CONSTRAINT `annual_financial_summary_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=514 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度财务摘要表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_accounts`
--

DROP TABLE IF EXISTS `asset_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `category_id` bigint NOT NULL COMMENT '类别ID',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `account_number` varchar(100) DEFAULT NULL COMMENT '账号/编号',
  `institution` varchar(100) DEFAULT NULL COMMENT '机构名称 (银行/券商等)',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币类型',
  `notes` text COMMENT '备注',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否活跃',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tax_status` enum('TAXABLE','TAX_FREE','TAX_DEFERRED') DEFAULT NULL,
  `is_primary_residence` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `idx_user_category` (`user_id`,`category_id`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  CONSTRAINT `asset_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `asset_accounts_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `asset_categories` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_categories`
--

DROP TABLE IF EXISTS `asset_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) NOT NULL COMMENT '类别名称',
  `type` varchar(50) NOT NULL COMMENT '类别类型: CASH, STOCKS, RETIREMENT_FUND, INSURANCE, REAL_ESTATE, CRYPTOCURRENCY',
  `description` text COMMENT '描述',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `color` varchar(20) DEFAULT NULL COMMENT '显示颜色',
  `display_order` int DEFAULT '0' COMMENT '显示顺序',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`,`name`),
  KEY `idx_user_type` (`user_id`,`type`),
  CONSTRAINT `asset_categories_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_liability_type_mappings`
--

DROP TABLE IF EXISTS `asset_liability_type_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_liability_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_type` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `liability_type` varchar(50) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_records`
--

DROP TABLE IF EXISTS `asset_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `amount` decimal(18,2) NOT NULL COMMENT '金额',
  `quantity` decimal(18,6) DEFAULT NULL COMMENT '数量（股票/基金份额等）',
  `unit_price` decimal(18,6) DEFAULT NULL COMMENT '单价',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币',
  `notes` text COMMENT '备注',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件URL（账单截图等）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_date` (`account_id`,`record_date`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_account_date` (`account_id`,`record_date`),
  CONSTRAINT `asset_records_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `asset_records_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `asset_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=761 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `budgets`
--

DROP TABLE IF EXISTS `budgets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budgets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `budget_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预算名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类',
  `amount` decimal(18,2) NOT NULL COMMENT '预算金额',
  `period` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期: MONTHLY, QUARTERLY, YEARLY',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY',
  `is_active` tinyint(1) DEFAULT '1',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_period` (`user_id`,`period`,`is_active`),
  KEY `idx_user_dates` (`user_id`,`start_date`,`end_date`),
  CONSTRAINT `budgets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exchange_rates`
--

DROP TABLE IF EXISTS `exchange_rates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exchange_rates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `currency` varchar(10) NOT NULL,
  `rate_to_usd` decimal(18,8) NOT NULL,
  `effective_date` date NOT NULL,
  `source` varchar(100) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_currency_date` (`currency`,`effective_date`),
  UNIQUE KEY `UKicuvr10gmn700mnd6uxf19ftq` (`currency`,`effective_date`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `expense_categories_major`
--

DROP TABLE IF EXISTS `expense_categories_major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_categories_major` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(50) NOT NULL COMMENT '大类编码（CHILDREN, CLOTHING等）',
  `name` varchar(50) NOT NULL COMMENT '大类名称',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标（emoji或图标类名）',
  `color` varchar(20) DEFAULT NULL COMMENT '颜色代码',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `description` text COMMENT '说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出大类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `expense_categories_minor`
--

DROP TABLE IF EXISTS `expense_categories_minor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_categories_minor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `major_category_id` bigint NOT NULL COMMENT '所属大类ID',
  `name` varchar(100) NOT NULL COMMENT '子分类名称',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统默认分类',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序',
  `description` text COMMENT '说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_name` (`major_category_id`,`name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_major_category` (`major_category_id`),
  CONSTRAINT `expense_categories_minor_ibfk_1` FOREIGN KEY (`major_category_id`) REFERENCES `expense_categories_major` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出子分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `expense_records`
--

DROP TABLE IF EXISTS `expense_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `user_id` bigint NOT NULL COMMENT '记录人ID',
  `expense_year` int NOT NULL COMMENT '支出年份',
  `expense_month` int NOT NULL COMMENT '支出月份',
  `expense_period` varchar(7) NOT NULL COMMENT '支出期间（YYYY-MM）',
  `major_category_id` bigint NOT NULL COMMENT '大类ID',
  `minor_category_id` bigint NOT NULL COMMENT '子分类ID',
  `amount` decimal(18,2) NOT NULL COMMENT '支出金额',
  `currency` varchar(10) NOT NULL DEFAULT 'USD' COMMENT '货币代码',
  `expense_type` varchar(20) NOT NULL COMMENT '支出类型（FIXED_DAILY/LARGE_IRREGULAR）',
  `description` text COMMENT '说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_category` (`family_id`,`expense_period`,`minor_category_id`),
  KEY `user_id` (`user_id`),
  KEY `idx_family_period` (`family_id`,`expense_year`,`expense_month`),
  KEY `idx_expense_period` (`expense_period`),
  KEY `idx_major_category` (`major_category_id`),
  KEY `idx_minor_category` (`minor_category_id`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `expense_records_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `expense_records_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `expense_records_ibfk_3` FOREIGN KEY (`major_category_id`) REFERENCES `expense_categories_major` (`id`),
  CONSTRAINT `expense_records_ibfk_4` FOREIGN KEY (`minor_category_id`) REFERENCES `expense_categories_minor` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `families`
--

DROP TABLE IF EXISTS `families`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `families` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `annual_expenses` decimal(15,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `emergency_fund_months` int DEFAULT NULL,
  `family_name` varchar(100) NOT NULL,
  `financial_goals` text,
  `updated_at` datetime(6) DEFAULT NULL,
  `expenses_currency` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `financial_goals`
--

DROP TABLE IF EXISTS `financial_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `financial_goals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goal_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标名称',
  `goal_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: SAVING, INVESTMENT, DEBT_PAYOFF, RETIREMENT, PURCHASE, OTHER',
  `target_amount` decimal(18,2) NOT NULL COMMENT '目标金额',
  `current_amount` decimal(18,2) DEFAULT '0.00' COMMENT '当前金额',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY' COMMENT '货币',
  `target_date` date DEFAULT NULL COMMENT '目标日期',
  `priority` int DEFAULT '3' COMMENT '优先级 1-5',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IN_PROGRESS' COMMENT '状态: NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_user_date` (`user_id`,`target_date`),
  CONSTRAINT `financial_goals_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务目标表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goal_progress_records`
--

DROP TABLE IF EXISTS `goal_progress_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_progress_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `goal_id` bigint NOT NULL COMMENT '目标ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `amount` decimal(18,2) NOT NULL COMMENT '当期金额',
  `progress_percentage` decimal(5,2) DEFAULT NULL COMMENT '完成百分比',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goal_date` (`goal_id`,`record_date`),
  KEY `idx_goal_date` (`goal_id`,`record_date`),
  CONSTRAINT `goal_progress_records_ibfk_1` FOREIGN KEY (`goal_id`) REFERENCES `financial_goals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标进度记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liability_accounts`
--

DROP TABLE IF EXISTS `liability_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liability_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `category_id` bigint NOT NULL COMMENT '类别ID',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `account_number` varchar(100) DEFAULT NULL COMMENT '账号',
  `institution` varchar(100) DEFAULT NULL COMMENT '机构名称',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币类型',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '利率(%)',
  `original_amount` decimal(18,2) DEFAULT NULL COMMENT '原始借款金额',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '到期日期',
  `monthly_payment` decimal(18,2) DEFAULT NULL COMMENT '月供金额',
  `notes` text COMMENT '备注',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否活跃',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `idx_user_category` (`user_id`,`category_id`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  CONSTRAINT `liability_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `liability_accounts_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `liability_categories` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liability_categories`
--

DROP TABLE IF EXISTS `liability_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liability_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) NOT NULL COMMENT '类别名称',
  `type` varchar(50) NOT NULL COMMENT '类别类型: MORTGAGE, AUTO_LOAN, CREDIT_CARD, PERSONAL_LOAN, STUDENT_LOAN, OTHER',
  `description` text COMMENT '描述',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `color` varchar(20) DEFAULT NULL COMMENT '显示颜色',
  `display_order` int DEFAULT '0' COMMENT '显示顺序',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`,`name`),
  KEY `idx_user_type` (`user_id`,`type`),
  CONSTRAINT `liability_categories_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liability_records`
--

DROP TABLE IF EXISTS `liability_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liability_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `outstanding_balance` decimal(18,2) NOT NULL COMMENT '未偿余额',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '货币',
  `payment_amount` decimal(18,2) DEFAULT NULL COMMENT '本期还款金额',
  `principal_payment` decimal(18,2) DEFAULT NULL COMMENT '本金还款',
  `interest_payment` decimal(18,2) DEFAULT NULL COMMENT '利息还款',
  `notes` text COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_date` (`account_id`,`record_date`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_account_date` (`account_id`,`record_date`),
  CONSTRAINT `liability_records_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `liability_records_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `liability_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `net_asset_categories`
--

DROP TABLE IF EXISTS `net_asset_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `net_asset_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `color` varchar(20) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7h5bspx87836vsma5rlpxei1v` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `net_asset_category_asset_type_mappings`
--

DROP TABLE IF EXISTS `net_asset_category_asset_type_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `net_asset_category_asset_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_type` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `net_asset_category_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `net_asset_category_liability_type_mappings`
--

DROP TABLE IF EXISTS `net_asset_category_liability_type_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `net_asset_category_liability_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `liability_type` varchar(50) NOT NULL,
  `net_asset_category_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transaction_categories`
--

DROP TABLE IF EXISTS `transaction_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别名称',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: INCOME, EXPENSE',
  `parent_id` bigint DEFAULT NULL COMMENT '父类别ID',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`,`name`),
  KEY `parent_id` (`parent_id`),
  KEY `idx_user_type` (`user_id`,`type`),
  CONSTRAINT `transaction_categories_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `transaction_categories_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `transaction_categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_id` bigint DEFAULT NULL COMMENT '关联账户ID（资产账户）',
  `category_id` bigint DEFAULT NULL COMMENT '类别ID',
  `transaction_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: INCOME, EXPENSE, TRANSFER',
  `amount` decimal(18,2) NOT NULL COMMENT '金额',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY',
  `transaction_date` date NOT NULL COMMENT '交易日期',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件URL',
  `to_account_id` bigint DEFAULT NULL COMMENT '转账目标账户ID',
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（逗号分隔）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `to_account_id` (`to_account_id`),
  KEY `idx_user_date` (`user_id`,`transaction_date`),
  KEY `idx_user_type` (`user_id`,`transaction_type`),
  KEY `idx_account_date` (`account_id`,`transaction_date`),
  CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `transactions_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `asset_accounts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `transactions_ibfk_3` FOREIGN KEY (`category_id`) REFERENCES `transaction_categories` (`id`) ON DELETE SET NULL,
  CONSTRAINT `transactions_ibfk_4` FOREIGN KEY (`to_account_id`) REFERENCES `asset_accounts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preferences`
--

DROP TABLE IF EXISTS `user_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `base_currency` varchar(10) DEFAULT 'CNY' COMMENT '基准货币',
  `locale` varchar(10) DEFAULT 'zh_CN' COMMENT '语言区域',
  `timezone` varchar(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `theme` varchar(20) DEFAULT 'light' COMMENT '主题: light, dark',
  `date_format` varchar(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
  `number_format` varchar(20) DEFAULT '#,##0.00' COMMENT '数字格式',
  `fiscal_year_start_month` int DEFAULT '1' COMMENT '财年起始月份',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户偏好设置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_profiles`
--

DROP TABLE IF EXISTS `user_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `emergency_fund_months` int DEFAULT NULL,
  `estimated_annual_expenses` decimal(15,2) DEFAULT NULL,
  `notes` text,
  `risk_tolerance` enum('CONSERVATIVE','MODERATE','AGGRESSIVE') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_e5h89rk3ijvdmaiig4srogdc6` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `full_name` varchar(100) DEFAULT NULL COMMENT '全名',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_at` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  `age` int DEFAULT NULL,
  `annual_income` decimal(15,2) DEFAULT NULL,
  `family_id` bigint DEFAULT NULL,
  `notes` text,
  `risk_tolerance` enum('CONSERVATIVE','MODERATE','AGGRESSIVE') DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `income_currency` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_annual_financial_trend`
--

DROP TABLE IF EXISTS `v_annual_financial_trend`;
/*!50001 DROP VIEW IF EXISTS `v_annual_financial_trend`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_annual_financial_trend` AS SELECT 
 1 AS `family_id`,
 1 AS `year`,
 1 AS `summary_date`,
 1 AS `total_assets`,
 1 AS `total_liabilities`,
 1 AS `net_worth`,
 1 AS `asset_breakdown`,
 1 AS `liability_breakdown`,
 1 AS `currency`,
 1 AS `yoy_asset_change`,
 1 AS `yoy_liability_change`,
 1 AS `yoy_net_worth_change`,
 1 AS `yoy_asset_change_pct`,
 1 AS `yoy_liability_change_pct`,
 1 AS `yoy_net_worth_change_pct`,
 1 AS `mom_asset_change`,
 1 AS `mom_liability_change`,
 1 AS `mom_net_worth_change`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_annual_financial_trend`
--

/*!50001 DROP VIEW IF EXISTS `v_annual_financial_trend`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`austinxu`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_annual_financial_trend` AS select `annual_financial_summary`.`family_id` AS `family_id`,`annual_financial_summary`.`year` AS `year`,`annual_financial_summary`.`summary_date` AS `summary_date`,`annual_financial_summary`.`total_assets` AS `total_assets`,`annual_financial_summary`.`total_liabilities` AS `total_liabilities`,`annual_financial_summary`.`net_worth` AS `net_worth`,`annual_financial_summary`.`asset_breakdown` AS `asset_breakdown`,`annual_financial_summary`.`liability_breakdown` AS `liability_breakdown`,`annual_financial_summary`.`currency` AS `currency`,`annual_financial_summary`.`yoy_asset_change` AS `yoy_asset_change`,`annual_financial_summary`.`yoy_liability_change` AS `yoy_liability_change`,`annual_financial_summary`.`yoy_net_worth_change` AS `yoy_net_worth_change`,`annual_financial_summary`.`yoy_asset_change_pct` AS `yoy_asset_change_pct`,`annual_financial_summary`.`yoy_liability_change_pct` AS `yoy_liability_change_pct`,`annual_financial_summary`.`yoy_net_worth_change_pct` AS `yoy_net_worth_change_pct`,(`annual_financial_summary`.`total_assets` - lag(`annual_financial_summary`.`total_assets`) OVER (PARTITION BY `annual_financial_summary`.`family_id` ORDER BY `annual_financial_summary`.`year` ) ) AS `mom_asset_change`,(`annual_financial_summary`.`total_liabilities` - lag(`annual_financial_summary`.`total_liabilities`) OVER (PARTITION BY `annual_financial_summary`.`family_id` ORDER BY `annual_financial_summary`.`year` ) ) AS `mom_liability_change`,(`annual_financial_summary`.`net_worth` - lag(`annual_financial_summary`.`net_worth`) OVER (PARTITION BY `annual_financial_summary`.`family_id` ORDER BY `annual_financial_summary`.`year` ) ) AS `mom_net_worth_change` from `annual_financial_summary` order by `annual_financial_summary`.`family_id`,`annual_financial_summary`.`year` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-10 12:24:48
