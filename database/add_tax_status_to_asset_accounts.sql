-- Add tax_status field to asset_accounts table
-- This migration adds a new field to track the tax status of asset accounts

ALTER TABLE asset_accounts
ADD COLUMN tax_status VARCHAR(20) DEFAULT 'TAXABLE'
COMMENT '税务状态: TAXABLE(应税), TAX_FREE(免税), TAX_DEFERRED(延迟纳税)'
AFTER is_active;

-- Update existing records to have default TAXABLE status
UPDATE asset_accounts
SET tax_status = 'TAXABLE'
WHERE tax_status IS NULL;
