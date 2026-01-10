# 收入管理模块 - 技术设计文档

## 1. 概述

### 1.1 技术栈

**后端**：
- Java 17
- Spring Boot 3.2
- Spring Data JPA
- MySQL 8.0
- Flyway (数据库版本管理)

**前端**：
- Vue 3 (Composition API)
- Vite
- Tailwind CSS
- Chart.js (数据可视化)
- Axios (HTTP客户端)

### 1.2 架构特点

1. **时间序列设计**：按月记录收入数据，支持时间序列分析
2. **分层架构**：Controller → Service → Repository → Database
3. **多货币支持**：原币记录 + USD转换冗余字段
4. **预聚合优化**：annual_income_summary 表存储预计算数据
5. **投资收益特殊处理**：Investment 大类使用实时计算而非手工记录

### 1.3 与支出模块的差异

| 特性 | 支出模块 | 收入模块 |
|------|---------|---------|
| 记录粒度 | 月度预算 + 实际支出 | 月度汇总 |
| 分类层级 | 大类 + 小类 | 大类 + 小类 |
| 特殊处理 | 无 | Investment 大类实时计算 |
| 年度汇总 | 存储过程 | 存储过程 |
| 预算功能 | 有 | 有（income_budgets表） |

---

## 2. 数据库设计

### 2.1 表结构

#### 2.1.1 收入大类表 (income_categories_major)

```sql
CREATE TABLE income_categories_major (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '英文名称',
    chinese_name VARCHAR(100) NOT NULL COMMENT '中文名称',
    icon VARCHAR(50) COMMENT '图标emoji或类名',
    color VARCHAR(20) COMMENT '颜色代码',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    description TEXT COMMENT '说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**初始数据**（10个预定义大类）：
- Salary (工资, 💼)
- Bonus (奖金, 🎁)
- Investment (投资收益, 📈) - **特殊处理**
- Rental (租金, 🏠)
- Business (经营收入, 💼)
- Freelance (自由职业, 💻)
- Dividend (股息, 💰)
- Interest (利息, 🏦)
- Royalty (版税, 📚)
- Other (其他, 📦)

#### 2.1.2 收入小类表 (income_categories_minor)

```sql
CREATE TABLE income_categories_minor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_category_id BIGINT NOT NULL COMMENT '所属大类ID',
    name VARCHAR(100) NOT NULL COMMENT '小类名称',
    chinese_name VARCHAR(100) COMMENT '中文名称',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    description TEXT COMMENT '说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    UNIQUE KEY uk_major_name (major_category_id, name),
    INDEX idx_major_category (major_category_id),
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**初始数据示例**（30+预定义小类）：
- Salary → Base Salary (基本工资), Year-End Bonus (年终奖), Performance Bonus (绩效奖金)
- Investment → 使用实时计算，无需手工记录小类
- Rental → Residential (住宅租金), Commercial (商业租金)

#### 2.1.3 收入记录表 (income_records)

```sql
CREATE TABLE income_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT NOT NULL COMMENT '记录人ID',
    major_category_id BIGINT NOT NULL COMMENT '大类ID',
    minor_category_id BIGINT NOT NULL COMMENT '小类ID',
    period VARCHAR(7) NOT NULL COMMENT '收入期间(YYYY-MM)',
    amount DECIMAL(18, 2) NOT NULL COMMENT '收入金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币代码',
    amount_usd DECIMAL(18, 2) COMMENT 'USD金额（冗余字段）',
    description TEXT COMMENT '说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    FOREIGN KEY (minor_category_id) REFERENCES income_categories_minor(id),
    UNIQUE KEY uk_family_period_category (family_id, period, major_category_id, minor_category_id, currency),
    INDEX idx_family_period (family_id, period),
    INDEX idx_period (period),
    INDEX idx_major_category (major_category_id),
    INDEX idx_currency (currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**唯一约束说明**：
- 同一家庭、同一期间、同一分类、同一货币只能有一条记录
- 支持同一期间不同货币的记录（如：2025-01 的 USD 和 CNY 记录）

#### 2.1.4 年度收入汇总表 (annual_income_summary)

```sql
CREATE TABLE annual_income_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    summary_year INT NOT NULL COMMENT '汇总年份',
    major_category_id BIGINT NOT NULL COMMENT '大类ID (0=总计)',
    minor_category_id BIGINT COMMENT '小类ID (NULL=大类汇总)',
    actual_income_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '实际收入金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_family_year_category (family_id, summary_year, major_category_id, minor_category_id, currency),
    INDEX idx_family_year (family_id, summary_year),
    INDEX idx_major_category (major_category_id),
    INDEX idx_currency (currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**数据说明**：
- major_category_id = 0：总计行
- minor_category_id = NULL：大类汇总行
- 其他：小类明细行

#### 2.1.5 收入预算表 (income_budgets)

```sql
CREATE TABLE income_budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    budget_year INT NOT NULL COMMENT '预算年份',
    major_category_id BIGINT NOT NULL COMMENT '大类ID',
    minor_category_id BIGINT COMMENT '小类ID',
    budget_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '预算金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型',
    description TEXT COMMENT '说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (major_category_id) REFERENCES income_categories_major(id),
    FOREIGN KEY (minor_category_id) REFERENCES income_categories_minor(id),
    UNIQUE KEY uk_family_year_category (family_id, budget_year, major_category_id, minor_category_id, currency),
    INDEX idx_family_year (family_id, budget_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 索引设计

**查询场景优化**：

1. **按家庭和期间查询**：`idx_family_period` (income_records)
2. **按期间查询**：`idx_period` (income_records)
3. **按大类分组**：`idx_major_category` (多表)
4. **年度汇总查询**：`uk_family_year_category` (annual_income_summary)
5. **货币过滤**：`idx_currency` (多表)

### 2.3 存储过程

#### sp_refresh_annual_income_summary

**功能**：刷新年度收入汇总数据

**参数**：
- `p_family_id` BIGINT：家庭ID
- `p_year` INT：年份
- `p_currency` VARCHAR(10)：货币代码

**逻辑**：
1. 删除旧的汇总数据
2. 从 income_records 汇总计算大类、小类、总计
3. 插入新的汇总数据到 annual_income_summary

**调用方式**：
```sql
CALL sp_refresh_annual_income_summary(1, 2025, 'USD');
```

---

## 3. 后端API设计

### 3.1 实体层 (Entity)

#### 3.1.1 IncomeCategoryMajor

```java
@Entity
@Table(name = "income_categories_major")
@Data
public class IncomeCategoryMajor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "chinese_name", nullable = false, length = 100)
    private String chineseName;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### 3.1.2 IncomeRecord

```java
@Entity
@Table(name = "income_records",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"family_id", "period", "major_category_id",
                      "minor_category_id", "currency"}
    )
)
@Data
public class IncomeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "major_category_id", nullable = false)
    private Long majorCategoryId;

    @Column(name = "minor_category_id", nullable = false)
    private Long minorCategoryId;

    @Column(nullable = false, length = 7)
    private String period;  // YYYY-MM

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "amount_usd", precision = 18, scale = 2)
    private BigDecimal amountUsd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.2 仓储层 (Repository)

#### IncomeCategoryMajorRepository

```java
@Repository
public interface IncomeCategoryMajorRepository
        extends JpaRepository<IncomeCategoryMajor, Long> {

    List<IncomeCategoryMajor> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<IncomeCategoryMajor> findByName(String name);
}
```

#### IncomeRecordRepository

```java
@Repository
public interface IncomeRecordRepository
        extends JpaRepository<IncomeRecord, Long> {

    List<IncomeRecord> findByFamilyIdAndPeriod(Long familyId, String period);

    List<IncomeRecord> findByFamilyIdAndPeriodBetween(
        Long familyId, String startPeriod, String endPeriod
    );

    @Query("SELECT ir FROM IncomeRecord ir WHERE ir.familyId = :familyId " +
           "AND ir.period LIKE CONCAT(:year, '%') AND ir.currency = :currency")
    List<IncomeRecord> findByFamilyIdAndYearAndCurrency(
        @Param("familyId") Long familyId,
        @Param("year") Integer year,
        @Param("currency") String currency
    );
}
```

### 3.3 服务层 (Service)

#### 3.3.1 IncomeRecordService

**核心方法**：

```java
@Service
@Transactional
public class IncomeRecordService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    /**
     * 批量保存收入记录（支持新增和更新）
     */
    public List<IncomeRecord> batchSave(List<IncomeRecord> records) {
        List<IncomeRecord> savedRecords = new ArrayList<>();

        for (IncomeRecord record : records) {
            // 自动设置 USD 金额
            if (!"USD".equals(record.getCurrency())) {
                BigDecimal usdAmount = exchangeRateService.convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    parsePeriodToDate(record.getPeriod())
                );
                record.setAmountUsd(usdAmount);
            } else {
                record.setAmountUsd(record.getAmount());
            }

            // 检查是否已存在记录
            Optional<IncomeRecord> existing = findExisting(record);

            if (existing.isPresent()) {
                if (BigDecimal.ZERO.compareTo(record.getAmount()) == 0) {
                    // 金额为0，删除记录
                    incomeRecordRepository.delete(existing.get());
                } else {
                    // 更新记录
                    IncomeRecord existingRecord = existing.get();
                    existingRecord.setAmount(record.getAmount());
                    existingRecord.setAmountUsd(record.getAmountUsd());
                    existingRecord.setDescription(record.getDescription());
                    savedRecords.add(incomeRecordRepository.save(existingRecord));
                }
            } else if (BigDecimal.ZERO.compareTo(record.getAmount()) != 0) {
                // 新增记录（金额不为0）
                savedRecords.add(incomeRecordRepository.save(record));
            }
        }

        return savedRecords;
    }

    private Optional<IncomeRecord> findExisting(IncomeRecord record) {
        return incomeRecordRepository.findOne(Example.of(record,
            ExampleMatcher.matching()
                .withIgnorePaths("id", "amount", "amountUsd", "description",
                               "createdAt", "updatedAt")
        ));
    }
}
```

#### 3.3.2 IncomeAnalysisService

**关键特性**：Investment 大类使用 InvestmentAnalysisService 实时计算

```java
@Service
public class IncomeAnalysisService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private IncomeCategoryMajorRepository majorCategoryRepository;

    @Autowired
    private InvestmentAnalysisService investmentAnalysisService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 获取年度大类汇总（Investment使用实时计算）
     */
    public List<AnnualMajorCategoryDTO> getAnnualMajorCategories(
            Long familyId, Integer year, String currency) {

        // 1. 获取所有大类
        List<IncomeCategoryMajor> majorCategories =
            majorCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        // 2. 获取收入记录
        List<IncomeRecord> records = incomeRecordRepository
            .findByFamilyIdAndYearAndCurrency(familyId, year, currency);

        // 3. 按大类ID分组求和
        Map<Long, BigDecimal> majorCategoryTotals = records.stream()
            .collect(Collectors.groupingBy(
                IncomeRecord::getMajorCategoryId,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    IncomeRecord::getAmount,
                    BigDecimal::add
                )
            ));

        // 4. 查找Investment大类
        IncomeCategoryMajor investmentCategory =
            majorCategoryRepository.findByName("Investment").orElse(null);

        // 5. 特殊处理：Investment大类使用实时投资回报
        if (investmentCategory != null) {
            try {
                List<InvestmentCategoryAnalysisDTO> investmentAnalysis =
                    investmentAnalysisService.getAnnualByCategory(
                        familyId, year, currency
                    );

                BigDecimal totalInvestmentReturn = investmentAnalysis.stream()
                    .map(dto -> dto.getReturns() != null ?
                        dto.getReturns() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 覆盖Investment大类的金额
                majorCategoryTotals.put(
                    investmentCategory.getId(),
                    totalInvestmentReturn
                );

                log.info("Investment大类使用实时计算: {}", totalInvestmentReturn);
            } catch (Exception e) {
                log.error("获取投资回报失败，使用income_records中的数据", e);
            }
        }

        // 6. 构建DTO
        return majorCategories.stream()
            .map(major -> {
                AnnualMajorCategoryDTO dto = new AnnualMajorCategoryDTO();
                dto.setMajorCategoryId(major.getId());
                dto.setMajorCategoryName(major.getName());
                dto.setMajorCategoryChineseName(major.getChineseName());
                dto.setMajorCategoryIcon(major.getIcon());
                dto.setTotalAmount(
                    majorCategoryTotals.getOrDefault(major.getId(), BigDecimal.ZERO)
                );
                dto.setCurrency(currency);
                return dto;
            })
            .filter(dto -> dto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());
    }

    /**
     * 刷新年度收入汇总（调用存储过程）
     */
    @Transactional
    public void refreshAnnualIncomeSummary(
            Long familyId, Integer year, String currency) {

        entityManager.createNativeQuery(
            "CALL sp_refresh_annual_income_summary(:familyId, :year, :currency)"
        )
        .setParameter("familyId", familyId)
        .setParameter("year", year)
        .setParameter("currency", currency)
        .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        log.info("年度收入汇总刷新完成: familyId={}, year={}, currency={}",
                familyId, year, currency);
    }
}
```

### 3.4 控制器层 (Controller)

#### IncomeAnalysisController

```java
@RestController
@RequestMapping("/api/incomes-analysis")
public class IncomeAnalysisController {

    @Autowired
    private IncomeAnalysisService analysisService;

    /**
     * 获取年度大类汇总
     * GET /api/incomes-analysis/annual/major-categories
     */
    @GetMapping("/annual/major-categories")
    public ApiResponse<List<AnnualMajorCategoryDTO>> getAnnualMajorCategories(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency) {

        List<AnnualMajorCategoryDTO> data =
            analysisService.getAnnualMajorCategories(familyId, year, currency);

        return ApiResponse.success(data);
    }

    /**
     * 获取年度小类汇总
     * GET /api/incomes-analysis/annual/minor-categories
     */
    @GetMapping("/annual/minor-categories")
    public ApiResponse<List<AnnualMinorCategoryDTO>> getAnnualMinorCategories(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {

        List<AnnualMinorCategoryDTO> data =
            analysisService.getAnnualMinorCategories(
                familyId, year, majorCategoryId, currency
            );

        return ApiResponse.success(data);
    }

    /**
     * 获取年度月度趋势
     * GET /api/incomes-analysis/annual/monthly-trend
     */
    @GetMapping("/annual/monthly-trend")
    public ApiResponse<List<MonthlyTrendDTO>> getAnnualMonthlyTrend(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam Long minorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {

        List<MonthlyTrendDTO> data =
            analysisService.getAnnualMonthlyTrend(
                familyId, year, majorCategoryId, minorCategoryId, currency
            );

        return ApiResponse.success(data);
    }

    /**
     * 刷新年度收入汇总
     * POST /api/incomes-analysis/annual/refresh
     */
    @PostMapping("/annual/refresh")
    public ApiResponse<Void> refreshAnnualSummary(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "All") String currency) {

        analysisService.refreshAnnualIncomeSummary(familyId, year, currency);

        return ApiResponse.success(null);
    }
}
```

---

## 4. 前端设计

### 4.1 API封装 (api/income.js)

```javascript
import request from '@/utils/request'

// 收入分类管理
export const incomeCategoryAPI = {
  // 获取所有分类（大类+小类树形结构）
  getAll() {
    return request.get('/incomes-categories')
  },

  // 新增大类
  createMajor(data) {
    return request.post('/incomes-categories/major', data)
  },

  // 新增小类
  createMinor(data) {
    return request.post('/incomes-categories/minor', data)
  }
}

// 收入记录管理
export const incomeRecordAPI = {
  // 批量保存收入记录
  batchSave(records) {
    return request.post('/incomes/batch', records)
  },

  // 查询收入记录（按期间）
  getByPeriod(familyId, period) {
    return request.get('/incomes/period', {
      params: { familyId, period }
    })
  },

  // 查询收入记录（按期间范围）
  getByPeriodRange(familyId, startPeriod, endPeriod) {
    return request.get('/incomes/period-range', {
      params: { familyId, startPeriod, endPeriod }
    })
  }
}

// 收入分析
export const incomeAnalysisAPI = {
  // 获取年度大类汇总
  getAnnualMajorCategories(familyId, year, currency = 'USD') {
    return request.get('/incomes-analysis/annual/major-categories', {
      params: { familyId, year, currency }
    })
  },

  // 获取年度小类汇总
  getAnnualMinorCategories(familyId, year, majorCategoryId, currency = 'USD') {
    return request.get('/incomes-analysis/annual/minor-categories', {
      params: { familyId, year, majorCategoryId, currency }
    })
  },

  // 获取年度月度趋势
  getAnnualMonthlyTrend(familyId, year, majorCategoryId, minorCategoryId, currency = 'USD') {
    return request.get('/incomes-analysis/annual/monthly-trend', {
      params: { familyId, year, majorCategoryId, minorCategoryId, currency }
    })
  },

  // 刷新年度收入汇总数据
  refreshAnnualSummary(familyId, year, currency = 'All') {
    return request.post('/incomes-analysis/annual/refresh', null, {
      params: { familyId, year, currency }
    })
  }
}
```

### 4.2 核心组件

#### 4.2.1 IncomeBatchUpdate.vue（批量录入）

**功能**：按月批量录入收入记录

**技术要点**：
- 表格形式展示所有小类
- 显示前3个月历史数据供参考
- 实时计算本月总收入
- 支持同一期间+同一分类+同一货币的记录更新

**代码片段**：
```vue
<template>
  <div class="batch-update">
    <!-- 选择器 -->
    <div class="controls">
      <select v-model="selectedFamilyId">...</select>
      <select v-model="selectedCurrency">...</select>
      <input type="month" v-model="selectedPeriod" />
      <button @click="saveBatch">保存全部</button>
    </div>

    <!-- 统计汇总 -->
    <div class="summary">
      <div>11月总收入: {{ formatCurrency(history[0].total) }}</div>
      <div>12月总收入: {{ formatCurrency(history[1].total) }}</div>
      <div>01月总收入: {{ formatCurrency(history[2].total) }}</div>
      <div>本月总收入: {{ formatCurrency(currentTotal) }}</div>
    </div>

    <!-- 录入表格 -->
    <table>
      <thead>
        <tr>
          <th>分类</th>
          <th>11月</th>
          <th>12月</th>
          <th>01月</th>
          <th>本月金额</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="category in categories" :key="category.id">
          <td>{{ category.icon }} {{ category.name }}</td>
          <td>{{ formatCurrency(getHistory(category, 0)) }}</td>
          <td>{{ formatCurrency(getHistory(category, 1)) }}</td>
          <td>{{ formatCurrency(getHistory(category, 2)) }}</td>
          <td>
            <input
              type="number"
              v-model="category.amount"
              @input="calculateTotal"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { incomeRecordAPI, incomeCategoryAPI } from '@/api/income'

const selectedFamilyId = ref(null)
const selectedCurrency = ref('USD')
const selectedPeriod = ref(getCurrentPeriod())
const categories = ref([])
const history = ref([])

const currentTotal = computed(() => {
  return categories.value.reduce((sum, cat) =>
    sum + (parseFloat(cat.amount) || 0), 0
  )
})

async function loadCategories() {
  const response = await incomeCategoryAPI.getAll()
  categories.value = response.data.flatMap(major =>
    major.minorCategories.map(minor => ({
      majorId: major.id,
      minorId: minor.id,
      icon: major.icon,
      name: `${major.chineseName} - ${minor.chineseName}`,
      amount: 0
    }))
  )
}

async function saveBatch() {
  const records = categories.value
    .filter(cat => parseFloat(cat.amount) > 0)
    .map(cat => ({
      familyId: selectedFamilyId.value,
      userId: 1, // TODO: 从登录状态获取
      majorCategoryId: cat.majorId,
      minorCategoryId: cat.minorId,
      period: selectedPeriod.value,
      amount: parseFloat(cat.amount),
      currency: selectedCurrency.value
    }))

  await incomeRecordAPI.batchSave(records)
  alert('保存成功！')
}

onMounted(() => {
  loadCategories()
  // loadHistory() - 加载前3个月历史数据
})
</script>
```

#### 4.2.2 IncomeAnnual.vue（年度分析）

**功能**：年度收入分析（大类分布 + 小类钻取 + 月度趋势）

**技术要点**：
- Chart.js 饼图展示大类占比
- 点击大类钻取到小类分布
- 点击小类展示月度趋势柱状图
- 支持同比增长率计算

**组件结构**：
```vue
<template>
  <div class="income-annual">
    <!-- 筛选控制 -->
    <div class="controls">
      <select v-model="selectedYear">...</select>
      <select v-model="selectedCurrency">...</select>
      <button @click="refreshData">刷新数据</button>
    </div>

    <!-- 收入总览 -->
    <div class="summary-cards">
      <div class="card">总收入: {{ formatCurrency(totalIncome) }}</div>
      <div class="card">平均月收入: {{ formatCurrency(totalIncome / 12) }}</div>
      <div class="card">同比增长: {{ yearOverYearGrowth }}%</div>
    </div>

    <!-- 大类分布 -->
    <div class="major-category-section">
      <div class="chart">
        <canvas ref="majorCategoryChart"></canvas>
      </div>
      <table class="category-table">
        <tr v-for="cat in majorCategoryData"
            @click="selectMajorCategory(cat)"
            :class="{ selected: selectedMajorCategoryId === cat.id }">
          <td>{{ cat.icon }} {{ cat.name }}</td>
          <td>{{ formatCurrency(cat.amount) }}</td>
          <td>{{ cat.percentage }}%</td>
          <td>{{ cat.yoyGrowth }}%</td>
        </tr>
      </table>
    </div>

    <!-- 小类分布（钻取） -->
    <div v-if="selectedMajorCategoryId" class="minor-category-section">
      <h3>{{ selectedMajorCategoryName }} - 小类分布</h3>
      <canvas ref="minorCategoryChart"></canvas>
      <table>...</table>
    </div>

    <!-- 月度趋势（钻取） -->
    <div v-if="selectedMinorCategoryId" class="monthly-trend-section">
      <h3>{{ selectedMinorCategoryName }} - 月度趋势</h3>
      <canvas ref="monthlyTrendChart"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Chart } from 'chart.js'
import { incomeAnalysisAPI } from '@/api/income'

const selectedYear = ref(new Date().getFullYear())
const majorCategoryData = ref([])
const selectedMajorCategoryId = ref(null)

async function loadMajorCategoryData() {
  const response = await incomeAnalysisAPI.getAnnualMajorCategories(
    selectedFamilyId.value,
    selectedYear.value,
    selectedCurrency.value
  )
  majorCategoryData.value = response.data
  updateMajorCategoryChart()
}

function updateMajorCategoryChart() {
  const ctx = majorCategoryChart.value.getContext('2d')
  new Chart(ctx, {
    type: 'pie',
    data: {
      labels: majorCategoryData.value.map(d => d.name),
      datasets: [{
        data: majorCategoryData.value.map(d => d.amount),
        backgroundColor: ['#10b981', '#3b82f6', '#f59e0b', ...]
      }]
    },
    options: {
      plugins: {
        datalabels: {
          formatter: (value, context) => {
            const percentage = ((value / totalIncome.value) * 100).toFixed(1)
            return percentage > 5 ? `${percentage}%` : ''
          }
        }
      }
    }
  })
}

async function selectMajorCategory(category) {
  selectedMajorCategoryId.value = category.id
  selectedMajorCategoryName.value = category.name

  const response = await incomeAnalysisAPI.getAnnualMinorCategories(
    selectedFamilyId.value,
    selectedYear.value,
    category.id,
    selectedCurrency.value
  )
  minorCategoryData.value = response.data
  updateMinorCategoryChart()
}

onMounted(() => {
  loadMajorCategoryData()
})

watch([selectedYear, selectedCurrency], () => {
  loadMajorCategoryData()
})
</script>
```

### 4.3 路由配置

```javascript
// router/index.js
const routes = [
  {
    path: '/incomes',
    component: Layout,
    meta: { title: '收入管理' },
    children: [
      {
        path: 'batch-update',
        component: () => import('@/views/incomes/IncomeBatchUpdate.vue'),
        meta: { title: '批量录入收入' }
      },
      {
        path: 'categories',
        component: () => import('@/views/incomes/IncomeCategories.vue'),
        meta: { title: '收入分类管理' }
      }
    ]
  },
  {
    path: '/analysis',
    component: Layout,
    meta: { title: '数据分析' },
    children: [
      {
        path: 'income-annual',
        component: () => import('@/views/analysis/IncomeAnnual.vue'),
        meta: { title: '年度收入分析' }
      }
    ]
  }
]
```

---

## 5. 性能优化

### 5.1 数据库层优化

1. **索引优化**：
   - 复合索引 `idx_family_period` 支持按家庭和期间快速查询
   - `uk_family_year_category` 唯一索引避免重复数据
   - `idx_major_category` 支持按大类分组聚合

2. **预聚合表**：
   - `annual_income_summary` 存储预计算的年度汇总
   - 减少实时查询 `income_records` 表的复杂聚合
   - 存储过程批量计算提高效率

3. **冗余字段**：
   - `amount_usd` 字段避免实时汇率转换
   - 批量保存时自动计算并存储

### 5.2 服务层优化

1. **批量操作**：
   ```java
   @Transactional
   public List<IncomeRecord> batchSave(List<IncomeRecord> records) {
       // 单次事务处理多条记录
       // 减少数据库往返次数
   }
   ```

2. **Investment特殊处理**：
   ```java
   // 使用InvestmentAnalysisService实时计算
   // 避免手工维护投资收益记录
   BigDecimal totalInvestmentReturn = investmentAnalysisService
       .getAnnualByCategory(familyId, year, currency)
       .stream()
       .map(InvestmentCategoryAnalysisDTO::getReturns)
       .reduce(BigDecimal.ZERO, BigDecimal::add);
   ```

3. **存储过程调用**：
   ```java
   // 使用存储过程批量刷新汇总数据
   // 比Java代码逐条计算快10-100倍
   entityManager.createNativeQuery(
       "CALL sp_refresh_annual_income_summary(:familyId, :year, :currency)"
   ).executeUpdate();
   ```

### 5.3 前端优化

1. **懒加载路由**：
   ```javascript
   component: () => import('@/views/analysis/IncomeAnnual.vue')
   ```

2. **图表按需渲染**：
   ```javascript
   // 只在用户选择大类后才渲染小类饼图
   watch(selectedMajorCategoryId, () => {
     if (selectedMajorCategoryId.value) {
       updateMinorCategoryChart()
     }
   })
   ```

3. **防抖保存**：
   ```javascript
   const debouncedSave = debounce(async () => {
     await incomeRecordAPI.batchSave(records)
   }, 1000)
   ```

---

## 6. 错误处理

### 6.1 后端异常处理

#### 自定义异常

```java
public class IncomeRecordException extends RuntimeException {
    private String errorCode;

    public IncomeRecordException(String message) {
        super(message);
    }

    public IncomeRecordException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

#### 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncomeRecordException.class)
    public ApiResponse<Void> handleIncomeRecordException(
            IncomeRecordException ex) {
        log.error("收入记录异常: {}", ex.getMessage(), ex);
        return ApiResponse.error(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Void> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        log.error("数据完整性异常: {}", ex.getMessage(), ex);
        return ApiResponse.error("DATA_INTEGRITY_ERROR",
            "违反唯一约束，请检查是否重复记录");
    }
}
```

### 6.2 前端错误处理

#### Axios拦截器

```javascript
// utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    if (response.data.success) {
      return response.data
    } else {
      throw new Error(response.data.message || '请求失败')
    }
  },
  error => {
    console.error('请求错误:', error)

    if (error.response?.status === 401) {
      // 跳转登录页
      router.push('/login')
    } else if (error.response?.status === 500) {
      alert('服务器内部错误，请稍后重试')
    } else {
      alert(error.message || '网络错误')
    }

    return Promise.reject(error)
  }
)
```

#### 组件错误边界

```vue
<script setup>
import { ref, onErrorCaptured } from 'vue'

const error = ref(null)

onErrorCaptured((err, instance, info) => {
  console.error('组件错误:', err, info)
  error.value = err.message
  return false // 阻止错误向上传播
})
</script>

<template>
  <div v-if="error" class="error-boundary">
    <p>发生错误: {{ error }}</p>
    <button @click="() => error = null">重试</button>
  </div>
  <slot v-else />
</template>
```

---

## 7. 测试

### 7.1 单元测试

#### Service层测试

```java
@SpringBootTest
@Transactional
class IncomeAnalysisServiceTest {

    @Autowired
    private IncomeAnalysisService analysisService;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Test
    void testGetAnnualMajorCategories() {
        // Given
        Long familyId = 1L;
        Integer year = 2025;
        String currency = "USD";

        // 准备测试数据
        IncomeRecord record = new IncomeRecord();
        record.setFamilyId(familyId);
        record.setPeriod("2025-01");
        record.setMajorCategoryId(1L);
        record.setMinorCategoryId(1L);
        record.setAmount(new BigDecimal("5000"));
        record.setCurrency(currency);
        incomeRecordRepository.save(record);

        // When
        List<AnnualMajorCategoryDTO> result =
            analysisService.getAnnualMajorCategories(familyId, year, currency);

        // Then
        assertNotNull(result);
        assertTrue(result.size() > 0);

        AnnualMajorCategoryDTO firstCategory = result.get(0);
        assertEquals(new BigDecimal("5000"), firstCategory.getTotalAmount());
    }

    @Test
    void testInvestmentCategorySpecialHandling() {
        // Given
        Long familyId = 1L;
        Integer year = 2025;

        // Mock InvestmentAnalysisService
        // 验证Investment大类使用实时计算

        // When
        List<AnnualMajorCategoryDTO> result =
            analysisService.getAnnualMajorCategories(familyId, year, "USD");

        // Then
        Optional<AnnualMajorCategoryDTO> investmentCategory = result.stream()
            .filter(dto -> "Investment".equals(dto.getMajorCategoryName()))
            .findFirst();

        assertTrue(investmentCategory.isPresent());
        // 验证金额来自InvestmentAnalysisService而非income_records
    }
}
```

### 7.2 集成测试

#### Controller层测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class IncomeAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAnnualMajorCategoriesEndpoint() throws Exception {
        mockMvc.perform(get("/api/incomes-analysis/annual/major-categories")
                .param("familyId", "1")
                .param("year", "2025")
                .param("currency", "USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testRefreshAnnualSummary() throws Exception {
        mockMvc.perform(post("/api/incomes-analysis/annual/refresh")
                .param("familyId", "1")
                .param("year", "2025")
                .param("currency", "USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

### 7.3 前端测试（待完善）

```javascript
// tests/unit/IncomeAnnual.spec.js
import { mount } from '@vue/test-utils'
import IncomeAnnual from '@/views/analysis/IncomeAnnual.vue'

describe('IncomeAnnual.vue', () => {
  it('renders major category data', async () => {
    const wrapper = mount(IncomeAnnual, {
      data() {
        return {
          majorCategoryData: [
            { id: 1, name: 'Salary', amount: 80000 },
            { id: 2, name: 'Investment', amount: 15000 }
          ]
        }
      }
    })

    expect(wrapper.text()).toContain('Salary')
    expect(wrapper.text()).toContain('Investment')
  })
})
```

---

## 8. 部署

### 8.1 数据库迁移

**Flyway版本控制**：

```
backend/src/main/resources/db/migration/
  V16__create_income_tables.sql
  V17__create_income_stored_procedures.sql
```

**迁移步骤**：
1. Spring Boot启动时自动执行Flyway迁移
2. 检查 `flyway_schema_history` 表确认迁移成功
3. 验证表结构和初始数据

### 8.2 环境配置

**backend/.env**:
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=finance
DB_USER=finance_user
DB_PASSWORD=your_password
```

**frontend/.env.production**:
```properties
VITE_API_BASE_URL=/api
```

### 8.3 打包部署

**后端**:
```bash
cd backend
mvn clean package -DskipTests
java -jar target/finance-backend-1.0.0.jar
```

**前端**:
```bash
cd frontend
npm run build
# 部署 dist/ 目录到Nginx或其他静态服务器
```

### 8.4 监控和日志

**应用日志**:
```java
@Slf4j
public class IncomeAnalysisService {
    public void refreshAnnualIncomeSummary(...) {
        log.info("开始刷新年度收入汇总: familyId={}, year={}", familyId, year);
        // ...
        log.info("年度收入汇总刷新完成");
    }
}
```

**性能监控**:
- Spring Boot Actuator: `/actuator/health`, `/actuator/metrics`
- 数据库慢查询日志: `slow_query_log = ON`

---

## 9. 常见问题 (FAQ)

### Q1: Investment大类为什么使用实时计算？

**A**: 投资收益具有波动性，需要根据最新的资产价格和汇率计算。手工记录无法及时反映市场变化，因此通过 `InvestmentAnalysisService` 实时计算更准确。

### Q2: 如何处理同一期间多货币记录？

**A**: 系统支持同一期间多货币记录。唯一约束包含 `currency` 字段，因此可以同时记录 USD、CNY、EUR 等不同货币的收入。分析时可选择单一货币或 'All' 模式（自动转USD）。

### Q3: 批量保存时如何处理重复记录？

**A**: `batchSave` 方法会检查唯一约束（family_id + period + category + currency）：
- 如果已存在：更新金额
- 如果金额为0：删除记录
- 如果不存在且金额不为0：新增记录

### Q4: 年度汇总刷新的时机？

**A**: 建议在以下情况刷新：
- 批量录入收入记录后
- 修改历史收入数据后
- 定时任务每日凌晨自动刷新（可选）

### Q5: 如何扩展新的大类？

**A**:
1. 使用 `IncomeCategoryAPI.createMajor()` 添加新大类
2. 为新大类添加小类
3. 新大类会自动包含在分析和报表中
4. 如需特殊处理（如Investment），修改 `IncomeAnalysisService` 添加逻辑

---

## 10. 未来扩展

### 10.1 智能分析

- **收入预测**：基于历史数据预测未来收入趋势
- **异常检测**：识别异常收入波动并提醒
- **收入来源优化建议**：根据收入结构提供多元化建议

### 10.2 多维度分析

- **按成员分析**：支持家庭成员维度的收入分析
- **同期对比**：多年度同期对比分析
- **与支出关联分析**：收支平衡和储蓄率分析

### 10.3 第三方集成

- **银行流水导入**：自动识别收入类型并导入
- **工资单OCR**：扫描工资单自动录入
- **投资账户同步**：自动同步券商账户数据计算投资收益

---

## 附录

### A. API端点汇总

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/incomes-categories` | GET | 获取所有分类 |
| `/api/incomes-categories/major` | POST | 新增大类 |
| `/api/incomes-categories/minor` | POST | 新增小类 |
| `/api/incomes/batch` | POST | 批量保存记录 |
| `/api/incomes/period` | GET | 按期间查询 |
| `/api/incomes-analysis/annual/major-categories` | GET | 年度大类汇总 |
| `/api/incomes-analysis/annual/minor-categories` | GET | 年度小类汇总 |
| `/api/incomes-analysis/annual/monthly-trend` | GET | 年度月度趋势 |
| `/api/incomes-analysis/annual/refresh` | POST | 刷新年度汇总 |

### B. 数据库表汇总

| 表名 | 说明 | 行数估算 |
|------|------|---------|
| `income_categories_major` | 大类 | 10-20 |
| `income_categories_minor` | 小类 | 30-100 |
| `income_records` | 收入记录 | 1000+ |
| `annual_income_summary` | 年度汇总 | 100+ |
| `income_budgets` | 预算 | 50+ |

### C. 组件汇总

| 组件 | 路径 | 功能 |
|------|------|------|
| IncomeBatchUpdate | `/incomes/batch-update` | 批量录入 |
| IncomeCategories | `/incomes/categories` | 分类管理 |
| IncomeAnnual | `/analysis/income-annual` | 年度分析 |
| Dashboard | `/` | 首页（包含收入卡片） |
