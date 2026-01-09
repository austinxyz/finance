-- ============================================
-- JWT Authentication System - Database Migration
-- ============================================

-- ============================================
-- 1. 添加用户角色字段
-- ============================================
ALTER TABLE users
ADD COLUMN role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER'
COMMENT '用户角色：ADMIN=管理员，USER=普通用户'
AFTER is_active;

CREATE INDEX idx_role ON users(role);

-- ============================================
-- 2. 创建审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    action VARCHAR(100) NOT NULL COMMENT '操作类型：CREATE_USER, DELETE_USER, UPDATE_USER, RESET_PASSWORD, CREATE_FAMILY, DELETE_FAMILY等',
    entity_type VARCHAR(50) COMMENT '实体类型：USER, FAMILY, EXPENSE, INCOME等',
    entity_id BIGINT COMMENT '实体ID',
    old_value JSON COMMENT '操作前的值（JSON格式）',
    new_value JSON COMMENT '操作后的值（JSON格式）',
    ip_address VARCHAR(45) COMMENT '操作IP地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_action (user_id, action),
    INDEX idx_created_at (created_at),
    INDEX idx_entity (entity_type, entity_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) COMMENT '审计日志表：记录管理员操作和敏感数据变更';

-- ============================================
-- 3. 创建初始管理员账户
-- ============================================
-- 注意：密码hash为临时占位符，需要在应用启动后通过管理员端点生成真实BCrypt hash
-- 临时密码：Admin@123（首次登录后必须修改）

INSERT INTO users (username, email, password_hash, full_name, role, is_active, family_id, created_at, updated_at)
VALUES (
    'admin',
    'admin@finance.local',
    -- BCrypt hash for "Admin@123" (使用BCrypt 10轮加密)
    -- 注意：这是示例hash，实际部署时需要通过密码加密端点生成
    '$2a$10$YourBCryptHashWillBeGeneratedByTheApplication',
    'System Administrator',
    'ADMIN',
    1,
    1,  -- 关联到family_id=1 (Austin family)
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    role = 'ADMIN',
    updated_at = NOW();

-- ============================================
-- 4. 更新现有用户为普通用户角色
-- ============================================
UPDATE users
SET role = 'USER'
WHERE role IS NULL OR role = '';

-- ============================================
-- 5. 验证数据
-- ============================================
-- 检查角色分配情况
SELECT
    role,
    COUNT(*) as user_count,
    GROUP_CONCAT(username) as usernames
FROM users
GROUP BY role;

-- 检查审计日志表是否创建成功
SHOW CREATE TABLE audit_logs;
