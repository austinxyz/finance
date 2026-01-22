# Family Finance Management System

A full-stack financial management system built with Spring Boot + Vue.js, supporting asset/liability tracking, income/expense analysis, and cash flow management.

> **Latest Update (v1.2.0)**: Enhanced Exchange Rate Management - Multi-currency support with API integration, historical trends, and time-series charts
> **Feature Completion**: 98% | **Performance**: Page load <2s | **Multi-platform**: amd64 + arm64

## ✨ Core Features

### 🔐 Security & Authentication
- **JWT Authentication** - Token-based stateless authentication
- **Role-Based Access Control** - Admin/User dual-role system with fine-grained permissions
- **Family-Level Data Isolation** - Data strictly isolated by family for privacy
- **Password Encryption** - BCrypt encrypted storage to prevent password leaks

### 📊 Data Management
- **Family Management** - Multi-member collaboration with unified financial view
- **Asset Management** - 8 asset types, multi-currency support, automatic exchange rate conversion
- **Liability Management** - 7 liability types with complete tracking and time-series records
- **Income Management** - 10 major income categories, monthly batch entry, annual budget planning ⭐
- **Expense Management** - 10 major expense categories, batch entry, budget planning, 3-level drill-down
- **Investment Management** - Transaction records, cost/market value/P&L calculation, annual analysis
- **Exchange Rate Management** - Multi-currency rate tracking, API integration, historical trends ⭐ New

### 📈 Data Analysis
- **Cash Flow Analysis** - Income/expense comparison, savings rate trends, monthly detail analysis ⭐ New
- **Asset Allocation** - Multi-dimensional analysis by member/currency/tax status (optimized: <2s) ⚡
- **Trend Analysis** - Net worth trends, individual asset P&L tracking
- **Financial Metrics** - Total assets, net worth, debt-to-asset ratio, liquidity ratio
- **Google Sheets Export** - Annual financial reports, async export with real-time progress

### 🗄️ Database Backup & Restore
- **Automated Backups** - Scheduled daily/weekly/monthly backups with configurable retention
- **Manual Backups** - Trigger on-demand backups anytime via admin panel
- **Restore with Verification** - Database name confirmation required before restore
- **Backup Monitoring** - Disk usage tracking, backup history, and comprehensive logs
- **Docker-based Service** - Isolated backup container with health checks

## 🎯 System Highlights

### Data Integrity
- Complete coverage of five dimensions: assets, liabilities, income, expenses, investments
- Complete time-series data recording and historical tracking
- Multi-currency support (USD/CNY/EUR/GBP/JPY/AUD/CAD) with automatic rate conversion
- Third-party API integration (Frankfurter/ECB) for real-time exchange rates

### Performance Excellence ⚡
- Fixed N+1 query issues (batch query optimization)
- Page load time reduced from 10-30 seconds to <2 seconds
- Optimized repository layer batch query methods

### Smart Analysis 📊
- Cash flow health assessment (savings rate, monthly balance)
- Intelligent investment return exclusion (focus on actual cash flow)
- Multi-dimensional visualization (Chart.js charts)

### Security & Reliability 🔒
- JWT authentication + family-level data isolation
- Account-level permission verification (User → Family relationship chain)
- Admin global access, regular users limited to their family data
- Password BCrypt encrypted storage

### Usability
- Responsive design, mobile-friendly
- Batch entry for improved efficiency
- Smart number formatting

## 🛠️ Technology Stack

- **Backend**: Java 17 + Spring Boot 3.2 + MySQL 8.0 + JPA
- **Frontend**: Vue 3 (Composition API) + Tailwind CSS + Chart.js
- **Backup**: Python 3 + Flask + Docker + Cron
- **Deployment**: Kubernetes (Helm) + Docker (Multi-arch: amd64/arm64)
- **Tools**: Maven + Vite + Google Sheets API

## Quick Start

### Local Development

```bash
# 1. Setup database
CREATE DATABASE finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. Configure environment variables
cp backend/.env.example backend/.env
# Edit backend/.env with actual database connection info:
#   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, JWT_SECRET

# 3. Start backend (port 8080)
./backend/start.sh

# 4. Start frontend (port 3000)
cd frontend
npm install
npm run dev
```

### Docker Compose Deployment

```bash
# 1. Configure environment variables
cp .env.example .env
# Edit .env with database connection info and JWT secret

# 2. Start services
docker-compose up -d

# 3. View logs
docker-compose logs -f

# 4. Access application
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080/api
```

### Kubernetes Deployment

```bash
# One-click deployment with Helm (includes MySQL)
cd k8s
./deploy.sh install

# Access application
kubectl port-forward -n finance svc/finance-frontend 3000:80
```

See [k8s/README.md](./k8s/README.md) for details.

## 💱 Exchange Rate Management

Advanced multi-currency exchange rate management with automated API integration and trend visualization.

**Key Features:**

### Tab 1: Daily Rate Manager
- **Date Selection** - Select any date to view/edit exchange rates
- **API Integration** - One-click fetch from Frankfurter API (European Central Bank data)
- **Batch Operations** - Edit all 6 currencies (CNY/EUR/GBP/JPY/AUD/CAD) at once
- **Change Tracking** - Visual indicators for modified rates
- **Manual Override** - Support for manual rate input when needed

### Tab 2: Rate History & Trends
- **Currency Selector** - View historical data for any supported currency
- **Date Range Picker** - Custom time period selection (default: last 7 days)
- **Time-Series Chart** - Interactive Chart.js line chart with real-time scale
- **Statistical Cards** - Latest/Maximum/Minimum/Average rate display
- **Data Table** - Detailed historical records with source tracking

**API Endpoints:**
- `GET /api/exchange-rates/latest` - Get current rates for all currencies
- `GET /api/exchange-rates/date/{date}` - Get rates for specific date
- `GET /api/exchange-rates/currency/{currency}/range` - Date range query
- `POST /api/exchange-rates/fetch-from-api` - Fetch rates from external API (admin)
- `POST /api/exchange-rates` - Create/update rate records (admin)

**Data Source:** Frankfurter API (https://www.frankfurter.app/) - Free, reliable European Central Bank data

## 🗄️ Database Backup System

Automated backup system with scheduled backups, manual triggers, and safe restoration.

**Key Features:**
- Automated daily/weekly/monthly backups
- Manual backup via admin panel
- Safe restore with database name confirmation
- Backup monitoring and logs

See [docs/backup-system.md](./docs/backup-system.md) for complete documentation.

## Data Import Tools

Excel data batch import tool supporting preview, validation, and import of expense and budget data.

```bash
cd import

# 1. Generate preview file
python3 import_from_excel.py preview --year 2024

# 2. Check new records
python3 import_from_excel.py check --year 2024

# 3. Import data
python3 import_from_excel.py import --year 2024

# 4. Clean temporary files
python3 import_from_excel.py clean --year 2024
```

See [import/README.md](./import/README.md) for details.

## Project Structure

```
finance/
├── backend/          # Spring Boot backend
├── frontend/         # Vue.js frontend
├── backup/           # Backup service (Docker)
├── database/         # Database scripts
├── import/           # Excel import tools
├── k8s/              # Kubernetes deployment
└── requirement/      # Requirements documentation
```

## API Documentation

- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **API Details**: [requirement/API文档.md](./requirement/API文档.md)

## Main Endpoints

### Authentication
- `/api/auth/login` - User login (get JWT token)
- `/api/auth/admin/encrypt-passwords` - Password encryption migration (admin)

### Data Management
- `/api/assets/*` - Asset management (accounts, records, batch updates)
- `/api/liabilities/*` - Liability management (accounts, records, batch updates)
- `/api/incomes/*` - Income management (categories, records, batch entry)
- `/api/expenses/*` - Expense management (categories, records, budgets, batch entry)
- `/api/investments/*` - Investment management (transactions, account analysis)
- `/api/family` - Family management (members, switching)
- `/api/backup/*` - Backup management (trigger, list, restore, logs)

### Data Analysis
- `/api/analysis/*` - Comprehensive analysis (trends, allocation, financial metrics)
- `/api/analysis/cashflow` - Cash flow analysis ⭐ New
- `/api/incomes/analysis/*` - Income analysis (annual, by major/minor category)
- `/api/expenses/analysis/*` - Expense analysis (annual, budget comparison)
- `/api/investments/analysis/*` - Investment analysis (annual, by account, monthly trends)

### Tools
- `/api/exchange-rates/*` - Exchange rate management (CRUD, API fetch, date range queries)
- `/api/google-sheets/*` - Google Sheets export

## 📅 Development Roadmap

### ✅ Completed (v1.2.0)
- [x] JWT authentication & authorization system (family-level data isolation)
- [x] Asset & liability management system
- [x] Income management module (10 major categories, budget management)
- [x] Expense management module (10 major categories, budget management)
- [x] Cash flow integration view (income/expense comparison, savings rate trends)
- [x] Investment management & analysis
- [x] Performance optimization (N+1 query fixes)
- [x] Google Sheets export
- [x] Multi-architecture Docker images (amd64/arm64)
- [x] Database backup & restore system
- [x] Enhanced exchange rate management (API integration, historical trends, time-series charts)

### 🔄 In Progress
- [ ] Financial goal management (short/mid/long-term goal setting and tracking)
- [ ] Smart analysis algorithm improvements (risk assessment, optimization suggestions)

### 📝 Planned
- [ ] Cash flow forecasting (predict next 3-6 months based on historical trends)
- [ ] Enhanced investment analysis (IRR, Sharpe ratio, and other advanced metrics)
- [ ] Smart alert system
- [ ] Mobile application

## 📖 Documentation

- **Requirements**: [requirement/需求说明.md](./requirement/需求说明.md) (Chinese)
- **API Documentation**: [requirement/API文档.md](./requirement/API文档.md) (Chinese)
- **Feature Gap Analysis**: [requirement/功能缺口分析.md](./requirement/功能缺口分析.md) (Chinese)
- **Authorization Design**: [docs/authorization-design.md](./docs/authorization-design.md)
- **Frontend Best Practices**: [docs/frontend-best-practices.md](./docs/frontend-best-practices.md)
- **Deployment Guide**: [k8s/README.md](./k8s/README.md)
- **Data Import**: [import/README.md](./import/README.md)
- **Backup System**: [docs/backup-system.md](./docs/backup-system.md)

## 🤝 Contributing

This project uses [Claude Code](https://claude.com/claude-code) for AI-assisted development.

Issues and Pull Requests are welcome!

## 📄 License

MIT License

## 📊 Project Statistics

- **Feature Completion**: 98%
- **Lines of Code**: ~50,000+ (Java + Vue)
- **Database Tables**: 40+
- **API Endpoints**: 100+
- **Supported Currencies**: 7
- **Docker Images**: Multi-architecture support (amd64/arm64)

## 🐳 Multi-Architecture Support

All components support multi-architecture Docker images for both `linux/amd64` and `linux/arm64` platforms, allowing deployment on x86 servers and ARM-based systems (Apple Silicon, ARM servers).
