-- ============================================================
-- 家庭理财管理系统 - 初始数据
-- ============================================================
-- 包含类型定义表的基础数据
--
-- 生成时间: $(date '+%Y-%m-%d %H:%M:%S')
-- ============================================================

SET NAMES utf8mb4;



-- ============================================================
-- Table: asset_type
-- ============================================================

TRUNCATE TABLE `asset_type`;
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (1,'现金类','#10B981','2025-12-14 00:02:47.000000','银行存款、手头现金、货币基金、定期存款等流动性资产',1,'💵',_binary '\0','Cash & Equivalents','CASH','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (2,'股票投资','#3B82F6','2025-12-14 00:02:47.000000','国内外股票、基金、ETF、债券等证券投资',2,'📈',_binary '','Stocks & Funds','STOCKS','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (3,'退休基金','#8B5CF6','2025-12-14 00:02:47.000000','养老保险、401k、IRA等退休账户',3,'🎯',_binary '','Retirement Funds','RETIREMENT_FUND','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (4,'保险','#F59E0B','2025-12-14 00:02:47.000000','人寿保险、年金保险等具有现金价值的保险产品',4,'🛡️',_binary '\0','Insurance','INSURANCE','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (5,'房地产','#EF4444','2025-12-14 00:02:47.000000','自住房产、投资房产、商铺、车位等不动产',5,'🏠',_binary '','Real Estate','REAL_ESTATE','2025-12-14 21:11:23.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (6,'数字货币','#F97316','2025-12-14 00:02:47.000000','比特币、以太坊、稳定币等数字资产',6,'₿',_binary '','Cryptocurrency','CRYPTOCURRENCY','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (7,'贵金属','#F59E0B','2025-12-14 00:02:47.000000','黄金、白银等贵金属投资',7,'🥇',_binary '','Precious Metals','PRECIOUS_METALS','2025-12-14 00:02:47.000000');
INSERT INTO `asset_type` (`id`, `chinese_name`, `color`, `created_at`, `description`, `display_order`, `icon`, `is_investment`, `name`, `type`, `updated_at`) VALUES (8,'其他','#6B7280','2025-12-14 00:02:47.000000','汽车、收藏品、珠宝首饰、应收账款等其他资产',8,'📦',_binary '\0','Other Assets','OTHER','2025-12-14 00:02:47.000000');

-- ============================================================
-- Table: liability_type
-- ============================================================

TRUNCATE TABLE `liability_type`;
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (1,'MORTGAGE','房贷','Mortgage','住房抵押贷款','🏠','#EF4444',1,'2025-12-14 01:28:17','2025-12-14 01:28:17');
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (2,'AUTO_LOAN','车贷','Auto Loan','汽车贷款','🚗','#F59E0B',2,'2025-12-14 01:28:17','2025-12-14 01:28:17');
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (3,'CREDIT_CARD','信用卡','Credit Card','信用卡负债','💳','#EC4899',3,'2025-12-14 01:28:17','2025-12-14 01:28:17');
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (4,'PERSONAL_LOAN','个人贷款','Personal Loan','个人消费贷款','💰','#8B5CF6',4,'2025-12-14 01:28:17','2025-12-14 01:28:17');
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (5,'STUDENT_LOAN','学生贷款','Student Loan','教育贷款','🎓','#3B82F6',5,'2025-12-14 01:28:17','2025-12-14 01:28:17');
INSERT INTO `liability_type` (`id`, `type`, `chinese_name`, `english_name`, `description`, `icon`, `color`, `display_order`, `created_at`, `updated_at`) VALUES (6,'OTHER','其他负债','Other','其他类型负债','📝','#6B7280',6,'2025-12-14 01:28:17','2025-12-14 01:28:17');

-- ============================================================
-- Table: net_asset_categories
-- ============================================================

TRUNCATE TABLE `net_asset_categories`;
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (1,'REAL_ESTATE_NET','#10b981','2025-11-09 17:32:48.843809','房地产总值减去房贷',1,'房地产净值','2025-11-09 17:32:48.843817');
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (2,'LIQUID_NET','#3b82f6','2025-11-09 17:32:48.851423','现金类资产减去信用卡和其他短期负债',3,'流动资产净值','2025-11-14 04:12:35.000000');
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (3,'INVESTMENT_NET','#8b5cf6','2025-11-09 17:32:48.855795','股票和退休基金等投资',5,'投资净值','2025-11-14 04:12:35.000000');
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (4,'OTHER_NET','#f59e0b','2025-11-09 17:32:48.860512','其他资产',6,'其他净值','2025-11-14 04:12:35.000000');
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (5,'RETIREMENT_FUND_NET','#8b5cf6','2025-11-14 04:12:35.000000','退休基金资产净值（不可即时取出的长期投资）',2,'退休基金净值','2025-11-14 04:12:35.000000');
INSERT INTO `net_asset_categories` (`id`, `code`, `color`, `created_at`, `description`, `display_order`, `name`, `updated_at`) VALUES (6,'INSURANCE_NET','#ec4899','2025-12-12 16:32:04.000000','保险资产价值',4,'保险净值','2025-12-12 16:32:04.000000');

-- ============================================================
-- Table: expense_categories_major
-- ============================================================

TRUNCATE TABLE `expense_categories_major`;
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (1,'CHILDREN','子女','👶','#FF6B6B',1,1,'子女相关支出：学费、补习、兴趣班等','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (2,'CLOTHING','衣','👔','#4ECDC4',2,1,'服装鞋帽相关支出','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (3,'FOOD','食','🍜','#95E1D3',3,1,'饮食相关支出：外出就餐、食材采购等','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (4,'HOUSING','住','🏠','#F38181',4,1,'居住相关支出：房租/房贷、物业费、水电等','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (5,'TRANSPORTATION','行','🚗','#AA96DA',5,1,'交通相关支出：公共交通、打车、加油等','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (6,'INSURANCE','保险','🛡️','#FCBAD3',6,1,'保险相关支出：车险、医疗保险等','2025-12-10 19:16:25','2025-12-10 19:16:25');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (7,'SOCIAL','人情','🎁','#FFFFD2',8,1,'人情往来支出：礼金、送礼、孝敬长辈等','2025-12-10 19:16:25','2025-12-11 03:10:27');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (8,'ENTERTAINMENT','娱乐','🎮','#A8D8EA',9,1,'娱乐相关支出：旅游、电影、健身等','2025-12-10 19:16:25','2025-12-11 03:10:27');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (9,'BUSINESS','经营','💼','#FFB6B9',10,1,'经营相关支出：办公费用、营销费用等','2025-12-10 19:16:25','2025-12-11 03:10:28');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (10,'OTHER','其他','📦','#C7CEEA',11,1,'其他未分类支出','2025-12-10 19:16:25','2025-12-11 03:10:28');
INSERT INTO `expense_categories_major` (`id`, `code`, `name`, `icon`, `color`, `sort_order`, `is_active`, `description`, `created_at`, `updated_at`) VALUES (21,'MEDICAL','医疗','🏥','#FF6B9D',7,1,'医疗相关支出：看病、买药、体检等','2025-12-11 03:10:27','2025-12-11 03:10:27');

-- ============================================================
-- Table: expense_categories_minor
-- ============================================================

TRUNCATE TABLE `expense_categories_minor`;
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (65,1,'育儿费用',1,1,0,'子女相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:09:46');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (66,2,'服装化妆品',1,1,0,'衣相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:14:03');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (67,3,'饮食',1,1,0,'食相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:14:24');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (68,4,'水电煤物业',1,1,0,'住相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:15:03');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (69,5,'日常交通',1,1,0,'行相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:17:18');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (71,7,'朋友人情',1,1,0,'人情相关支出','LARGE_IRREGULAR','2025-12-10 20:18:49','2025-12-11 01:09:51');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (72,8,'娱乐健身',1,1,0,'娱乐相关支出','FIXED_DAILY','2025-12-10 20:18:49','2025-12-11 00:20:50');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (80,10,'未分类',1,1,0,'暂未分类的支出','LARGE_IRREGULAR','2025-12-10 20:18:49','2025-12-11 00:21:42');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (81,4,'生活用品',1,0,0,'','FIXED_DAILY','2025-12-10 20:19:31','2025-12-11 00:15:34');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (82,1,'学费',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:12:49','2025-12-11 00:12:49');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (83,1,'礼金',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:13:31','2025-12-11 00:13:39');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (84,4,'租房还贷',1,0,0,'','FIXED_DAILY','2025-12-11 00:16:16','2025-12-11 00:16:16');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (85,4,'房产税',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:16:29','2025-12-11 00:16:29');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (86,4,'保姆',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:16:47','2025-12-11 00:16:47');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (87,5,'车保养保险',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:17:34','2025-12-11 00:17:34');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (88,5,'旅游',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:17:51','2025-12-11 00:17:51');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (89,6,'人寿保险',1,0,0,'','FIXED_DAILY','2025-12-11 00:18:26','2025-12-11 00:18:26');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (90,6,'养老保险',1,0,0,'','FIXED_DAILY','2025-12-11 00:18:44','2025-12-11 00:18:44');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (91,7,'亲戚往来',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:19:10','2025-12-11 00:20:27');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (92,5,'中美往来机票',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:19:43','2025-12-11 00:19:43');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (93,8,'健身美容',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:21:01','2025-12-11 00:21:01');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (94,8,'数码产品',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:21:13','2025-12-11 00:21:13');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (95,9,'经营开支',1,0,0,'','LARGE_IRREGULAR','2025-12-11 00:21:31','2025-12-11 00:21:31');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (96,6,'房屋保险',1,0,0,'','LARGE_IRREGULAR','2025-12-11 03:09:25','2025-12-11 03:09:25');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (97,21,'门诊',1,1,0,'医疗相关支出','LARGE_IRREGULAR','2025-12-11 03:10:28','2025-12-11 03:11:29');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (98,4,'装修',1,0,0,'','LARGE_IRREGULAR','2025-12-15 02:47:18','2025-12-15 02:47:18');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (99,4,'买房',1,0,0,'','LARGE_IRREGULAR','2025-12-16 06:31:14','2025-12-16 06:31:14');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (100,4,'首付',1,0,0,'','FIXED_DAILY','2025-12-17 04:35:01','2025-12-17 04:35:01');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (101,4,'契税中介费',1,0,0,'','FIXED_DAILY','2025-12-17 04:35:39','2025-12-17 04:35:39');
INSERT INTO `expense_categories_minor` (`id`, `major_category_id`, `name`, `is_active`, `is_default`, `sort_order`, `description`, `expense_type`, `created_at`, `updated_at`) VALUES (102,5,'买车',1,0,0,'','LARGE_IRREGULAR','2025-12-17 07:51:01','2025-12-17 07:51:01');

-- ============================================================
-- Table: transaction_categories
-- ============================================================

TRUNCATE TABLE `transaction_categories`;
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (1,1,'工资收入','INCOME',NULL,'💼','#10B981',1,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (2,1,'奖金','INCOME',NULL,'🎁','#10B981',2,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (3,1,'投资收益','INCOME',NULL,'📈','#10B981',3,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (4,1,'租金收入','INCOME',NULL,'🏠','#10B981',6,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (5,1,'副业收入','INCOME',NULL,'💡','#10B981',7,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (6,1,'其他收入','INCOME',NULL,'📝','#10B981',8,1,'2025-11-09 06:06:22');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (8,1,'餐饮','EXPENSE',NULL,'🍽️','#EF4444',1,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (9,1,'购物','EXPENSE',NULL,'🛍️','#EF4444',2,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (10,1,'交通','EXPENSE',NULL,'🚗','#EF4444',3,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (11,1,'住房','EXPENSE',NULL,'🏠','#EF4444',4,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (12,1,'医疗健康','EXPENSE',NULL,'🏥','#EF4444',9,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (13,1,'教育培训','EXPENSE',NULL,'📚','#EF4444',10,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (14,1,'娱乐休闲','EXPENSE',NULL,'🎮','#EF4444',11,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (15,1,'保险','EXPENSE',NULL,'🛡️','#EF4444',12,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (16,1,'通讯','EXPENSE',NULL,'📱','#EF4444',13,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (17,1,'还款','EXPENSE',NULL,'💳','#EF4444',14,1,'2025-11-09 06:06:23');
INSERT INTO `transaction_categories` (`id`, `user_id`, `name`, `type`, `parent_id`, `icon`, `color`, `display_order`, `is_system`, `created_at`) VALUES (18,1,'其他支出','EXPENSE',NULL,'📝','#EF4444',15,1,'2025-11-09 06:06:23');

-- ============================================================
-- Table: asset_liability_type_mappings
-- ============================================================

-- 表 asset_liability_type_mappings 暂无数据

-- ============================================================
-- Table: net_asset_category_asset_type_mappings
-- ============================================================

TRUNCATE TABLE `net_asset_category_asset_type_mappings`;
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (1,'REAL_ESTATE','2025-11-09 17:32:48.865448',1);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (2,'CASH','2025-11-09 17:32:48.879375',2);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (3,'STOCKS','2025-11-09 17:32:48.914188',3);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (7,'PRECIOUS_METALS','2025-11-09 17:32:48.934770',4);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (8,'OTHER','2025-11-09 17:32:48.939729',4);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (9,'RETIREMENT_FUND','2025-11-14 04:12:35.000000',5);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (10,'INSURANCE','2025-12-12 16:32:04.000000',6);
INSERT INTO `net_asset_category_asset_type_mappings` (`id`, `asset_type`, `created_at`, `net_asset_category_id`) VALUES (11,'CRYPTOCURRENCY','2025-12-12 16:35:59.000000',3);

-- ============================================================
-- Table: net_asset_category_liability_type_mappings
-- ============================================================

TRUNCATE TABLE `net_asset_category_liability_type_mappings`;
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (1,'2025-11-09 17:32:48.870016','MORTGAGE',1);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (2,'2025-11-09 17:32:48.874426','AUTO_LOAN',1);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (3,'2025-11-09 17:32:48.885132','CREDIT_CARD',2);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (4,'2025-11-09 17:32:48.891100','PERSONAL_LOAN',2);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (5,'2025-11-09 17:32:48.897502','STUDENT_LOAN',2);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (6,'2025-11-09 17:32:48.904417','BUSINESS_LOAN',2);
INSERT INTO `net_asset_category_liability_type_mappings` (`id`, `created_at`, `liability_type`, `net_asset_category_id`) VALUES (8,'2025-12-12 16:39:14.000000','OTHER',4);

-- ============================================================
-- Table: expense_category_adjustment_config
-- ============================================================

TRUNCATE TABLE `expense_category_adjustment_config`;
INSERT INTO `expense_category_adjustment_config` (`id`, `major_category_id`, `adjustment_type`, `asset_type_code`, `liability_type`, `adjustment_direction`, `description`, `is_active`, `created_at`, `updated_at`) VALUES (1,4,'LIABILITY',NULL,'MORTGAGE','ADD','房贷本金偿还计入住房支出: 实际支出 = 月度支出 + 房贷减少额',1,'2025-12-12 19:11:24','2025-12-12 19:11:24');
INSERT INTO `expense_category_adjustment_config` (`id`, `major_category_id`, `adjustment_type`, `asset_type_code`, `liability_type`, `adjustment_direction`, `description`, `is_active`, `created_at`, `updated_at`) VALUES (2,6,'ASSET','INSURANCE',NULL,'SUBTRACT','保险资产增值扣除: 实际支出 = 月度保费 - 保险现金价值增加',1,'2025-12-12 19:11:24','2025-12-12 19:11:24');
