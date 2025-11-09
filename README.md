# 个人理财管理系统

一个基于Java Spring Boot + Vue.js的个人资产负债管理系统，帮助您追踪财务状况，分析资产趋势，并获得智能理财建议。

## 项目结构

```
finance/
├── backend/          # Spring Boot后端
├── frontend/         # Vue.js前端
├── database/         # 数据库脚本
├── requirement/      # 需求文档
└── README.md
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0
- Maven

### 前端
- Vue.js 3 (Composition API)
- Vue Router 4
- Tailwind CSS (样式框架)
- Chart.js (数据可视化)
- Vite (构建工具)
- Axios (HTTP客户端)

## 核心功能

### 资产管理
- 支持多种资产类型：现金、股票、退休基金、保险、房产、数字货币等
- 每种资产可包含多个账号
- 时间序列数据记录

### 负债管理
- 支持多种负债类型：房贷、车贷、信用卡、个人借债等
- 负债详情追踪（利率、期限等）

### 数据分析
- 资产/负债时间趋势分析
- 盈亏计算
- 资产配置占比分析
- 财务指标计算（总资产、净资产、资产负债率等）

### 智能建议
- 风险评估
- 资产配置优化建议
- 负债管理建议
- 财务目标规划

## 快速开始

### 环境要求

- Java 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 数据库配置

1. 创建数据库schema：
```sql
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量：

复制环境变量模板文件：
```bash
cp .env.example .env
```

编辑 `.env` 文件，填入你的数据库配置：
```bash
DB_URL=jdbc:mysql://localhost:3306/finance?ssl-mode=REQUIRED
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**注意：** `.env` 文件包含敏感信息，已被添加到 `.gitignore`，不会被提交到版本控制。

### 后端启动

确保已经配置好 `.env` 文件后：

```bash
cd backend

# 设置Java环境（如果需要）
export JAVA_HOME=/path/to/java17

# 加载环境变量
export DB_URL="jdbc:mysql://localhost:3306/finance?ssl-mode=REQUIRED"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"

# 构建并运行
mvn clean install
mvn spring-boot:run
```

后端服务将运行在 http://localhost:8080

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端应用将运行在 http://localhost:3000

## 开发指南

### 后端项目结构

```
backend/src/main/java/com/finance/app/
├── controller/      # REST API控制器
├── service/         # 业务逻辑层
├── repository/      # 数据访问层
├── model/           # 实体类
├── dto/             # 数据传输对象
└── config/          # 配置类
```

### 前端项目结构

```
frontend/src/
├── components/      # 可复用组件
├── views/           # 页面视图
├── router/          # 路由配置
├── stores/          # Pinia状态管理
├── api/             # API请求封装
└── assets/          # 静态资源
```

## API文档

后端API基础路径：`http://localhost:8080/api`

主要API端点：
- `/assets/*` - 资产管理
- `/liabilities/*` - 负债管理
- `/records/*` - 记录管理
- `/analysis/*` - 数据分析

## 配置说明

### 后端配置文件

配置文件位置：`backend/src/main/resources/application.properties`

主要配置项：
- 数据库连接（通过环境变量）
- JPA配置
- 日志级别
- 服务端口

### 前端配置文件

配置文件位置：`frontend/vite.config.js`

- API代理配置
- 开发服务器端口
- 路径别名

## 需求文档

详细需求说明请查看：[需求说明.md](./requirement/需求说明.md)

## 已实现功能

- [x] 用户管理基础功能
- [x] 资产账户CRUD操作
- [x] 资产分类管理
- [x] 时间序列资产记录
- [x] 批量更新资产记录（支持重复检测和自动填充）
- [x] 资产历史记录管理（增删改查）
- [x] 资产趋势图表可视化（Chart.js）
- [x] 多时间范围分析（本周/本月/本年/全部）
- [x] 资产配置占比分析
- [x] 净资产计算
- [x] 多币种支持

## 待开发功能

- [ ] 用户认证与授权（JWT）
- [ ] 负债管理模块
- [ ] 智能推荐引擎
- [ ] 报表导出功能（PDF/Excel）
- [ ] 移动端适配
- [ ] 数据备份与恢复

## 许可证

MIT License
