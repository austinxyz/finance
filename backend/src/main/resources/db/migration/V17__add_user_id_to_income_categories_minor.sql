-- 添加 user_id 字段到收入小类表
ALTER TABLE income_categories_minor
ADD COLUMN user_id BIGINT NULL AFTER major_category_id;

-- 添加外键约束（可选，如果需要强制引用完整性）
-- ALTER TABLE income_categories_minor
-- ADD CONSTRAINT fk_income_category_minor_user
-- FOREIGN KEY (user_id) REFERENCES users(id)
-- ON DELETE SET NULL;
