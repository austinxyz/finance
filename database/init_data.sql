-- 初始化数据脚本
-- 包含系统预设的资产/负债类别和交易类别

USE finance;

-- ============================================
-- 系统预设资产类别（针对用户ID = 1，实际使用时需要为每个用户创建）
-- ============================================

-- 注意：这些是示例数据，实际应用中应该在用户注册时自动创建
-- 或者设计为全局系统类别，通过is_system标记

INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system) VALUES
(1, '现金', 'CASH', '银行存款、手头现金、货币基金', '💵', '#10B981', 1, TRUE),
(1, '银行存款', 'CASH', '储蓄账户、活期存款', '🏦', '#10B981', 2, TRUE),
(1, '货币基金', 'CASH', '余额宝、理财通等货币基金', '💰', '#10B981', 3, TRUE),
(1, '证券账号', 'CASH', '证券商开户账号中的现金余额', '📊', '#10B981', 4, TRUE),
(1, '股票', 'STOCKS', '国内外股票、基金', '📈', '#3B82F6', 5, TRUE),
(1, '国内股票', 'STOCKS', 'A股市场股票', '🇨🇳', '#3B82F6', 6, TRUE),
(1, '海外股票', 'STOCKS', '美股、港股等', '🌍', '#3B82F6', 7, TRUE),
(1, '基金', 'STOCKS', '股票基金、混合基金等', '📊', '#3B82F6', 8, TRUE),
(1, '退休基金', 'RETIREMENT_FUND', '养老保险、401k、IRA等', '🎯', '#8B5CF6', 9, TRUE),
(1, '401k', 'RETIREMENT_FUND', '美国401k退休账户', '🇺🇸', '#8B5CF6', 10, TRUE),
(1, 'IRA', 'RETIREMENT_FUND', '个人退休账户', '🏦', '#8B5CF6', 11, TRUE),
(1, '养老保险', 'RETIREMENT_FUND', '社保、商业养老保险', '🛡️', '#8B5CF6', 12, TRUE),
(1, '保险', 'INSURANCE', '人寿保险、年金保险现金价值', '🛡️', '#F59E0B', 13, TRUE),
(1, '人寿保险', 'INSURANCE', '人寿保险现金价值', '👨‍👩‍👧', '#F59E0B', 14, TRUE),
(1, '年金保险', 'INSURANCE', '年金保险账户', '💍', '#F59E0B', 15, TRUE),
(1, '房产', 'REAL_ESTATE', '自住房产、投资房产', '🏠', '#EF4444', 16, TRUE),
(1, '自住房产', 'REAL_ESTATE', '主要居住房产', '🏡', '#EF4444', 17, TRUE),
(1, '投资房产', 'REAL_ESTATE', '出租或投资房产', '🏢', '#EF4444', 18, TRUE),
(1, '数字货币', 'CRYPTOCURRENCY', '比特币、以太坊等', '₿', '#F97316', 19, TRUE),
(1, '比特币', 'CRYPTOCURRENCY', 'Bitcoin', '₿', '#F97316', 20, TRUE),
(1, '以太坊', 'CRYPTOCURRENCY', 'Ethereum', 'Ξ', '#F97316', 21, TRUE);

-- ============================================
-- 系统预设负债类别
-- ============================================

INSERT INTO liability_categories (user_id, name, type, description, icon, color, display_order, is_system) VALUES
(1, '房贷', 'MORTGAGE', '住房抵押贷款', '🏠', '#EF4444', 1, TRUE),
(1, '车贷', 'AUTO_LOAN', '汽车贷款', '🚗', '#F59E0B', 2, TRUE),
(1, '信用卡', 'CREDIT_CARD', '信用卡负债', '💳', '#EC4899', 3, TRUE),
(1, '个人贷款', 'PERSONAL_LOAN', '个人消费贷款', '💰', '#8B5CF6', 4, TRUE),
(1, '学生贷款', 'STUDENT_LOAN', '教育贷款', '🎓', '#3B82F6', 5, TRUE),
(1, '其他负债', 'OTHER', '其他类型负债', '📝', '#6B7280', 6, TRUE);

-- ============================================
-- 系统预设交易类别
-- ============================================

-- 收入类别
INSERT INTO transaction_categories (user_id, name, type, parent_id, icon, color, display_order, is_system) VALUES
(1, '工资收入', 'INCOME', NULL, '💼', '#10B981', 1, TRUE),
(1, '奖金', 'INCOME', NULL, '🎁', '#10B981', 2, TRUE),
(1, '投资收益', 'INCOME', NULL, '📈', '#10B981', 3, TRUE),
(1, '股息分红', 'INCOME', 3, '💰', '#10B981', 4, TRUE),
(1, '基金收益', 'INCOME', 3, '📊', '#10B981', 5, TRUE),
(1, '租金收入', 'INCOME', NULL, '🏠', '#10B981', 6, TRUE),
(1, '副业收入', 'INCOME', NULL, '💡', '#10B981', 7, TRUE),
(1, '其他收入', 'INCOME', NULL, '📝', '#10B981', 8, TRUE);

-- 支出类别
INSERT INTO transaction_categories (user_id, name, type, parent_id, icon, color, display_order, is_system) VALUES
(1, '餐饮', 'EXPENSE', NULL, '🍽️', '#EF4444', 1, TRUE),
(1, '购物', 'EXPENSE', NULL, '🛍️', '#EF4444', 2, TRUE),
(1, '交通', 'EXPENSE', NULL, '🚗', '#EF4444', 3, TRUE),
(1, '住房', 'EXPENSE', NULL, '🏠', '#EF4444', 4, TRUE),
(1, '房租', 'EXPENSE', 13, '🏡', '#EF4444', 5, TRUE),
(1, '房贷', 'EXPENSE', 13, '🏦', '#EF4444', 6, TRUE),
(1, '水电燃气', 'EXPENSE', 13, '💡', '#EF4444', 7, TRUE),
(1, '物业费', 'EXPENSE', 13, '🏢', '#EF4444', 8, TRUE),
(1, '医疗健康', 'EXPENSE', NULL, '🏥', '#EF4444', 9, TRUE),
(1, '教育培训', 'EXPENSE', NULL, '📚', '#EF4444', 10, TRUE),
(1, '娱乐休闲', 'EXPENSE', NULL, '🎮', '#EF4444', 11, TRUE),
(1, '保险', 'EXPENSE', NULL, '🛡️', '#EF4444', 12, TRUE),
(1, '通讯', 'EXPENSE', NULL, '📱', '#EF4444', 13, TRUE),
(1, '还款', 'EXPENSE', NULL, '💳', '#EF4444', 14, TRUE),
(1, '其他支出', 'EXPENSE', NULL, '📝', '#EF4444', 15, TRUE);

-- ============================================
-- 常用汇率初始化（示例）
-- ============================================

-- 人民币作为基准货币
INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) VALUES
('CNY', 'CNY', 1.000000, CURDATE(), 'system'),
('USD', 'CNY', 7.200000, CURDATE(), 'system'),
('EUR', 'CNY', 7.800000, CURDATE(), 'system'),
('JPY', 'CNY', 0.048000, CURDATE(), 'system'),
('GBP', 'CNY', 9.100000, CURDATE(), 'system'),
('HKD', 'CNY', 0.920000, CURDATE(), 'system'),
('AUD', 'CNY', 4.750000, CURDATE(), 'system'),
('CAD', 'CNY', 5.300000, CURDATE(), 'system');

-- ============================================
-- 示例：创建测试用户和数据（可选）
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
-- 注意事项
-- ============================================

-- 1. 实际使用时，应该在用户注册时为每个用户创建个性化的类别
-- 2. 密码必须使用强加密算法（如BCrypt）存储
-- 3. 汇率数据应该定期从第三方API更新
-- 4. 生产环境中不应该包含明文密码或测试用户
-- 5. 考虑为高频查询的列添加适当的索引
