-- 合并AustinXu和LorrianeChen到同一个家庭

-- 1. 更新AustinXu的家庭名称为Austin Family
UPDATE families
SET family_name = 'Austin Family'
WHERE family_name = 'AustinXu\'s Family';

-- 2. 将LorrianeChen的family_id改为AustinXu的family_id
UPDATE users u
SET u.family_id = (SELECT id FROM families WHERE family_name = 'Austin Family')
WHERE u.username = 'LorrianeChen';

-- 3. 删除LorrianeChen's Family（现在没有用户关联了）
DELETE FROM families
WHERE family_name = 'LorrianeChen\'s Family';

-- 4. 验证结果
SELECT '=== 合并后的家庭 ===' as info;
SELECT * FROM families;

SELECT '' as separator;
SELECT '=== 用户-家庭关系 ===' as info;
SELECT
    u.id,
    u.username,
    u.family_id,
    f.family_name
FROM users u
LEFT JOIN families f ON u.family_id = f.id;
