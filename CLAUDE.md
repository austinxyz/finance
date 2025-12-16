# CLAUDE.md

Claude Code 工作指南 - 家庭理财管理系统

> **重要提示**:
> - 项目根目录: `/Users/yanzxu/claude/finance/`
> - 会话重置后，先读取此文件恢复上下文

## 项目概览

**技术栈**: Java 17 + Spring Boot 3.2 + Vue 3 + MySQL 8.0
**架构**: 前后端分离 + RESTful API + 时序数据模型

## 环境配置

### 1. Java 环境

**必须使用 Java 17**，运行setup-java技能自动配置：

```bash
/setup-java
```

自动完成：设置JAVA_HOME、加载数据库凭证、导出环境变量

### 2. 数据库配置

凭证存储在 `backend/.env`（不提交到git）：

```bash
DB_URL=jdbc:mysql://host:port/finance?useSSL=false&serverTimezone=UTC
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

## 开发命令

### 后端（Spring Boot）

```bash
cd backend
mvn clean install          # 构建项目
mvn spring-boot:run        # 启动（支持热重载）
mvn test                   # 运行测试
```

**热重载**: Spring Boot DevTools自动重启

### 前端（Vue 3 + Vite）

```bash
cd frontend
npm install                # 安装依赖
npm run dev                # 开发服务器（端口3000，HMR）
npm run build              # 生产构建
```

**热重载**: Vite HMR自动更新

### 数据库操作

使用 `/mysql-exec` 技能：

```bash
/mysql-exec path/to/script.sql    # 执行SQL文件
/mysql-exec "SHOW TABLES;"        # 快速查询
/mysql-exec                       # 交互式shell
```

## 架构设计

### 后端分层（com.finance.app）

```
controller/    # REST API端点
service/       # 业务逻辑
repository/    # 数据访问（Spring Data JPA）
model/         # JPA实体
dto/           # 数据传输对象
config/        # 配置（CORS等）
```

**关键技术**:
- JPA配置: `ddl-auto=update`（自动更新schema）
- 连接池: Tomcat JDBC
- 数据库: MySQL 8.0 dialect

### 前端组件（Vue 3 Composition API）

```
components/    # 可复用组件
  MainLayout.vue    # 主布局
  Sidebar.vue       # 导航侧边栏
  ui/               # shadcn风格组件
views/         # 页面组件
  assets/           # 资产管理
  liabilities/      # 负债管理
  analysis/         # 数据分析
router/        # 路由配置（懒加载）
api/           # Axios API客户端
```

**样式系统**:
- Tailwind CSS + CSS变量主题
- shadcn/ui组件模式（基于radix-vue）
- `cn()`工具: 合并类名（clsx + tailwind-merge）
- 深色模式支持

**最佳实践** (详见 `docs/frontend-best-practices.md`):
- Chart.js饼图: 标签格式"名称-百分比%"，5%可见阈值
- 货币显示: 统一格式化，带货币符号
- 响应式布局: 图表-表格50/50分割

### 数据模型

**核心实体**:
- Users（用户）
- Asset Categories/Accounts/Records（资产类别/账户/记录）
- Liability Categories/Accounts/Records（负债类别/账户/记录）
- Expense Categories/Budgets/Records（支出类别/预算/记录）

**时序数据模式**:
账户使用基于记录的时序方法，每个账户可有多条带时间戳的价值记录，用于趋势分析。

**类型定义表**:
- `asset_type` - 8种资产类型
- `liability_type` - 7种负债类型
- `net_asset_categories` - 净资产分类
- `expense_categories_major` - 支出主类别
- `expense_categories_minor` - 支出子类别

## API 接口

**基础路径**: `http://localhost:8080/api`

**端点**:
- `/assets/*` - 资产CRUD
- `/liabilities/*` - 负债CRUD
- `/expenses/*` - 支出管理
- `/analysis/*` - 财务分析
- `/family` - 家庭管理

**前后端集成**:
- 前端开发服务器（3000端口）代理`/api`到后端（8080端口）
- CORS配置: 开发环境允许所有来源
- API客户端: `src/api/request.js`中的Axios实例

## 业务逻辑

**财务计算**:
```
总资产 = 所有资产账户最新时间戳的值总和
总负债 = 所有负债账户最新时间戳的值总和
净资产 = 总资产 - 总负债
资产配置 = 各类别资产百分比分布
负债比率 = 总负债 / 总资产
```

**多币种支持**: 数据录入支持多币种，自动转换为基础货币（USD）

## 可用技能（Slash Commands）

- `/setup-java` - 配置Java 17 + 加载数据库凭证
- `/mysql-exec` - 执行MySQL命令（SQL文件/查询/交互shell）
- `/git-commit-push` - 暂存、提交、推送到GitHub
- `/docker-build-push` - 构建多架构Docker镜像（amd64/arm64）

## 典型工作流

```bash
# 1. 启动开发
/setup-java              # 配置环境
cd backend && mvn spring-boot:run

# 2. 数据库操作
/mysql-exec              # 交互shell

# 3. 代码修改
# 保存Java/Vue文件 → 自动重载

# 4. 提交代码
git add .
git commit -m "feat: new feature"
git push
```

## 重要提示

- JPA `ddl-auto=update` 会自动应用schema变更，生产环境需谨慎
- 与zjutennis项目共享MySQL服务器，但使用独立的`finance`数据库
- 路由懒加载：除Dashboard外所有路由均懒加载
- 导航界面为中文（仪表盘、资产管理等）
- 时序数据记录是核心模式：创建新时间戳记录，而非更新现有值

## 参考文档

- `requirement/需求说明.md` - 功能详细规划
- `requirement/API文档.md` - 接口说明
- `docs/frontend-best-practices.md` - 前端实现指南
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html
