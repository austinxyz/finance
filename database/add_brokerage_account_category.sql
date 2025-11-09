-- 在现金类中添加"证券账号"小类别
-- 代表在证券商开户账号中的现金

USE finance;

-- 为所有用户添加"证券账号"类别
-- 如果你的系统有多个用户，需要为每个用户都添加
INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system, created_at, updated_at)
VALUES
(1, '证券账号', 'CASH', '证券商开户账号中的现金余额', '📊', '#10B981', 4, TRUE, NOW(), NOW());

-- 如果有其他用户，可以使用下面的语句批量添加
-- INSERT INTO asset_categories (user_id, name, type, description, icon, color, display_order, is_system, created_at, updated_at)
-- SELECT id, '证券账号', 'CASH', '证券商开户账号中的现金余额', '📊', '#10B981', 4, TRUE, NOW(), NOW()
-- FROM users
-- WHERE id > 1;

-- 查看添加结果
SELECT id, user_id, name, type, description, display_order, is_system
FROM asset_categories
WHERE type = 'CASH'
ORDER BY display_order;
