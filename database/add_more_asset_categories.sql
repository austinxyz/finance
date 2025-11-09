-- 添加更多资产分类
-- 在现有基础上添加更细分的资产类别

-- 获取用户ID (假设是第一个用户)
SET @user_id = 1;

-- ========== 现金类 ==========
-- 定期存款
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '定期存款', 'CASH', '银行定期存款、大额存单', '🏦', '#10B981', 21, true);

-- 外币存款
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '外币存款', 'CASH', '美元、欧元等外币存款', '💱', '#10B981', 22, true);

-- ========== 股票投资类 ==========
-- ETF基金
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, 'ETF基金', 'STOCKS', '交易型开放式指数基金', '📊', '#3B82F6', 23, true);

-- 债券
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '债券', 'STOCKS', '国债、企业债、可转债', '📜', '#3B82F6', 24, true);

-- 可转债
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '可转债', 'STOCKS', '可转换债券', '🔄', '#3B82F6', 25, true);

-- ========== 贵金属类 ==========
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '黄金', 'PRECIOUS_METALS', '实物黄金、纸黄金、黄金ETF', '🥇', '#F59E0B', 26, true);

INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '白银', 'PRECIOUS_METALS', '实物白银、纸白银', '⚪', '#9CA3AF', 27, true);

-- ========== 其他投资类 ==========
-- 私募股权
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '私募股权', 'OTHER', 'PE投资、股权投资', '💼', '#8B5CF6', 28, true);

-- 信托产品
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '信托产品', 'OTHER', '信托理财产品', '🏛️', '#8B5CF6', 29, true);

-- 银行理财
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '银行理财', 'OTHER', '银行理财产品', '🏦', '#10B981', 30, true);

-- 资管计划
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '资管计划', 'OTHER', '券商资管、基金专户', '📋', '#8B5CF6', 31, true);

-- ========== 实物资产类 ==========
-- 商铺
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '商铺', 'REAL_ESTATE', '商业地产、店铺', '🏪', '#EC4899', 32, true);

-- 车位
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '车位', 'REAL_ESTATE', '停车位产权', '🅿️', '#EC4899', 33, true);

-- 汽车
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '汽车', 'OTHER', '私家车、商用车', '🚗', '#6B7280', 34, true);

-- 收藏品
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '收藏品', 'OTHER', '艺术品、古董、邮票', '🎨', '#F59E0B', 35, true);

-- 珠宝首饰
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '珠宝首饰', 'OTHER', '钻石、玉石、珠宝', '💎', '#EC4899', 36, true);

-- ========== 数字资产类 ==========
-- 稳定币
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '稳定币', 'CRYPTOCURRENCY', 'USDT、USDC等稳定币', '💵', '#10B981', 37, true);

-- DeFi资产
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, 'DeFi资产', 'CRYPTOCURRENCY', '去中心化金融资产', '🌐', '#8B5CF6', 38, true);

-- NFT
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, 'NFT', 'CRYPTOCURRENCY', '非同质化代币', '🖼️', '#EC4899', 39, true);

-- ========== 教育与知识产权 ==========
-- 教育储蓄
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '教育储蓄', 'OTHER', '529计划、教育基金', '🎓', '#3B82F6', 40, true);

-- 知识产权
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '知识产权', 'OTHER', '专利、版权等无形资产', '📚', '#8B5CF6', 41, true);

-- ========== 应收款项 ==========
-- 应收账款
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '应收账款', 'OTHER', '他人欠款、应收款项', '💰', '#10B981', 42, true);

-- 借出款项
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system)
VALUES (@user_id, '借出款项', 'OTHER', '借给他人的款项', '🤝', '#F59E0B', 43, true);

SELECT COUNT(*) as total_categories FROM asset_categories WHERE user_id = @user_id;
