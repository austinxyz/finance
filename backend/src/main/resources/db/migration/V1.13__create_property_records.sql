-- 创建房产记录表
-- 用于记录房产购买的详细信息，与资产账户关联
-- 在年度支出汇总时，购买年份会使用特殊的计算逻辑

CREATE TABLE property_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',

    -- 关联资产账户
    asset_account_id BIGINT NOT NULL COMMENT '关联的房产资产账户ID',

    -- 购买信息
    purchase_date DATE NOT NULL COMMENT '购买日期',
    down_payment DECIMAL(18, 2) NOT NULL COMMENT '首付金额',
    mortgage_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '房贷金额（初始贷款金额）',
    property_value DECIMAL(18, 2) NOT NULL COMMENT '房产价值（购买时的市场价值）',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币',

    -- 补充信息
    notes TEXT COMMENT '备注说明',

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    CONSTRAINT fk_property_asset_account
        FOREIGN KEY (asset_account_id)
        REFERENCES asset_accounts(id)
        ON DELETE CASCADE,

    -- 唯一约束：每个资产账户只能有一条房产记录
    CONSTRAINT uk_property_asset_account
        UNIQUE (asset_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房产记录表';

-- 创建索引
CREATE INDEX idx_property_purchase_date ON property_records(purchase_date);
CREATE INDEX idx_property_asset_account ON property_records(asset_account_id);
