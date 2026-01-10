-- 添加家庭数据保护字段
-- 用于标记需要额外确认才能删除数据的家庭

ALTER TABLE families
ADD COLUMN is_protected BOOLEAN DEFAULT FALSE COMMENT '是否受保护（删除操作需要二次确认）';

-- 将 Austin家 标记为受保护
UPDATE families
SET is_protected = TRUE
WHERE family_name = 'Austin家';

-- 验证修改
SELECT id, family_name, is_protected
FROM families;
