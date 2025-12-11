-- V005: 创建支出年度预算表
-- 用途：为每个家庭的每个支出子分类设定年度预算

CREATE TABLE expense_budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    budget_year INT NOT NULL COMMENT '预算年份（如2025）',
    minor_category_id BIGINT NOT NULL COMMENT '子分类ID',
    budget_amount DECIMAL(15,2) NOT NULL COMMENT '预算金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型（USD, CNY等）',
    notes VARCHAR(500) COMMENT '备注说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 约束：每个家庭每年每个子分类每种货币只能有一条预算记录
    CONSTRAINT uk_family_year_category_currency UNIQUE (family_id, budget_year, minor_category_id, currency),

    -- 外键
    CONSTRAINT fk_budget_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_budget_minor_category FOREIGN KEY (minor_category_id) REFERENCES expense_categories_minor(id),

    -- 索引
    INDEX idx_family_year (family_id, budget_year),
    INDEX idx_minor_category (minor_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出年度预算表';
