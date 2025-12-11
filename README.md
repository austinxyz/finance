# 家庭理财管理系统

一个基于 Java Spring Boot + Vue.js 的家庭资产负债管理系统，帮助您和家人追踪财务状况、分析资产趋势、管理税收规划。支持多成员家庭协同管理，统一查看家庭整体财务状况。

## 核心功能

- **家庭管理** - 支持创建家庭，添加家庭成员，统一管理家庭财务
- **资产管理** - 支持现金、股票、退休基金、保险、房产、数字货币、贵金属等 8 种资产类型
- **负债管理** - 追踪房贷、车贷、信用卡、个人借债等 7 种负债类型
- **支出管理** - 完整的支出跟踪系统，支持分类管理、批量录入、预算规划和多维分析
- **税收分析** - 资产税收状态分类（应税/免税/延税），优化税务规划
- **数据分析** - 时间趋势分析、资产配置分析、财务指标计算、支出分析，支持按家庭维度统计
- **可视化** - 基于 Chart.js 的图表展示，支持历史数据查询和家庭切换
- **批量操作** - 支持批量更新资产、负债和支出记录，自动填充历史数据

## 技术栈

**后端**
- Java 17 + Spring Boot 3.2.0
- Spring Data JPA + MySQL 8.0
- Maven 3.6+

**前端**
- Vue.js 3 (Composition API)
- Tailwind CSS + Chart.js
- Vite + Axios

## 快速开始

### 数据库配置

```sql
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

配置环境变量：
```bash
export DB_URL="jdbc:mysql://localhost:3306/finance?useSSL=false&serverTimezone=UTC"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
```

### 启动项目

**后端：**
```bash
cd backend
export JAVA_HOME=/path/to/java17
mvn clean install
mvn spring-boot:run
```
服务运行在 http://localhost:8080

**前端：**
```bash
cd frontend
npm install
npm run dev
```
应用运行在 http://localhost:3000

## 主要特性

### 资产配置分析
- 净资产分类饼图（资产减去对应负债）
- 税收状态分析（应税/免税/延税净值分布）
- 负债优先抵扣应税资产，再抵扣免税和延税资产
- 支持指定日期查看历史配置
- 多货币显示（USD/CNY）

### 趋势分析
- 综合趋势（净资产、总资产、总负债）
- 分类趋势（资产/负债/净资产各类别）
- 多时间范围（本周/本月/本年/全部）
- 变化百分比和绝对值显示
- 支持按家庭维度统计所有成员的资产负债

### 支出管理（全新模块）
**分类管理**
- 10 个预设大类（子女👶、衣👔、食🍜、住🏠、行🚗、保险🛡️、人情🎁、娱乐🎮、经营💼、其他📦）
- 灵活的子分类管理，支持自定义创建
- 支出类型标识（固定日常/大额不定期）

**批量录入**
- 月度批量输入，支持3个月历史对比
- 按支出类型自动排序（固定支出优先）
- 多币种支持（7种货币：USD/CNY/EUR/GBP/JPY/AUD/CAD）
- 智能汇率转换，自动计算基准货币金额

**预算规划**
- 年度预算设定，按子分类管理
- 预算执行分析，实际 vs 预算对比
- 图表可视化，直观展示预算完成情况

**多维分析**
- 年度支出分析：三级钻取（大类→小类→月度趋势）
- 同币种统计：选择特定货币查看该币种的支出汇总
- 交叉币种汇总：选择"All"查看所有货币换算后的总支出
- Chart.js 可视化：饼图、柱状图，支持 K/M 智能格式化

### 批量数据管理
- 批量更新资产、负债和支出记录
- 自动检测重复记录并提示
- 自动填充历史数据
- 支持多币种和汇率转换

## 项目结构

```
finance/
├── backend/          # Spring Boot 后端
│   └── src/main/java/com/finance/app/
│       ├── controller/  # REST API
│       ├── service/     # 业务逻辑
│       ├── repository/  # 数据访问
│       ├── model/       # 实体类
│       └── dto/         # 数据传输对象
├── frontend/         # Vue.js 前端
│   └── src/
│       ├── views/       # 页面视图
│       ├── components/  # 可复用组件
│       ├── api/         # API 封装
│       └── router/      # 路由配置
├── database/         # 数据库脚本
└── requirement/      # 需求文档
```

## API 端点

基础路径：`http://localhost:8080/api`

### 资产负债管理
- `/assets/*` - 资产管理 CRUD
- `/liabilities/*` - 负债管理 CRUD
- `/analysis/*` - 资产负债数据分析与趋势

### 支出管理
- `/expenses/categories` - 支出分类管理（大类和小类）
- `/expenses/categories/minor` - 子分类 CRUD（创建、更新、禁用）
- `/expenses/records` - 支出记录 CRUD
- `/expenses/records/batch` - 批量保存支出记录
- `/expenses/records/range` - 按时间范围查询记录
- `/expense-budgets` - 年度预算管理
- `/expense-budgets/batch` - 批量保存预算

### 支出分析
- `/expenses/analysis/annual/major-categories` - 年度大类汇总分析
- `/expenses/analysis/annual/minor-categories` - 年度小类汇总分析
- `/expenses/analysis/annual/monthly-trend` - 年度月度趋势分析
- `/expenses/analysis/budget-execution` - 预算执行分析

### 其他
- `/family` - 家庭管理
- `/exchange-rates` - 汇率管理
- `/annual-summary` - 年度财务汇总

## 详细文档

查看 [需求说明文档](./requirement/需求说明.md) 了解完整功能规划。

## 许可证

MIT License
