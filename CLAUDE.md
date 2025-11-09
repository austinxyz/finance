# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

个人理财管理系统 (Personal Finance Management System) - A full-stack application for managing personal assets, liabilities, and financial analysis using Java Spring Boot backend and Vue.js frontend with Tailwind CSS.

## Development Commands

### Backend (Spring Boot)
```bash
cd backend
mvn clean install          # Build project
mvn spring-boot:run        # Run backend server (port 8080)
mvn test                   # Run all tests
```

### Frontend (Vue.js + Vite)
```bash
cd frontend
npm install                # Install dependencies
npm run dev                # Run dev server (port 3000)
npm run build              # Build for production
npm run preview            # Preview production build
```

### Database Setup
```bash
# Create MySQL database
mysql -u root -p
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Set environment variables (shared with zjutennis project)
export DB_URL="jdbc:mysql://localhost:3306/finance?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
```

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
- Shares database server with zjutennis project, different schema name: `finance`
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
Data entry supports multiple currencies with automatic conversion to base currency (CNY).

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
