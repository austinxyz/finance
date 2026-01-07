-- 创建年度收入汇总表
-- 用于存储每年按大类和小类汇总的收入数据
-- 类似于 annual_expense_summary，但用于收入

CREATE TABLE IF NOT EXISTS annual_income_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT NULL COMMENT '用户ID（可为空，表示家庭级别汇总）',
    summary_year INT NOT NULL COMMENT '汇总年份',
    major_category_id BIGINT NOT NULL COMMENT '收入大类ID',
    minor_category_id BIGINT NULL COMMENT '收入小类ID（NULL表示大类汇总）',

    -- 基础收入（来自income_records的直接汇总）
    base_income_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '基础收入金额',

    -- 特殊收入调整（一次性收入、奖励等）
    special_income_amount DECIMAL(18,2) NULL DEFAULT 0.00 COMMENT '特殊收入金额',
    special_income_details JSON NULL COMMENT '特殊收入详情（JSON格式）',

    -- 资产相关调整（如投资收益的细分调整）
    asset_adjustment DECIMAL(18,2) NULL DEFAULT 0.00 COMMENT '资产相关调整金额',
    liability_adjustment DECIMAL(18,2) NULL DEFAULT 0.00 COMMENT '负债相关调整金额',
    adjustment_details JSON NULL COMMENT '调整详情（JSON格式）',

    -- 实际收入（基础收入 + 特殊收入 + 调整）
    actual_income_amount DECIMAL(18,2) NOT NULL COMMENT '实际收入金额',

    currency VARCHAR(10) NULL DEFAULT 'USD' COMMENT '币种',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_family_year (family_id, summary_year),
    INDEX idx_user_year (user_id, summary_year),
    INDEX idx_major_category (major_category_id),
    INDEX idx_minor_category (minor_category_id),

    -- 唯一约束：同一家庭、同一年、同一大类、同一小类、同一币种只能有一条记录
    -- 注意：MySQL的唯一索引不支持函数表达式，需要用触发器或应用层保证唯一性

    -- 外键约束
    CONSTRAINT fk_income_summary_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_income_summary_major_category FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    CONSTRAINT fk_income_summary_minor_category FOREIGN KEY (minor_category_id) REFERENCES income_categories_minor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年度收入汇总表';

-- 创建索引以提升查询性能
CREATE INDEX idx_summary_year ON annual_income_summary(summary_year);
CREATE INDEX idx_actual_amount ON annual_income_summary(actual_income_amount);
