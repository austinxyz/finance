-- V16__create_income_tables.sql
-- 创建收入管理相关表

-- 收入大类表
CREATE TABLE income_categories_major (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '英文名称',
    chinese_name VARCHAR(100) NOT NULL COMMENT '中文名称',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收入大类表';

-- 收入小类表
CREATE TABLE income_categories_minor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    major_category_id BIGINT NOT NULL COMMENT '所属大类ID',
    name VARCHAR(100) NOT NULL COMMENT '英文名称',
    chinese_name VARCHAR(100) NOT NULL COMMENT '中文名称',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    UNIQUE KEY uk_major_name (major_category_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收入小类表';

-- 收入记录表（月度）
CREATE TABLE income_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    asset_account_id BIGINT COMMENT '关联的资产账户ID',
    major_category_id BIGINT NOT NULL COMMENT '收入大类ID',
    minor_category_id BIGINT COMMENT '收入小类ID',
    period VARCHAR(7) NOT NULL COMMENT '周期（YYYY-MM）',
    amount DECIMAL(18, 2) NOT NULL COMMENT '金额（税后实际到账）',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '币种',
    amount_usd DECIMAL(18, 2) COMMENT '换算成USD的金额',
    description TEXT COMMENT '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (asset_account_id) REFERENCES asset_accounts(id),
    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    FOREIGN KEY (minor_category_id) REFERENCES income_categories_minor(id),
    UNIQUE KEY uk_income (family_id, user_id, period, major_category_id, minor_category_id, currency),
    INDEX idx_family_period (family_id, period),
    INDEX idx_user_period (user_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收入记录表';

-- 年度收入预算表
CREATE TABLE income_budgets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT COMMENT '用户ID（NULL表示全家庭）',
    major_category_id BIGINT NOT NULL COMMENT '收入大类ID',
    minor_category_id BIGINT COMMENT '收入小类ID',
    year INT NOT NULL COMMENT '年份',
    budgeted_amount DECIMAL(18, 2) NOT NULL COMMENT '预算金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '币种',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    FOREIGN KEY (minor_category_id) REFERENCES income_categories_minor(id),
    UNIQUE KEY uk_budget (family_id, user_id, major_category_id, minor_category_id, year, currency),
    INDEX idx_family_year (family_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度收入预算表';

-- 插入收入大类初始数据
INSERT INTO income_categories_major (name, chinese_name, icon, color, display_order) VALUES
('Salary', '工资', '💼', '#3b82f6', 1),
('Bonus', '奖金', '🎁', '#8b5cf6', 2),
('Investment', '投资收益', '📈', '#10b981', 3),
('Rental', '租金', '🏠', '#f59e0b', 4),
('SideHustle', '副业', '💡', '#06b6d4', 5),
('RSU', '股票RSU', '📊', '#6366f1', 6),
('Retirement', '退休基金贡献', '🏦', '#14b8a6', 7),
('TaxRefund', '退税', '💰', '#22c55e', 8),
('Gift', '礼金', '🎀', '#ec4899', 9),
('Other', '其他', '📦', '#6b7280', 10);

-- 插入收入小类初始数据
-- 工资小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Salary'), 'BaseSalary', '基本工资'),
((SELECT id FROM income_categories_major WHERE name = 'Salary'), 'Overtime', '加班费'),
((SELECT id FROM income_categories_major WHERE name = 'Salary'), 'Commission', '提成');

-- 奖金小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Bonus'), 'YearEndBonus', '年终奖'),
((SELECT id FROM income_categories_major WHERE name = 'Bonus'), 'PerformanceBonus', '绩效奖金'),
((SELECT id FROM income_categories_major WHERE name = 'Bonus'), 'SigningBonus', '签约奖金');

-- 投资收益小类（自动汇总，不手动录入）
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Investment'), 'StockGain', '股票收益'),
((SELECT id FROM income_categories_major WHERE name = 'Investment'), 'Dividend', '分红'),
((SELECT id FROM income_categories_major WHERE name = 'Investment'), 'Interest', '利息'),
((SELECT id FROM income_categories_major WHERE name = 'Investment'), 'CryptoGain', '数字货币收益');

-- 租金小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Rental'), 'ResidentialRent', '住宅租金'),
((SELECT id FROM income_categories_major WHERE name = 'Rental'), 'CommercialRent', '商业租金');

-- 副业小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'SideHustle'), 'Freelance', '自由职业'),
((SELECT id FROM income_categories_major WHERE name = 'SideHustle'), 'Consulting', '咨询'),
((SELECT id FROM income_categories_major WHERE name = 'SideHustle'), 'OnlineBusiness', '线上业务');

-- RSU小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'RSU'), 'VestedRSU', 'Vested股票'),
((SELECT id FROM income_categories_major WHERE name = 'RSU'), 'ESPP', '员工购股计划');

-- 退休基金小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Retirement'), 'EmployerMatch', '雇主匹配'),
((SELECT id FROM income_categories_major WHERE name = 'Retirement'), 'EmployerContribution', '雇主贡献'),
((SELECT id FROM income_categories_major WHERE name = 'Retirement'), 'SelfContribution', '个人贡献');

-- 退税小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'TaxRefund'), 'FederalRefund', '联邦退税'),
((SELECT id FROM income_categories_major WHERE name = 'TaxRefund'), 'StateRefund', '州退税');

-- 礼金小类
INSERT INTO income_categories_minor (major_category_id, name, chinese_name) VALUES
((SELECT id FROM income_categories_major WHERE name = 'Gift'), 'WeddingGift', '婚礼礼金'),
((SELECT id FROM income_categories_major WHERE name = 'Gift'), 'BirthdayGift', '生日礼金'),
((SELECT id FROM income_categories_major WHERE name = 'Gift'), 'HolidayGift', '节日礼金');
