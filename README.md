# 家庭理财管理系统

基于 Spring Boot + Vue.js 的全栈财务管理系统，支持资产负债追踪、支出分析和税务规划。

## 核心功能

- **家庭管理** - 多成员协同，统一财务视图
- **资产管理** - 8种资产类型，自动汇率转换
- **负债管理** - 7种负债类型，完整追踪
- **支出管理** - 分类管理、批量录入、预算规划、多维分析
- **税收分析** - 应税/免税/延税分类，优化税务
- **数据分析** - 趋势分析、配置分析、财务指标

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + MySQL 8.0
- **前端**: Vue 3 + Tailwind CSS + Chart.js
- **部署**: Kubernetes (Helm) + Docker

## 快速开始

### 本地开发

```bash
# 1. 配置数据库
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 设置环境变量（或使用 backend/.env）
export DB_URL="jdbc:mysql://localhost:3306/finance"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"

# 3. 启动后端（端口 8080）
cd backend
mvn spring-boot:run

# 4. 启动前端（端口 3000）
cd frontend
npm install
npm run dev
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

- `/api/assets/*` - 资产管理
- `/api/liabilities/*` - 负债管理
- `/api/expenses/*` - 支出管理
- `/api/analysis/*` - 数据分析
- `/api/family` - 家庭管理

## 许可证

MIT License
