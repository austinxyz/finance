# 家庭理财管理系统

一个基于 Java Spring Boot + Vue.js 的家庭资产负债管理系统，支持多成员协同管理、资产追踪、支出分析和税收规划。

## 核心功能

- **家庭管理** - 多成员协同，统一财务视图
- **资产管理** - 8种资产类型，自动汇率转换
- **负债管理** - 7种负债类型，完整追踪
- **支出管理** - 分类管理、批量录入、预算规划、多维分析
- **税收分析** - 应税/免税/延税分类，优化税务
- **数据分析** - 趋势分析、配置分析、财务指标

## 技术栈

**后端:** Java 17 + Spring Boot 3.2 + MySQL 8.0
**前端:** Vue 3 + Tailwind CSS + Chart.js

## 快速开始

### 环境准备

```bash
# 数据库
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 环境变量
export DB_URL="jdbc:mysql://localhost:3306/finance"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
```

### 启动服务

```bash
# 后端 (端口 8080)
cd backend
mvn spring-boot:run

# 前端 (端口 3000)
cd frontend
npm install
npm run dev
```

## 项目结构

```
finance/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue.js 前端
├── database/         # 数据库脚本
└── requirement/      # 需求文档
```

## 主要端点

- `/api/assets/*` - 资产管理
- `/api/liabilities/*` - 负债管理
- `/api/expenses/*` - 支出管理
- `/api/analysis/*` - 数据分析
- `/api/family` - 家庭管理

## 文档

- [需求说明文档](./requirement/需求说明.md) - 完整功能规划
- [API文档](./requirement/API文档.md) - 接口详细说明
- **Swagger UI** - http://localhost:8080/api/swagger-ui/index.html - 交互式API文档

## 许可证

MIT License
