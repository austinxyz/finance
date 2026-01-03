-- 为google_sheets_sync表添加异步任务状态字段

ALTER TABLE google_sheets_sync
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
ADD COLUMN progress INT DEFAULT 100,
ADD COLUMN error_message TEXT;

-- 为现有记录设置默认值（已完成的同步任务）
UPDATE google_sheets_sync
SET status = 'COMPLETED', progress = 100
WHERE status IS NULL OR status = '';

-- 添加索引以优化状态查询
CREATE INDEX idx_google_sheets_sync_status ON google_sheets_sync(status);
CREATE INDEX idx_google_sheets_sync_family_year ON google_sheets_sync(family_id, year);

-- 添加注释
COMMENT ON COLUMN google_sheets_sync.status IS '任务状态: PENDING, IN_PROGRESS, COMPLETED, FAILED';
COMMENT ON COLUMN google_sheets_sync.progress IS '进度百分比 (0-100)';
COMMENT ON COLUMN google_sheets_sync.error_message IS '错误信息（任务失败时）';
