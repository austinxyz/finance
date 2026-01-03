-- Google Sheets同步记录表
CREATE TABLE google_sheets_sync (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    year INT NOT NULL COMMENT '年份',
    spreadsheet_id VARCHAR(255) NOT NULL COMMENT 'Google Sheets电子表格ID',
    share_url TEXT NOT NULL COMMENT '分享链接',
    permission VARCHAR(20) NOT NULL DEFAULT 'reader' COMMENT '权限设置：reader或writer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_family_year (family_id, year),
    INDEX idx_family_id (family_id),
    INDEX idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Google Sheets同步记录表';
