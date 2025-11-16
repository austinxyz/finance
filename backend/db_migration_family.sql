-- 数据库迁移脚本：添加家庭功能
-- 执行前请备份数据库！

-- =====================================================
-- 1. 创建 families 表
-- =====================================================
CREATE TABLE IF NOT EXISTS families (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_name VARCHAR(100) NOT NULL,
    annual_expenses DECIMAL(15, 2) DEFAULT 0,
    emergency_fund_months INT DEFAULT 6,
    financial_goals TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_family_name (family_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. 修改 users 表，添加新字段
-- =====================================================

-- 检查并添加 family_id 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'family_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN family_id BIGINT AFTER id', 'SELECT ''family_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 age 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'age');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN age INT AFTER full_name', 'SELECT ''age already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 annual_income 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'annual_income');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN annual_income DECIMAL(15, 2) AFTER age', 'SELECT ''annual_income already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 risk_tolerance 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_tolerance');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN risk_tolerance VARCHAR(20) AFTER annual_income', 'SELECT ''risk_tolerance already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 notes 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'notes');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN notes TEXT AFTER risk_tolerance', 'SELECT ''notes already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_family_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE users ADD INDEX idx_family_id (family_id)', 'SELECT ''idx_family_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3. 数据迁移：为现有用户创建默认家庭
-- =====================================================

-- 为每个现有用户创建一个默认家庭
INSERT INTO families (family_name, annual_expenses, emergency_fund_months, financial_goals, created_at, updated_at)
SELECT
    CONCAT(CAST(u.username AS CHAR CHARACTER SET utf8mb4), '''s Family') COLLATE utf8mb4_unicode_ci as family_name,
    COALESCE(up.estimated_annual_expenses, 0) as annual_expenses,
    COALESCE(up.emergency_fund_months, 6) as emergency_fund_months,
    up.notes as financial_goals,
    NOW() as created_at,
    NOW() as updated_at
FROM users u
LEFT JOIN user_profiles up ON u.id = up.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM families f
    WHERE f.family_name = CONCAT(CAST(u.username AS CHAR CHARACTER SET utf8mb4), '''s Family') COLLATE utf8mb4_unicode_ci
);

-- 将用户关联到对应的家庭
UPDATE users u
INNER JOIN families f ON f.family_name = CONCAT(CAST(u.username AS CHAR CHARACTER SET utf8mb4), '''s Family') COLLATE utf8mb4_unicode_ci
SET u.family_id = f.id
WHERE u.family_id IS NULL;

-- 迁移风险承受能力数据
UPDATE users u
INNER JOIN user_profiles up ON u.id = up.user_id
SET u.risk_tolerance = up.risk_tolerance
WHERE u.risk_tolerance IS NULL;

-- =====================================================
-- 4. 验证数据迁移
-- =====================================================
SELECT
    '=== 家庭表统计 ===' as info,
    COUNT(*) as total_families
FROM families;

SELECT
    '=== 用户-家庭关联统计 ===' as info,
    COUNT(*) as users_with_family,
    COUNT(DISTINCT family_id) as unique_families
FROM users
WHERE family_id IS NOT NULL;

SELECT
    '=== 未关联家庭的用户 ===' as info,
    COUNT(*) as users_without_family
FROM users
WHERE family_id IS NULL;

-- =====================================================
-- 5. 可选：备份 user_profiles 表后删除
-- （建议先不删除，确认系统正常运行后再删除）
-- =====================================================
-- RENAME TABLE user_profiles TO user_profiles_backup;

SELECT '迁移完成！请检查上述统计信息' as message;
