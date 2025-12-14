# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **IMPORTANT for Claude**:
> - **Project Root**: `/Users/yanzxu/claude/finance/`
> - After any `/clear` or context summarization, always read this CLAUDE.md file again
> - If working directory changes, navigate back to project root: `cd /Users/yanzxu/claude/finance`
> - This ensures you're working in the correct project context

## Project Overview

**Name**: personal-finance
**Root Directory**: `/Users/yanzxu/claude/finance/`
**Description**: 个人理财管理系统 (Personal Finance Management System) - A full-stack application for managing personal assets, liabilities, and financial analysis using Java Spring Boot backend and Vue.js frontend with Tailwind CSS.

## Environment Setup

### Java Version Configuration

This project **REQUIRES Java 17**. Use the `/setup-java` skill to automatically configure the environment:

```bash
/setup-java
```

This skill will:
- Set JAVA_HOME to Java 17
- Load database credentials from backend/.env
- Export all necessary environment variables

### Database Configuration

Database credentials are stored in `backend/.env` file (not tracked in git):

```bash
# backend/.env example
DB_URL=jdbc:mysql://your-host:port/finance?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**Note**: The `.env` file is loaded automatically by the setup script. DO NOT commit this file to git.

## Development Commands

### Backend (Spring Boot with Hot Reload)

**ALWAYS run `/setup-java` first!**

```bash
cd backend
mvn clean install          # Build project
mvn spring-boot:run        # Start with hot reload
mvn test                   # Run tests
```

**Hot Reload**: Spring Boot DevTools is enabled - backend will automatically restart when you save Java files.

### Frontend (Vue.js + Vite with Hot Module Replacement)

```bash
cd frontend
npm install                # Install dependencies (first time only)
npm run dev                # Development server (port 3000)
npm run build              # Production build
npm run preview            # Preview production build
```

**Hot Reload**: Vite HMR is enabled - frontend will automatically update in browser when you save Vue/JS/CSS files.

### MySQL Database Operations

Use the `/mysql-exec` skill for all MySQL operations:

```bash
# Execute SQL file
/mysql-exec path/to/script.sql

# Interactive MySQL shell
/mysql-exec

# Quick query
/mysql-exec "SHOW TABLES;"
```

**Database Type Tables:**
- `asset_type` - Asset type definitions (CASH, STOCKS, RETIREMENT_FUND, INSURANCE, REAL_ESTATE, CRYPTOCURRENCY, PRECIOUS_METALS, OTHER)
- `liability_type` - Liability type definitions (MORTGAGE, AUTO_LOAN, CREDIT_CARD, PERSONAL_LOAN, STUDENT_LOAN, BUSINESS_LOAN, OTHER)
- `net_asset_categories` - Net asset category definitions (REAL_ESTATE_NET, RETIREMENT_FUND_NET, LIQUID_NET, INVESTMENT_NET, OTHER_NET)
- `expense_categories_major` - Expense major category definitions (CHILDREN, CLOTHING, FOOD, HOUSING, etc.)
- `expense_categories_minor` - Expense minor category definitions (subcategories under each major category)

## Architecture

### Backend Architecture (com.finance.app)

**Layered Structure:**
- `controller/` - REST API endpoints, handles HTTP requests
- `service/` - Business logic layer, orchestrates data operations
- `repository/` - Spring Data JPA repositories for database access
- `model/` - JPA entity classes (database table mappings)
- `dto/` - Data Transfer Objects for API contracts
- `config/` - Spring configuration (CORS, etc.)

**Key Technologies:**
- Spring Boot 3.2.0 with Java 17
- Spring Data JPA with Hibernate (ddl-auto=update)
- MySQL 8.0 dialect
- Lombok for reducing boilerplate

**Database Connection:**
- Uses environment variables (DB_URL, DB_USERNAME, DB_PASSWORD)
- Connection pool configured with Tomcat JDBC

### Frontend Architecture

**Component Structure:**
- `components/` - Reusable UI components
  - `MainLayout.vue` - Main app layout with sidebar and top bar
  - `Sidebar.vue` - Navigation sidebar with sections: 仪表盘, 资产管理, 负债管理, 数据分析, 智能建议
  - `ui/` - shadcn/ui style components (Card, Button, etc.)
- `views/` - Page components organized by feature
  - `Dashboard.vue` - Main dashboard
  - `assets/` - Asset management views
  - `liabilities/` - Liability management views
  - `analysis/` - Data analysis views
- `router/` - Vue Router configuration with lazy loading
- `api/` - Axios-based API client (proxy to localhost:8080)
- `lib/utils.js` - Utility functions (cn() for className merging)

**Key Technologies:**
- Vue 3 with Composition API
- Tailwind CSS with custom theme (green primary color for finance theme)
- shadcn/ui component patterns (radix-vue based)
- Lucide icons
- Chart.js for data visualization
- Vite for build tooling

**Styling System:**
- Tailwind CSS with CSS variables for theming
- Dark mode support via `class` strategy
- Custom color palette defined in `style.css` using HSL values
- `cn()` utility combines clsx and tailwind-merge for conditional classes

### Frontend Best Practices

See `docs/frontend-best-practices.md` for detailed implementation guides:

- **Chart.js Pie Charts**: Label format "名称-百分比%", 5% visibility threshold, use chartjs-plugin-datalabels
- **Currency Display**: Consistent formatting with currency symbols based on selected currency
- **Responsive Layouts**: 50/50 split for chart-table layouts, use Tailwind flex utilities

### Data Model (Planned)

**Core Entities:**
- Users
- Asset Categories (现金, 股票, 退休基金, 保险, 房产, 数字货币)
- Asset Accounts (multiple accounts per category)
- Asset Records (time-series data points)
- Liability Categories (房贷, 车贷, 信用卡, 个人借债, etc.)
- Liability Accounts
- Liability Records (time-series data)
- Financial Goals

**Time-Series Pattern:**
Assets and liabilities use a record-based time-series approach where each account can have multiple timestamped value records for trend analysis.

## API Structure

Base path: `http://localhost:8080/api` (not `/api/api` - the context path is already `/api`)

**Planned Endpoints:**
- `/assets/*` - Asset management CRUD
- `/liabilities/*` - Liability management CRUD
- `/records/*` - Time-series data recording
- `/analysis/*` - Financial analysis and calculations

## Frontend-Backend Integration

- Frontend dev server (port 3000) proxies `/api` requests to backend (port 8080)
- CORS is configured in `CorsConfig.java` to allow all origins in development
- API requests use Axios instance in `src/api/request.js`

## Key Business Logic

**Financial Calculations:**
- Total Assets = sum of all asset account values at latest timestamp
- Total Liabilities = sum of all liability account values at latest timestamp
- Net Worth = Total Assets - Total Liabilities
- Asset Allocation = percentage distribution across asset categories
- Debt-to-Asset Ratio = Total Liabilities / Total Assets

**Multi-Currency Support (Planned):**
Data entry supports multiple currencies with automatic conversion to base currency - USD.

## Important Notes

- JPA is set to `ddl-auto=update` - schema changes are auto-applied but be careful with production data
- The system shares MySQL server with zjutennis project but uses separate `finance` schema
- Frontend uses lazy loading for all routes except Dashboard for better initial load performance
- Sidebar navigation is Chinese-language focused (仪表盘, 资产管理, etc.)
- Time-series data recording is a core pattern - many operations involve creating new timestamped records rather than updating existing values

## Requirements Reference

See `requirement/需求说明.md` for detailed feature specifications including:
- Asset/liability category taxonomies
- Analysis requirements (trends, allocation, risk assessment)
- Smart recommendation features (AI-driven suggestions)
- Report generation and export capabilities

## Available Skills

This project includes several Claude Code skills (slash commands) for common tasks:

### `/setup-java`
Automatically configures Java 17 environment and loads database credentials from backend/.env

### `/mysql-exec`
Execute MySQL commands with automatic credential loading. Supports SQL files, inline queries, and interactive shell.

### `/git-push`
Reference guide for Git operations including commit conventions and branch management.

## Typical Development Workflow

1. **Start Development**:
   ```bash
   /setup-java           # Configure Java 17 + load DB credentials
   cd backend
   mvn spring-boot:run   # Start backend with hot reload
   ```

2. **Database Operations**:
   ```bash
   /mysql-exec           # Open interactive shell
   # Or
   /mysql-exec "SELECT * FROM asset_accounts;"
   ```

3. **Code Changes**:
   - Make changes to Java/Vue files
   - Both backend and frontend auto-reload on save
   - Test changes in browser

4. **Commit and Push**:
   ```bash
   git status
   git add .
   git commit -m "feat: implement new feature"
   git push origin main
   ```
