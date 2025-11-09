-- 个人理财管理系统数据库设计
-- Database: finance
-- Character Set: utf8mb4

-- 创建数据库
CREATE DATABASE IF NOT EXISTS finance
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE finance;

-- ============================================
-- 用户管理
-- ============================================

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    full_name VARCHAR(100) COMMENT '全名',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 资产管理
-- ============================================

-- 资产类别表
CREATE TABLE asset_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '类别名称',
    type VARCHAR(50) NOT NULL COMMENT '类别类型: CASH, STOCKS, RETIREMENT_FUND, INSURANCE, REAL_ESTATE, CRYPTOCURRENCY',
    description TEXT COMMENT '描述',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '显示颜色',
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预设',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_type (user_id, type),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产类别表';

-- 资产账户表
CREATE TABLE asset_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_id BIGINT NOT NULL COMMENT '类别ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    account_number VARCHAR(100) COMMENT '账号/编号',
    institution VARCHAR(100) COMMENT '机构名称 (银行/券商等)',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币类型',
    notes TEXT COMMENT '备注',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES asset_categories(id) ON DELETE RESTRICT,
    INDEX idx_user_category (user_id, category_id),
    INDEX idx_user_active (user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产账户表';

-- 资产记录表（时间序列数据）
CREATE TABLE asset_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    amount DECIMAL(18, 2) NOT NULL COMMENT '金额',
    quantity DECIMAL(18, 6) COMMENT '数量（股票/基金份额等）',
    unit_price DECIMAL(18, 6) COMMENT '单价',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    exchange_rate DECIMAL(12, 6) DEFAULT 1.0 COMMENT '汇率（转换为基准货币）',
    amount_in_base_currency DECIMAL(18, 2) COMMENT '基准货币金额',
    notes TEXT COMMENT '备注',
    attachment_url VARCHAR(500) COMMENT '附件URL（账单截图等）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES asset_accounts(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, record_date),
    INDEX idx_account_date (account_id, record_date),
    UNIQUE KEY uk_account_date (account_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产记录表';

-- ============================================
-- 负债管理
-- ============================================

-- 负债类别表
CREATE TABLE liability_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '类别名称',
    type VARCHAR(50) NOT NULL COMMENT '类别类型: MORTGAGE, AUTO_LOAN, CREDIT_CARD, PERSONAL_LOAN, STUDENT_LOAN, OTHER',
    description TEXT COMMENT '描述',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '显示颜色',
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预设',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_type (user_id, type),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='负债类别表';

-- 负债账户表
CREATE TABLE liability_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_id BIGINT NOT NULL COMMENT '类别ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    account_number VARCHAR(100) COMMENT '账号',
    institution VARCHAR(100) COMMENT '机构名称',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币类型',
    interest_rate DECIMAL(5, 2) COMMENT '利率(%)',
    original_amount DECIMAL(18, 2) COMMENT '原始借款金额',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '到期日期',
    monthly_payment DECIMAL(18, 2) COMMENT '月供金额',
    notes TEXT COMMENT '备注',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES liability_categories(id) ON DELETE RESTRICT,
    INDEX idx_user_category (user_id, category_id),
    INDEX idx_user_active (user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='负债账户表';

-- 负债记录表（时间序列数据）
CREATE TABLE liability_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    outstanding_balance DECIMAL(18, 2) NOT NULL COMMENT '未偿余额',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    exchange_rate DECIMAL(12, 6) DEFAULT 1.0 COMMENT '汇率',
    balance_in_base_currency DECIMAL(18, 2) COMMENT '基准货币余额',
    payment_amount DECIMAL(18, 2) COMMENT '本期还款金额',
    principal_payment DECIMAL(18, 2) COMMENT '本金还款',
    interest_payment DECIMAL(18, 2) COMMENT '利息还款',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES liability_accounts(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, record_date),
    INDEX idx_account_date (account_id, record_date),
    UNIQUE KEY uk_account_date (account_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='负债记录表';

-- ============================================
-- 财务目标管理
-- ============================================

-- 财务目标表
CREATE TABLE financial_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    goal_name VARCHAR(100) NOT NULL COMMENT '目标名称',
    goal_type VARCHAR(50) NOT NULL COMMENT '目标类型: SAVING, INVESTMENT, DEBT_PAYOFF, RETIREMENT, PURCHASE, OTHER',
    target_amount DECIMAL(18, 2) NOT NULL COMMENT '目标金额',
    current_amount DECIMAL(18, 2) DEFAULT 0 COMMENT '当前金额',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    target_date DATE COMMENT '目标日期',
    priority INT DEFAULT 3 COMMENT '优先级 1-5',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '状态: NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED',
    description TEXT COMMENT '描述',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_date (user_id, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务目标表';

-- 目标进度记录表
CREATE TABLE goal_progress_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL COMMENT '目标ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    amount DECIMAL(18, 2) NOT NULL COMMENT '当期金额',
    progress_percentage DECIMAL(5, 2) COMMENT '完成百分比',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (goal_id) REFERENCES financial_goals(id) ON DELETE CASCADE,
    INDEX idx_goal_date (goal_id, record_date),
    UNIQUE KEY uk_goal_date (goal_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标进度记录表';

-- ============================================
-- 预算管理（可选）
-- ============================================

-- 预算表
CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    budget_name VARCHAR(100) NOT NULL COMMENT '预算名称',
    category VARCHAR(50) COMMENT '分类',
    amount DECIMAL(18, 2) NOT NULL COMMENT '预算金额',
    period VARCHAR(20) NOT NULL COMMENT '周期: MONTHLY, QUARTERLY, YEARLY',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    currency VARCHAR(10) DEFAULT 'CNY',
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_period (user_id, period, is_active),
    INDEX idx_user_dates (user_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';

-- ============================================
-- 交易记录（收入支出明细）
-- ============================================

-- 交易类别表
CREATE TABLE transaction_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '类别名称',
    type VARCHAR(20) NOT NULL COMMENT '类型: INCOME, EXPENSE',
    parent_id BIGINT COMMENT '父类别ID',
    icon VARCHAR(50),
    color VARCHAR(20),
    display_order INT DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES transaction_categories(id) ON DELETE SET NULL,
    INDEX idx_user_type (user_id, type),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易类别表';

-- 交易记录表
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_id BIGINT COMMENT '关联账户ID（资产账户）',
    category_id BIGINT COMMENT '类别ID',
    transaction_type VARCHAR(20) NOT NULL COMMENT '类型: INCOME, EXPENSE, TRANSFER',
    amount DECIMAL(18, 2) NOT NULL COMMENT '金额',
    currency VARCHAR(10) DEFAULT 'CNY',
    transaction_date DATE NOT NULL COMMENT '交易日期',
    description TEXT COMMENT '描述',
    notes TEXT COMMENT '备注',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    to_account_id BIGINT COMMENT '转账目标账户ID',
    tags VARCHAR(500) COMMENT '标签（逗号分隔）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES asset_accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES transaction_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (to_account_id) REFERENCES asset_accounts(id) ON DELETE SET NULL,
    INDEX idx_user_date (user_id, transaction_date),
    INDEX idx_user_type (user_id, transaction_type),
    INDEX idx_account_date (account_id, transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';

-- ============================================
-- 系统配置和元数据
-- ============================================

-- 用户偏好设置
CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    base_currency VARCHAR(10) DEFAULT 'CNY' COMMENT '基准货币',
    locale VARCHAR(10) DEFAULT 'zh_CN' COMMENT '语言区域',
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    theme VARCHAR(20) DEFAULT 'light' COMMENT '主题: light, dark',
    date_format VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
    number_format VARCHAR(20) DEFAULT '#,##0.00' COMMENT '数字格式',
    fiscal_year_start_month INT DEFAULT 1 COMMENT '财年起始月份',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置';

-- 汇率表（用于多货币支持）
CREATE TABLE exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(10) NOT NULL COMMENT '源货币',
    to_currency VARCHAR(10) NOT NULL COMMENT '目标货币',
    rate DECIMAL(12, 6) NOT NULL COMMENT '汇率',
    rate_date DATE NOT NULL COMMENT '汇率日期',
    source VARCHAR(50) COMMENT '数据源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_currencies_date (from_currency, to_currency, rate_date),
    UNIQUE KEY uk_currencies_date (from_currency, to_currency, rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='汇率表';

-- ============================================
-- 创建视图（便于查询）
-- ============================================

-- 最新资产视图
CREATE OR REPLACE VIEW v_latest_asset_values AS
SELECT
    a.id AS account_id,
    a.user_id,
    a.category_id,
    c.name AS category_name,
    c.type AS category_type,
    a.account_name,
    a.institution,
    r.record_date AS latest_date,
    r.amount,
    r.amount_in_base_currency,
    r.currency
FROM asset_accounts a
INNER JOIN asset_categories c ON a.category_id = c.id
LEFT JOIN asset_records r ON a.id = r.account_id
WHERE r.record_date = (
    SELECT MAX(record_date)
    FROM asset_records
    WHERE account_id = a.id
)
AND a.is_active = TRUE;

-- 最新负债视图
CREATE OR REPLACE VIEW v_latest_liability_values AS
SELECT
    l.id AS account_id,
    l.user_id,
    l.category_id,
    c.name AS category_name,
    c.type AS category_type,
    l.account_name,
    l.institution,
    l.interest_rate,
    r.record_date AS latest_date,
    r.outstanding_balance,
    r.balance_in_base_currency,
    r.currency
FROM liability_accounts l
INNER JOIN liability_categories c ON l.category_id = c.id
LEFT JOIN liability_records r ON l.id = r.account_id
WHERE r.record_date = (
    SELECT MAX(record_date)
    FROM liability_records
    WHERE account_id = l.id
)
AND l.is_active = TRUE;

-- 用户净资产视图
CREATE OR REPLACE VIEW v_user_net_worth AS
SELECT
    u.id AS user_id,
    u.username,
    COALESCE(SUM(a.amount_in_base_currency), 0) AS total_assets,
    COALESCE(SUM(l.balance_in_base_currency), 0) AS total_liabilities,
    COALESCE(SUM(a.amount_in_base_currency), 0) - COALESCE(SUM(l.balance_in_base_currency), 0) AS net_worth,
    up.base_currency
FROM users u
LEFT JOIN v_latest_asset_values a ON u.id = a.user_id
LEFT JOIN v_latest_liability_values l ON u.id = l.user_id
LEFT JOIN user_preferences up ON u.id = up.user_id
WHERE u.is_active = TRUE
GROUP BY u.id, u.username, up.base_currency;
