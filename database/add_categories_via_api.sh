#!/bin/bash

# 通过API添加更多资产分类
API_URL="http://localhost:8080/assets/categories"
USER_ID=1

# 定期存款
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "定期存款",
  "type": "CASH",
  "description": "银行定期存款、大额存单",
  "icon": "🏦",
  "color": "#10B981",
  "displayOrder": 21,
  "isSystem": true
}'

# 外币存款
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "外币存款",
  "type": "CASH",
  "description": "美元、欧元等外币存款",
  "icon": "💱",
  "color": "#10B981",
  "displayOrder": 22,
  "isSystem": true
}'

# ETF基金
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "ETF基金",
  "type": "STOCKS",
  "description": "交易型开放式指数基金",
  "icon": "📊",
  "color": "#3B82F6",
  "displayOrder": 23,
  "isSystem": true
}'

# 债券
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "债券",
  "type": "STOCKS",
  "description": "国债、企业债、可转债",
  "icon": "📜",
  "color": "#3B82F6",
  "displayOrder": 24,
  "isSystem": true
}'

# 黄金
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "黄金",
  "type": "PRECIOUS_METALS",
  "description": "实物黄金、纸黄金、黄金ETF",
  "icon": "🥇",
  "color": "#F59E0B",
  "displayOrder": 25,
  "isSystem": true
}'

# 白银
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "白银",
  "type": "PRECIOUS_METALS",
  "description": "实物白银、纸白银",
  "icon": "⚪",
  "color": "#9CA3AF",
  "displayOrder": 26,
  "isSystem": true
}'

# 银行理财
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "银行理财",
  "type": "OTHER",
  "description": "银行理财产品",
  "icon": "🏦",
  "color": "#10B981",
  "displayOrder": 27,
  "isSystem": true
}'

# 信托产品
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "信托产品",
  "type": "OTHER",
  "description": "信托理财产品",
  "icon": "🏛️",
  "color": "#8B5CF6",
  "displayOrder": 28,
  "isSystem": true
}'

# 私募股权
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "私募股权",
  "type": "OTHER",
  "description": "PE投资、股权投资",
  "icon": "💼",
  "color": "#8B5CF6",
  "displayOrder": 29,
  "isSystem": true
}'

# 商铺
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "商铺",
  "type": "REAL_ESTATE",
  "description": "商业地产、店铺",
  "icon": "🏪",
  "color": "#EC4899",
  "displayOrder": 30,
  "isSystem": true
}'

# 车位
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "车位",
  "type": "REAL_ESTATE",
  "description": "停车位产权",
  "icon": "🅿️",
  "color": "#EC4899",
  "displayOrder": 31,
  "isSystem": true
}'

# 汽车
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "汽车",
  "type": "OTHER",
  "description": "私家车、商用车",
  "icon": "🚗",
  "color": "#6B7280",
  "displayOrder": 32,
  "isSystem": true
}'

# 收藏品
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "收藏品",
  "type": "OTHER",
  "description": "艺术品、古董、邮票",
  "icon": "🎨",
  "color": "#F59E0B",
  "displayOrder": 33,
  "isSystem": true
}'

# 珠宝首饰
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "珠宝首饰",
  "type": "OTHER",
  "description": "钻石、玉石、珠宝",
  "icon": "💎",
  "color": "#EC4899",
  "displayOrder": 34,
  "isSystem": true
}'

# 稳定币
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "稳定币",
  "type": "CRYPTOCURRENCY",
  "description": "USDT、USDC等稳定币",
  "icon": "💵",
  "color": "#10B981",
  "displayOrder": 35,
  "isSystem": true
}'

# 应收账款
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "应收账款",
  "type": "OTHER",
  "description": "他人欠款、应收款项",
  "icon": "💰",
  "color": "#10B981",
  "displayOrder": 36,
  "isSystem": true
}'

# 借出款项
curl -X POST $API_URL -H "Content-Type: application/json" -d '{
  "userId": '$USER_ID',
  "name": "借出款项",
  "type": "OTHER",
  "description": "借给他人的款项",
  "icon": "🤝",
  "color": "#F59E0B",
  "displayOrder": 37,
  "isSystem": true
}'

echo ""
echo "分类添加完成！"
