-- ============================================================
-- 家庭理财管理系统 - 数据库表结构
-- ============================================================
-- 技术栈：MySQL 8.0 + Spring Boot 3.2 + JPA
--
-- 数据模型说明：
-- 1. 时序数据模式：账户使用基于记录的时序方法
-- 2. 外键约束：确保数据完整性
-- 3. 索引优化：常用查询字段建立索引
--
-- 生成时间: $(date '+%Y-%m-%d %H:%M:%S')
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE `annual_expense_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `summary_year` int NOT NULL COMMENT '汇总年份',
  `major_category_id` bigint NOT NULL COMMENT '大类ID (0表示总计)',
  `minor_category_id` bigint DEFAULT NULL COMMENT '小类ID (NULL表示大类汇总)',
  `base_expense_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '基础支出(expense_records汇总)',
  `special_expense_amount` decimal(18,2) DEFAULT '0.00',
  `special_expense_details` json DEFAULT NULL,
  `asset_adjustment` decimal(18,2) DEFAULT '0.00' COMMENT '资产调整额 (资产增加为正, 减少为负)',
  `liability_adjustment` decimal(18,2) DEFAULT '0.00' COMMENT '负债调整额 (负债减少为正, 增加为负)',
  `adjustment_details` json DEFAULT NULL COMMENT '调整详情 [{type, code, amount, direction}]',
  `actual_expense_amount` decimal(18,2) NOT NULL COMMENT '实际年度支出',
  `currency` varchar(10) DEFAULT 'CNY',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary` (`family_id`,`summary_year`,`major_category_id`,`minor_category_id`),
  UNIQUE KEY `uk_year_category` (`family_id`,`summary_year`,`major_category_id`,`minor_category_id`),
  KEY `idx_family_year` (`family_id`,`summary_year`),
  KEY `idx_major_category` (`major_category_id`),
  KEY `FKa90vvqugnalabpwnnm7lxeyhv` (`minor_category_id`),
  KEY `FKpcpx3osphoy7ddoix5gr1w2wx` (`user_id`),
  CONSTRAINT `FKa90vvqugnalabpwnnm7lxeyhv` FOREIGN KEY (`minor_category_id`) REFERENCES `expense_categories_minor` (`id`),
  CONSTRAINT `FKpcpx3osphoy7ddoix5gr1w2wx` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq9pso6cftpfcu93817e5lmbt` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度支出汇总表 (通用可配置版本)';
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度财务摘要表';
CREATE TABLE `asset_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `asset_type_id` bigint DEFAULT NULL COMMENT '资产大类ID，引用asset_type表',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `account_number` varchar(100) DEFAULT NULL COMMENT '账号/编号',
  `institution` varchar(100) DEFAULT NULL COMMENT '机构名称 (银行/券商等)',
  `currency` varchar(10) DEFAULT 'USD',
  `notes` text COMMENT '备注',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否活跃',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tax_status` enum('TAXABLE','TAX_FREE','TAX_DEFERRED') DEFAULT NULL,
  `is_primary_residence` bit(1) DEFAULT NULL,
  `linked_liability_account_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_category` (`user_id`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  KEY `idx_asset_type_id` (`asset_type_id`),
  KEY `FKj7mfyj7r2qas62ibdt715gkr` (`linked_liability_account_id`),
  CONSTRAINT `asset_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKj7mfyj7r2qas62ibdt715gkr` FOREIGN KEY (`linked_liability_account_id`) REFERENCES `liability_accounts` (`id`),
  CONSTRAINT `FKlsfoe2fmtq95j3guuljpp09rx` FOREIGN KEY (`asset_type_id`) REFERENCES `asset_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产账户表';
CREATE TABLE `asset_liability_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_type` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `liability_type` varchar(50) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `asset_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `amount` decimal(18,2) NOT NULL COMMENT '金额',
  `quantity` decimal(18,6) DEFAULT NULL COMMENT '数量（股票/基金份额等）',
  `unit_price` decimal(18,6) DEFAULT NULL COMMENT '单价',
  `currency` varchar(10) DEFAULT 'USD',
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产记录表';
CREATE TABLE `asset_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chinese_name` varchar(100) NOT NULL,
  `color` varchar(20) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `display_order` int NOT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `is_investment` bit(1) NOT NULL,
  `name` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_p5n9lu0p3b85c122x2xet49dq` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `expense_budgets` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `budget_year` int NOT NULL COMMENT '预算年份（如2025）',
  `minor_category_id` bigint NOT NULL COMMENT '子分类ID',
  `budget_amount` decimal(15,2) NOT NULL COMMENT '预算金额',
  `currency` varchar(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型（USD, CNY等）',
  `notes` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_year_category_currency` (`family_id`,`budget_year`,`minor_category_id`,`currency`),
  KEY `idx_family_year` (`family_id`,`budget_year`),
  KEY `idx_minor_category` (`minor_category_id`),
  CONSTRAINT `fk_budget_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `fk_budget_minor_category` FOREIGN KEY (`minor_category_id`) REFERENCES `expense_categories_minor` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出年度预算表';
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出大类表';
CREATE TABLE `expense_categories_minor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `major_category_id` bigint NOT NULL COMMENT '所属大类ID',
  `name` varchar(100) NOT NULL COMMENT '子分类名称',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统默认分类',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序',
  `description` text COMMENT '说明',
  `expense_type` varchar(20) NOT NULL DEFAULT 'FIXED_DAILY' COMMENT '默认支出类型（FIXED_DAILY/LARGE_IRREGULAR）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_name` (`major_category_id`,`name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_major_category` (`major_category_id`),
  CONSTRAINT `expense_categories_minor_ibfk_1` FOREIGN KEY (`major_category_id`) REFERENCES `expense_categories_major` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出子分类表';
CREATE TABLE `expense_category_adjustment_config` (
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出类别调整配置表';
CREATE TABLE `expense_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `user_id` bigint DEFAULT NULL COMMENT '记录人ID（可选）',
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
  UNIQUE KEY `uk_family_period_category_currency` (`family_id`,`expense_period`,`minor_category_id`,`currency`),
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出记录表';
CREATE TABLE `families` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `annual_expenses` decimal(15,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `emergency_fund_months` int DEFAULT NULL,
  `family_name` varchar(100) NOT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `financial_goals` text,
  `updated_at` datetime(6) DEFAULT NULL,
  `expenses_currency` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `investment_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '资产账户ID',
  `transaction_period` varchar(7) NOT NULL COMMENT '交易期间(YYYY-MM格式，如2024-01)',
  `transaction_type` enum('DEPOSIT','WITHDRAWAL') NOT NULL COMMENT '交易类型：DEPOSIT-投入(买入/追加投资), WITHDRAWAL-取出(卖出/提取资金)',
  `amount` decimal(15,2) NOT NULL COMMENT '交易金额（正数，方向由transaction_type决定）',
  `description` varchar(500) DEFAULT NULL COMMENT '交易说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_period_type` (`account_id`,`transaction_period`,`transaction_type`),
  KEY `idx_account_period` (`account_id`,`transaction_period`),
  KEY `idx_period` (`transaction_period`),
  KEY `idx_transaction_type` (`transaction_type`),
  CONSTRAINT `investment_transactions_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `asset_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投资交易记录表';
CREATE TABLE `liability_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `liability_type_id` bigint DEFAULT NULL COMMENT '负债类型ID（关联 liability_type 表）',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `account_number` varchar(100) DEFAULT NULL COMMENT '账号',
  `institution` varchar(100) DEFAULT NULL COMMENT '机构名称',
  `currency` varchar(10) DEFAULT 'USD',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '利率(%)',
  `original_amount` decimal(18,2) DEFAULT NULL COMMENT '原始借款金额',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '到期日期',
  `monthly_payment` decimal(18,2) DEFAULT NULL COMMENT '月供金额',
  `notes` text COMMENT '备注',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否活跃',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `linked_asset_account_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_category` (`user_id`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  KEY `fk_liability_accounts_liability_type` (`liability_type_id`),
  KEY `FK836kbt4vlm2dti8p6uynx9mnw` (`linked_asset_account_id`),
  CONSTRAINT `FK836kbt4vlm2dti8p6uynx9mnw` FOREIGN KEY (`linked_asset_account_id`) REFERENCES `asset_accounts` (`id`),
  CONSTRAINT `fk_liability_accounts_liability_type` FOREIGN KEY (`liability_type_id`) REFERENCES `liability_type` (`id`),
  CONSTRAINT `liability_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债账户表';
CREATE TABLE `liability_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `outstanding_balance` decimal(18,2) NOT NULL COMMENT '未偿余额',
  `currency` varchar(10) DEFAULT 'USD',
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债记录表';
CREATE TABLE `liability_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL COMMENT '负债类型代码 (MORTGAGE, AUTO_LOAN, CREDIT_CARD, etc.)',
  `chinese_name` varchar(50) NOT NULL COMMENT '中文名称',
  `english_name` varchar(50) NOT NULL COMMENT '英文名称',
  `description` text COMMENT '类型描述',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `color` varchar(20) DEFAULT NULL COMMENT '颜色代码',
  `display_order` int DEFAULT '0' COMMENT '显示顺序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='负债类型表';
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `net_asset_category_asset_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_type` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `net_asset_category_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `net_asset_category_liability_type_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `liability_type` varchar(50) NOT NULL,
  `net_asset_category_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `property_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_account_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(10) NOT NULL,
  `down_payment` decimal(18,2) NOT NULL,
  `mortgage_amount` decimal(18,2) NOT NULL,
  `notes` text,
  `property_value` decimal(18,2) NOT NULL,
  `purchase_date` date NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cchqyjkt7e7iij221vq64aiw8` (`asset_account_id`),
  CONSTRAINT `FK1gxy56fcojjnuco2enjkgvrvr` FOREIGN KEY (`asset_account_id`) REFERENCES `asset_accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易类别表';
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户偏好设置';
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
SET @saved_cs_client     = @@character_set_client;
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

SET FOREIGN_KEY_CHECKS = 1;
