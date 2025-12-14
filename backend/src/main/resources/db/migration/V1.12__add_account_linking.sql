-- 添加资产账户和负债账户的关联字段
-- 用于建立房产和房贷等关联关系

-- 在资产账户表添加关联负债账户ID字段
ALTER TABLE asset_accounts
ADD COLUMN linked_liability_account_id BIGINT NULL COMMENT '关联的负债账户ID（例如：房产关联房贷）',
ADD CONSTRAINT fk_asset_linked_liability
    FOREIGN KEY (linked_liability_account_id)
    REFERENCES liability_accounts(id)
    ON DELETE SET NULL;

-- 在负债账户表添加关联资产账户ID字段
ALTER TABLE liability_accounts
ADD COLUMN linked_asset_account_id BIGINT NULL COMMENT '关联的资产账户ID（例如：房贷关联房产）',
ADD CONSTRAINT fk_liability_linked_asset
    FOREIGN KEY (linked_asset_account_id)
    REFERENCES asset_accounts(id)
    ON DELETE SET NULL;

-- 创建索引以提高查询性能
CREATE INDEX idx_asset_linked_liability ON asset_accounts(linked_liability_account_id);
CREATE INDEX idx_liability_linked_asset ON liability_accounts(linked_asset_account_id);
