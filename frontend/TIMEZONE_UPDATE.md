# 时区更新说明

## 更新日期：2025-11-13

## 问题描述
前端系统使用 `new Date().toISOString().split('T')[0]` 来获取日期，这个方法使用 UTC 时区，可能导致在不同时区使用时显示错误的日期。

例如：
- 在美国西海岸时间 11月13日 晚上11点
- UTC 时间已经是 11月14日 早上7点
- 使用 `toISOString()` 会得到 11月14日（错误）
- 应该显示 11月13日（用户本地时区的正确日期）

## 解决方案
在 `src/lib/utils.js` 中添加了两个新的工具函数，使用浏览器/系统的本地时区：

### 新增函数

1. **getTodayDate()**
   - 返回本地时区的当前日期（YYYY-MM-DD 格式）
   - 使用浏览器/系统的时区设置
   - 用于获取默认日期值

2. **formatDateLocal(date)**
   - 将 Date 对象转换为本地时区的日期字符串（YYYY-MM-DD 格式）
   - 用于格式化特定的日期对象

## 已更新的文件

以下文件已经更新为使用本地时区：

1. **frontend/src/views/assets/BatchUpdate.vue**
   - Line 114: 使用 `getTodayDate()` 代替 `new Date().toISOString().split('T')[0]`

2. **frontend/src/views/liabilities/LiabilityBatchUpdate.vue**
   - Line 114: 使用 `getTodayDate()` 代替 `new Date().toISOString().split('T')[0]`

3. **frontend/src/views/tools/ExchangeRateManagement.vue**
   - Line 244: formData 初始化使用 `getTodayDate()`
   - Line 278: openCreateDialog 函数使用 `getTodayDate()`

## 使用方法

在需要获取当前日期的地方：

```javascript
// 旧方法（使用 UTC 时区 - 可能显示错误日期）
const today = new Date().toISOString().split('T')[0]

// 新方法（使用本地时区 - 显示用户所在时区的正确日期）
import { getTodayDate } from '@/lib/utils'
const today = getTodayDate()
```

格式化日期对象：

```javascript
// 旧方法
const dateStr = someDate.toISOString().split('T')[0]

// 新方法
import { formatDateLocal } from '@/lib/utils'
const dateStr = formatDateLocal(someDate)
```

## 注意事项

- 所有新开发的功能应该使用 `getTodayDate()` 函数来获取当前日期
- 如果发现其他页面有日期显示问题，请检查是否使用了 `getTodayDate()` 函数
- 这个更改只影响前端显示，后端数据库仍然使用标准的日期格式
- 函数会自动使用运行环境（浏览器/系统）的本地时区
  - 在加州运行：显示太平洋时区的日期
  - 在中国运行：显示中国时区的日期
  - 在其他地方运行：显示相应时区的日期

## 待办事项

以下文件可能也需要更新（待确认）：
- src/views/analysis/TrendAnalysis.vue
- src/views/Dashboard.vue
- src/views/liabilities/LiabilityHistory.vue
- src/views/liabilities/LiabilityDetail.vue
- src/views/settings/UserManagement.vue
- src/views/assets/AssetHistory.vue (如果存在)
- src/views/assets/AssetDetail.vue (如果存在)
