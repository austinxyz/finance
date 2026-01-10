# 家庭理财管理系统

基于 Spring Boot + Vue.js 的全栈财务管理系统，支持资产负债追踪、收支分析和现金流管理。

> **最新更新 (v1.1.0)**: 现金流整合视图上线 - 收支对比、储蓄率趋势、月度分析全面呈现
> **核心功能完成度**: 98% | **性能**: 页面加载 <2秒 | **多平台**: amd64 + arm64

## ✨ 核心功能

### 🔐 安全认证
- **JWT 认证** - 基于 Token 的无状态认证，安全可靠
- **角色权限** - Admin/User 双角色体系，细粒度权限控制
- **家庭隔离** - 数据按家庭严格隔离，确保隐私安全
- **密码加密** - BCrypt 加密存储，防止密码泄露

### 📊 数据管理
- **家庭管理** - 多成员协同，统一财务视图
- **资产管理** - 8种资产类型，多币种支持，自动汇率转换
- **负债管理** - 7种负债类型，完整追踪，时序记录
- **收入管理** - 10大类收入分类，月度批量录入，年度预算管理 ⭐
- **支出管理** - 10大类支出分类，批量录入，预算规划，三级钻取
- **投资管理** - 交易记录，成本/现价/盈亏计算，年度分析

### 📈 数据分析
- **现金流分析** - 收支对比、储蓄率趋势、月度明细分析 ⭐ 新增
- **资产配置** - 按成员/货币/税收状态多维度分析（性能优化：<2秒）⚡
- **趋势分析** - 净资产趋势、单项资产盈亏追踪
- **财务指标** - 总资产、净资产、资产负债率、流动性比率
- **Google Sheets导出** - 年度财务报表，异步导出，实时进度

## 🎯 系统亮点

### 数据完整性
- 资产、负债、收入、支出、投资五大维度全覆盖
- 完整的时序数据记录与历史追踪
- 多币种支持（USD/CNY/EUR/GBP/JPY/AUD/CAD）

### 性能卓越 ⚡
- N+1查询问题修复（批量查询优化）
- 页面加载时间从10-30秒降至<2秒
- Repository层批量查询方法优化

### 智能分析 📊
- 现金流健康度评估（储蓄率、月度结余）
- 投资收益智能排除（聚焦实际现金流）
- 多维度可视化（Chart.js图表）

### 安全可靠 🔒
- JWT 认证 + 家庭级数据隔离
- 账户级权限验证（User → Family 关系链）
- 管理员全局访问，普通用户限制本家庭数据
- 密码 BCrypt 加密存储

### 易用性
- 响应式设计，移动端友好
- 批量录入，提升效率
- 智能数字格式化

## 🛠️ 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + MySQL 8.0 + JPA
- **前端**: Vue 3 (Composition API) + Tailwind CSS + Chart.js
- **部署**: Kubernetes (Helm) + Docker (多架构: amd64/arm64)
- **工具**: Maven + Vite + Google Sheets API

## 快速开始

### 本地开发

```bash
# 1. 配置数据库
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 配置环境变量
cp backend/.env.example backend/.env
# 编辑 backend/.env 填入实际的数据库连接信息：
#   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, JWT_SECRET

# 3. 启动后端（端口 8080）
./backend/start.sh

# 4. 启动前端（端口 3000）
cd frontend
npm install
npm run dev
```

### Docker Compose 部署

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填入数据库连接信息和 JWT 密钥

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f

# 4. 访问应用
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080/api
```

### Kubernetes部署

```bash
# 使用 Helm 一键部署（包含 MySQL）
cd k8s
./deploy.sh install

# 访问应用
kubectl port-forward -n finance svc/finance-frontend 3000:80
```

详见 [k8s/README.md](./k8s/README.md)

## 数据导入工具

Excel数据批量导入工具，支持费用和预算数据的预览、验证和导入。

```bash
cd import

# 1. 生成预览文件
python3 import_from_excel.py preview --year 2024

# 2. 检查新记录
python3 import_from_excel.py check --year 2024

# 3. 导入数据
python3 import_from_excel.py import --year 2024

# 4. 清理临时文件
python3 import_from_excel.py clean --year 2024
```

详见 [import/README.md](./import/README.md)

## 项目结构

```
finance/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue.js 前端
├── database/         # 数据库脚本
├── import/           # Excel 导入工具
├── k8s/              # Kubernetes 部署
└── requirement/      # 需求文档
```

## API 文档

- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **API 详细说明**: [requirement/API文档.md](./requirement/API文档.md)

## 主要端点

### 认证授权
- `/api/auth/login` - 用户登录（获取 JWT Token）
- `/api/auth/admin/encrypt-passwords` - 密码加密迁移（管理员）

### 数据管理
- `/api/assets/*` - 资产管理（账户、记录、批量更新）
- `/api/liabilities/*` - 负债管理（账户、记录、批量更新）
- `/api/incomes/*` - 收入管理（分类、记录、批量录入）
- `/api/expenses/*` - 支出管理（分类、记录、预算、批量录入）
- `/api/investments/*` - 投资管理（交易记录、账户分析）
- `/api/family` - 家庭管理（成员、切换）

### 数据分析
- `/api/analysis/*` - 综合分析（趋势、配置、财务指标）
- `/api/analysis/cashflow` - 现金流分析 ⭐ 新增
- `/api/incomes/analysis/*` - 收入分析（年度、大类、小类）
- `/api/expenses/analysis/*` - 支出分析（年度、预算对比）
- `/api/investments/analysis/*` - 投资分析（年度、账户、月度趋势）

### 工具
- `/api/exchange-rates/*` - 汇率管理
- `/api/google-sheets/*` - Google Sheets导出

## 📅 开发路线图

### ✅ 已完成 (v1.1.0)
- [x] JWT 认证与授权系统（家庭级数据隔离）
- [x] 资产负债管理系统
- [x] 收入管理模块（10大类分类、预算管理）
- [x] 支出管理模块（10大类分类、预算管理）
- [x] 现金流整合视图（收支对比、储蓄率趋势）
- [x] 投资管理与分析
- [x] 性能优化（N+1查询修复）
- [x] Google Sheets导出
- [x] 多架构Docker镜像（amd64/arm64）

### 🔄 进行中
- [ ] 财务目标管理（短期/中期/长期目标设定与追踪）
- [ ] 智能分析算法完善（风险评估、优化建议）

### 📝 计划中
- [ ] 现金流预测（基于历史趋势预测未来3-6个月）
- [ ] 投资分析增强（IRR、夏普比率等高级指标）
- [ ] 智能预警系统
- [ ] 移动端应用

## 📖 文档

- **需求文档**: [requirement/需求说明.md](./requirement/需求说明.md)
- **API文档**: [requirement/API文档.md](./requirement/API文档.md)
- **功能缺口分析**: [requirement/功能缺口分析.md](./requirement/功能缺口分析.md)
- **授权设计**: [docs/authorization-design.md](./docs/authorization-design.md)
- **前端最佳实践**: [docs/frontend-best-practices.md](./docs/frontend-best-practices.md)
- **部署指南**: [k8s/README.md](./k8s/README.md)
- **数据导入**: [import/README.md](./import/README.md)

## 🤝 贡献

本项目使用 [Claude Code](https://claude.com/claude-code) 进行AI辅助开发。

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 📊 项目统计

- **核心功能完成度**: 98%
- **代码行数**: ~50,000+ (Java + Vue)
- **数据库表**: 40+
- **API端点**: 100+
- **支持币种**: 7种
- **Docker镜像**: 多架构支持 (amd64/arm64)
