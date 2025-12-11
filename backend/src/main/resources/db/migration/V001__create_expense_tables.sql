-- ================================================================
-- 支出管理模块 - 数据库表结构
-- 版本: v1.0
-- 日期: 2024-12-10
-- 说明: 共3张表（大类、子分类、支出记录）
-- ================================================================

-- ----------------------------------------------------------------
-- 1. 支出大类表 (expense_categories_major)
-- 用途：存储固定的9个大类（子女、衣、食、住、行、保险、人情、娱乐、经营、其他）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expense_categories_major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  code VARCHAR(50) NOT NULL UNIQUE COMMENT '大类编码（CHILDREN, CLOTHING等）',
  name VARCHAR(50) NOT NULL COMMENT '大类名称',
  icon VARCHAR(50) COMMENT '图标（emoji或图标类名）',
  color VARCHAR(20) COMMENT '颜色代码',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_is_active (is_active),
  KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出大类表';

-- ----------------------------------------------------------------
-- 2. 支出子分类表 (expense_categories_minor)
-- 用途：存储各大类下的子分类，可自定义
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expense_categories_minor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  major_category_id BIGINT NOT NULL COMMENT '所属大类ID',
  name VARCHAR(100) NOT NULL COMMENT '子分类名称',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否系统默认分类',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  FOREIGN KEY (major_category_id) REFERENCES expense_categories_major(id),
  UNIQUE KEY uk_major_name (major_category_id, name),
  KEY idx_is_active (is_active),
  KEY idx_major_category (major_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出子分类表';

-- ----------------------------------------------------------------
-- 3. 支出记录表 (expense_records)
-- 用途：存储月度支出记录（月度汇总，非流水账）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expense_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  family_id BIGINT NOT NULL COMMENT '家庭ID',
  user_id BIGINT NOT NULL COMMENT '记录人ID',

  -- 期间与分类
  expense_year INT NOT NULL COMMENT '支出年份',
  expense_month INT NOT NULL COMMENT '支出月份',
  expense_period VARCHAR(7) NOT NULL COMMENT '支出期间（YYYY-MM）',
  major_category_id BIGINT NOT NULL COMMENT '大类ID',
  minor_category_id BIGINT NOT NULL COMMENT '子分类ID',

  -- 金额
  amount DECIMAL(18, 2) NOT NULL COMMENT '支出金额',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币代码',
  amount_in_base_currency DECIMAL(18, 2) COMMENT '基准货币金额（后端计算）',

  -- 类型与说明
  expense_type VARCHAR(20) NOT NULL COMMENT '支出类型（FIXED_DAILY/LARGE_IRREGULAR）',
  description TEXT COMMENT '说明',

  -- 审计字段
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  FOREIGN KEY (family_id) REFERENCES families(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (major_category_id) REFERENCES expense_categories_major(id),
  FOREIGN KEY (minor_category_id) REFERENCES expense_categories_minor(id),

  UNIQUE KEY uk_period_category (family_id, expense_period, minor_category_id),
  KEY idx_family_period (family_id, expense_year, expense_month),
  KEY idx_expense_period (expense_period),
  KEY idx_major_category (major_category_id),
  KEY idx_minor_category (minor_category_id),
  KEY idx_expense_type (expense_type),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出记录表';

-- ================================================================
-- 索引说明：
-- 1. expense_categories_major:
--    - idx_is_active: 快速筛选启用的大类
--    - idx_sort_order: 排序查询优化
--
-- 2. expense_categories_minor:
--    - uk_major_name: 保证同一大类下子分类名称唯一
--    - idx_is_active: 快速筛选启用的子分类
--    - idx_major_category: 查询某大类下所有子分类
--
-- 3. expense_records:
--    - uk_period_category: 保证同一期间同一分类只有一条记录
--    - idx_family_period: 查询某家庭某月支出
--    - idx_expense_period: 时间范围查询
--    - idx_minor_category: 查询某分类历史记录
--    - idx_expense_type: 固定/大额筛选
-- ================================================================
