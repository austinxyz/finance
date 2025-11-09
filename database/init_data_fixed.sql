-- 初始化数据脚本（修正版）
-- 先创建用户，再创建依赖用户的数据

USE finance;

-- ============================================
-- 创建测试用户（必须先创建）
-- ============================================

-- 创建测试用户
-- 密码: password123 (实际应用中需要使用BCrypt等加密)
INSERT INTO users (username, email, password_hash, full_name, is_active) VALUES
('demo_user', 'demo@example.com', '$2a$10$example_hash_here', '示例用户', TRUE);

-- 为测试用户创建偏好设置
INSERT INTO user_preferences (user_id, base_currency, locale, timezone, theme)
SELECT id, 'CNY', 'zh_CN', 'Asia/Shanghai', 'light'
FROM users WHERE username = 'demo_user';

-- ============================================
-- 系统预设资产类别
-- ============================================

INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
SELECT u.id, '现金', 'CASH', '银行存款、手头现金、货币基金', '💵', '#10B981', 1, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '银行存款', 'CASH', '储蓄账户、活期存款', '🏦', '#10B981', 2, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '货币基金', 'CASH', '余额宝、理财通等货币基金', '💰', '#10B981', 3, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '股票', 'STOCKS', '国内外股票、基金', '📈', '#3B82F6', 4, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '国内股票', 'STOCKS', 'A股市场股票', '🇨🇳', '#3B82F6', 5, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '海外股票', 'STOCKS', '美股、港股等', '🌍', '#3B82F6', 6, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '基金', 'STOCKS', '股票基金、混合基金等', '📊', '#3B82F6', 7, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '退休基金', 'RETIREMENT_FUND', '养老保险、401k、IRA等', '🎯', '#8B5CF6', 8, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '401k', 'RETIREMENT_FUND', '美国401k退休账户', '🇺🇸', '#8B5CF6', 9, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, 'IRA', 'RETIREMENT_FUND', '个人退休账户', '🏦', '#8B5CF6', 10, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '养老保险', 'RETIREMENT_FUND', '社保、商业养老保险', '🛡️', '#8B5CF6', 11, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '保险', 'INSURANCE', '人寿保险、年金保险现金价值', '🛡️', '#F59E0B', 12, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '人寿保险', 'INSURANCE', '人寿保险现金价值', '👨‍👩‍👧', '#F59E0B', 13, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '年金保险', 'INSURANCE', '年金保险账户', '💍', '#F59E0B', 14, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '房产', 'REAL_ESTATE', '自住房产、投资房产', '🏠', '#EF4444', 15, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '自住房产', 'REAL_ESTATE', '主要居住房产', '🏡', '#EF4444', 16, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '投资房产', 'REAL_ESTATE', '出租或投资房产', '🏢', '#EF4444', 17, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '数字货币', 'CRYPTOCURRENCY', '比特币、以太坊等', '₿', '#F97316', 18, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '比特币', 'CRYPTOCURRENCY', 'Bitcoin', '₿', '#F97316', 19, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '以太坊', 'CRYPTOCURRENCY', 'Ethereum', 'Ξ', '#F97316', 20, TRUE FROM users u WHERE u.username = 'demo_user';

-- ============================================
-- 系统预设负债类别
-- ============================================

INSERT INTO liability_categories (user_id, name, type, description, icon, color, display_order, is_system)
SELECT u.id, '房贷', 'MORTGAGE', '住房抵押贷款', '🏠', '#EF4444', 1, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '车贷', 'AUTO_LOAN', '汽车贷款', '🚗', '#F59E0B', 2, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '信用卡', 'CREDIT_CARD', '信用卡负债', '💳', '#EC4899', 3, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '个人贷款', 'PERSONAL_LOAN', '个人消费贷款', '💰', '#8B5CF6', 4, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '学生贷款', 'STUDENT_LOAN', '教育贷款', '🎓', '#3B82F6', 5, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '其他负债', 'OTHER', '其他类型负债', '📝', '#6B7280', 6, TRUE FROM users u WHERE u.username = 'demo_user';

-- ============================================
-- 系统预设交易类别
-- ============================================

-- 收入类别
INSERT INTO transaction_categories (user_id, name, type, parent_id, icon, color, display_order, is_system)
SELECT u.id, '工资收入', 'INCOME', NULL, '💼', '#10B981', 1, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '奖金', 'INCOME', NULL, '🎁', '#10B981', 2, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '投资收益', 'INCOME', NULL, '📈', '#10B981', 3, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '租金收入', 'INCOME', NULL, '🏠', '#10B981', 6, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '副业收入', 'INCOME', NULL, '💡', '#10B981', 7, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '其他收入', 'INCOME', NULL, '📝', '#10B981', 8, TRUE FROM users u WHERE u.username = 'demo_user';

-- 支出类别（不包含父子关系，简化版）
INSERT INTO transaction_categories (user_id, name, type, parent_id, icon, color, display_order, is_system)
SELECT u.id, '餐饮', 'EXPENSE', NULL, '🍽️', '#EF4444', 1, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '购物', 'EXPENSE', NULL, '🛍️', '#EF4444', 2, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '交通', 'EXPENSE', NULL, '🚗', '#EF4444', 3, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '住房', 'EXPENSE', NULL, '🏠', '#EF4444', 4, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '医疗健康', 'EXPENSE', NULL, '🏥', '#EF4444', 9, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '教育培训', 'EXPENSE', NULL, '📚', '#EF4444', 10, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '娱乐休闲', 'EXPENSE', NULL, '🎮', '#EF4444', 11, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '保险', 'EXPENSE', NULL, '🛡️', '#EF4444', 12, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '通讯', 'EXPENSE', NULL, '📱', '#EF4444', 13, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '还款', 'EXPENSE', NULL, '💳', '#EF4444', 14, TRUE FROM users u WHERE u.username = 'demo_user'
UNION ALL
SELECT u.id, '其他支出', 'EXPENSE', NULL, '📝', '#EF4444', 15, TRUE FROM users u WHERE u.username = 'demo_user';

-- ============================================
-- 常用汇率初始化
-- ============================================

INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) VALUES
('CNY', 'CNY', 1.000000, CURDATE(), 'system'),
('USD', 'CNY', 7.200000, CURDATE(), 'system'),
('EUR', 'CNY', 7.800000, CURDATE(), 'system'),
('JPY', 'CNY', 0.048000, CURDATE(), 'system'),
('GBP', 'CNY', 9.100000, CURDATE(), 'system'),
('HKD', 'CNY', 0.920000, CURDATE(), 'system'),
('AUD', 'CNY', 4.750000, CURDATE(), 'system'),
('CAD', 'CNY', 5.300000, CURDATE(), 'system');
