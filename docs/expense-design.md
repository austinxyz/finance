# 支出管理模块 - 设计文档

## 1. 概述

本文档详细描述支出管理模块的技术实现，包括数据库设计、后端API设计和前端组件设计。

## 2. 数据库设计

### 2.1 表结构设计

#### 2.1.1 expense_categories_major（支出大类表）

**用途**：存储固定的9个大类

```sql
CREATE TABLE expense_categories_major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  code VARCHAR(50) NOT NULL UNIQUE COMMENT '大类编码',
  name VARCHAR(50) NOT NULL COMMENT '大类名称',
  icon VARCHAR(50) COMMENT '图标',
  color VARCHAR(20) COMMENT '颜色代码',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_is_active (is_active),
  KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出大类表';
```

**索引设计**：
- `idx_is_active`: 快速筛选启用的大类
- `idx_sort_order`: 排序查询优化

**初始化数据**：
```sql
INSERT INTO expense_categories_major (code, name, icon, color, sort_order) VALUES
('CHILDREN',        '子女',   '👶', '#FF6B9D', 1),
('CLOTHING',        '衣',     '👔', '#C0B4D5', 2),
('FOOD',            '食',     '🍔', '#FFD93D', 3),
('HOUSING',         '住',     '🏠', '#6BCB77', 4),
('TRANSPORTATION',  '行',     '🚗', '#4D96FF', 5),
('INSURANCE',       '保险',   '🛡️', '#95E1D3', 6),
('SOCIAL',          '人情',   '🎁', '#FFA07A', 7),
('ENTERTAINMENT',   '娱乐',   '🎮', '#F38BA0', 8),
('BUSINESS',        '经营',   '💼', '#A8DADC', 9);
```

#### 2.1.2 expense_categories_minor（支出子分类表）

**用途**：存储各大类下的子分类，可自定义

```sql
CREATE TABLE expense_categories_minor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  major_category_id BIGINT NOT NULL COMMENT '所属大类ID',
  name VARCHAR(100) NOT NULL COMMENT '子分类名称',
  expense_type VARCHAR(20) NOT NULL COMMENT '支出类型',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否系统默认分类',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (major_category_id) REFERENCES expense_categories_major(id),
  UNIQUE KEY uk_major_name (major_category_id, name),
  KEY idx_is_active (is_active),
  KEY idx_major_category (major_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出子分类表';
```

**约束说明**：
- `uk_major_name`: 同一大类下子分类名称唯一
- 外键关联大类表

**expense_type 枚举值**：
- `FIXED_DAILY`: 固定日常支出
- `LARGE_IRREGULAR`: 不定期大额支出

#### 2.1.3 expense_records（支出记录表）

**用途**：存储月度支出记录（月度汇总，非流水账）

```sql
CREATE TABLE expense_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  family_id BIGINT NOT NULL COMMENT '家庭ID',
  user_id BIGINT NOT NULL COMMENT '记录人ID',

  -- 期间与分类
  expense_year INT NOT NULL COMMENT '支出年份',
  expense_month INT NOT NULL COMMENT '支出月份',
  expense_period VARCHAR(7) NOT NULL COMMENT '支出期间（YYYY-MM）',
  major_category_id BIGINT NOT NULL COMMENT '大类ID',
  minor_category_id BIGINT NOT NULL COMMENT '子分类ID',

  -- 金额
  amount DECIMAL(18, 2) NOT NULL COMMENT '支出金额',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币代码',
  amount_in_base_currency DECIMAL(18, 2) COMMENT '基准货币金额（后端计算）',

  -- 类型与说明
  expense_type VARCHAR(20) NOT NULL COMMENT '支出类型',
  description TEXT COMMENT '说明',

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (family_id) REFERENCES families(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (major_category_id) REFERENCES expense_categories_major(id),
  FOREIGN KEY (minor_category_id) REFERENCES expense_categories_minor(id),

  UNIQUE KEY uk_period_category (family_id, expense_period, minor_category_id, currency),
  KEY idx_family_period (family_id, expense_year, expense_month),
  KEY idx_expense_period (expense_period),
  KEY idx_major_category (major_category_id),
  KEY idx_minor_category (minor_category_id),
  KEY idx_expense_type (expense_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出记录表';
```

**关键约束**：
- `uk_period_category`: 保证同一期间同一分类同一货币只有一条记录（支持多货币）

**索引设计**：
- `idx_family_period`: 查询某家庭某月支出
- `idx_expense_period`: 时间范围查询
- `idx_minor_category`: 查询某分类历史记录
- `idx_expense_type`: 固定/大额筛选

#### 2.1.4 expense_budgets（支出预算表）

**用途**：为每个家庭的每个支出子分类设定年度预算

```sql
CREATE TABLE expense_budgets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  family_id BIGINT NOT NULL COMMENT '家庭ID',
  budget_year INT NOT NULL COMMENT '预算年份（如2025）',
  minor_category_id BIGINT NOT NULL COMMENT '子分类ID',
  budget_amount DECIMAL(15,2) NOT NULL COMMENT '预算金额',
  currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型',
  notes VARCHAR(500) COMMENT '备注说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uk_family_year_category_currency
    UNIQUE (family_id, budget_year, minor_category_id, currency),
  CONSTRAINT fk_budget_family
    FOREIGN KEY (family_id) REFERENCES families(id),
  CONSTRAINT fk_budget_minor_category
    FOREIGN KEY (minor_category_id) REFERENCES expense_categories_minor(id),

  INDEX idx_family_year (family_id, budget_year),
  INDEX idx_minor_category (minor_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支出年度预算表';
```

#### 2.1.5 annual_expense_summary（年度支出汇总表）

**用途**：存储预计算的年度支出汇总（包含资产/负债调整）

```sql
CREATE TABLE annual_expense_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  family_id BIGINT NOT NULL,
  summary_year INT NOT NULL,
  major_category_id BIGINT COMMENT '大类ID，0表示总计',
  minor_category_id BIGINT COMMENT '子分类ID，NULL表示大类汇总',
  base_expense_amount DECIMAL(18,2) DEFAULT 0 COMMENT '基础支出金额',
  asset_adjustment DECIMAL(18,2) DEFAULT 0 COMMENT '资产调整金额',
  liability_adjustment DECIMAL(18,2) DEFAULT 0 COMMENT '负债调整金额',
  actual_expense_amount DECIMAL(18,2) DEFAULT 0 COMMENT '实际支出金额',
  currency VARCHAR(10) NOT NULL COMMENT '货币类型',
  adjustment_details JSON COMMENT '调整明细',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_family_year (family_id, summary_year),
  KEY idx_major_category (major_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度支出汇总表';
```

**字段说明**：
- `base_expense_amount`: 支出记录表直接汇总的金额
- `asset_adjustment`: 增加资产价值的支出（如装修），需要从实际支出中扣除
- `liability_adjustment`: 本金还款（减少负债），需要从实际支出中扣除
- `actual_expense_amount`: 真实的消费支出 = base - asset - liability
- `adjustment_details`: JSON格式存储明细，便于审计

### 2.2 存储过程

#### calculate_annual_expense_summary_v2

**用途**：计算年度支出汇总（包含资产/负债调整）

**调用方式**：
```sql
CALL calculate_annual_expense_summary_v2(1, 2025);
-- 参数：familyId, year
```

**核心逻辑**：
1. 删除旧数据
2. 汇总支出记录表数据（按大类、子分类）
3. 关联资产记录查找资产调整
4. 关联负债记录查找负债调整
5. 计算实际支出金额
6. 插入汇总结果

## 3. 后端API设计

### 3.1 技术栈

- **框架**: Spring Boot 3.x
- **ORM**: Spring Data JPA + Hibernate
- **数据库**: MySQL 8.0
- **语言**: Java 17

### 3.2 实体模型

#### ExpenseCategoryMajor

```java
@Entity
@Table(name = "expense_categories_major")
public class ExpenseCategoryMajor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Boolean isActive;
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### ExpenseCategoryMinor

```java
@Entity
@Table(name = "expense_categories_minor")
public class ExpenseCategoryMinor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long majorCategoryId;
    private String name;
    private String expenseType; // FIXED_DAILY, LARGE_IRREGULAR
    private Boolean isActive;
    private Boolean isDefault;
    private Integer sortOrder;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMajor majorCategory;
}
```

#### ExpenseRecord

```java
@Entity
@Table(name = "expense_records")
public class ExpenseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long familyId;
    private Long userId;
    private Integer expenseYear;
    private Integer expenseMonth;
    private String expensePeriod; // YYYY-MM
    private Long majorCategoryId;
    private Long minorCategoryId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal amountInBaseCurrency;
    private String expenseType;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMajor majorCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "minor_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMinor minorCategory;
}
```

### 3.3 Repository层

#### ExpenseRecordRepository

```java
@Repository
public interface ExpenseRecordRepository
    extends JpaRepository<ExpenseRecord, Long> {

    // 查询某家庭某期间的所有支出
    List<ExpenseRecord> findByFamilyIdAndExpensePeriod(
        Long familyId, String period
    );

    // 查询某家庭期间范围内的所有支出
    @Query("SELECT e FROM ExpenseRecord e " +
           "WHERE e.familyId = :familyId " +
           "AND e.expensePeriod >= :startPeriod " +
           "AND e.expensePeriod <= :endPeriod")
    List<ExpenseRecord> findByFamilyIdAndPeriodRange(
        @Param("familyId") Long familyId,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    // 查询某家庭某期间某分类的支出（支持多货币）
    Optional<ExpenseRecord> findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(
        Long familyId, String period, Long minorCategoryId, String currency
    );

    // 查询某家庭某期间内某分类的所有支出
    List<ExpenseRecord> findByFamilyIdAndExpensePeriodAndMinorCategoryId(
        Long familyId, String period, Long minorCategoryId
    );
}
```

### 3.4 Service层

#### ExpenseService

**职责**：分类管理、支出记录管理

**核心方法**：

```java
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseCategoryMajorRepository majorCategoryRepository;
    private final ExpenseCategoryMinorRepository minorCategoryRepository;
    private final ExpenseRecordRepository expenseRecordRepository;

    // ==================== 分类管理 ====================

    /**
     * 获取所有大类及其子分类（树形结构）
     */
    public List<ExpenseCategoryDTO> getAllCategories() {
        List<ExpenseCategoryMajor> majors = majorCategoryRepository
            .findAllByOrderBySortOrder();

        return majors.stream().map(major -> {
            List<ExpenseCategoryMinor> minors = minorCategoryRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());

            // 转换为DTO，包含recordCount
            List<MinorCategoryDTO> minorDTOs = minors.stream()
                .map(minor -> {
                    long recordCount = minorCategoryRepository
                        .countExpenseRecords(minor.getId());
                    return buildMinorCategoryDTO(minor, recordCount);
                })
                .collect(Collectors.toList());

            return buildCategoryDTO(major, minorDTOs);
        }).collect(Collectors.toList());
    }

    /**
     * 创建子分类
     */
    @Transactional
    public MinorCategoryDTO createMinorCategory(CreateMinorCategoryRequest request) {
        // 检查大类是否存在
        ExpenseCategoryMajor major = majorCategoryRepository
            .findById(request.getMajorCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("大类不存在"));

        // 检查名称是否重复
        Optional<ExpenseCategoryMinor> existing = minorCategoryRepository
            .findByMajorCategoryIdAndName(
                request.getMajorCategoryId(),
                request.getName()
            );

        if (existing.isPresent()) {
            throw new IllegalArgumentException("该大类下已存在同名子分类");
        }

        ExpenseCategoryMinor minor = new ExpenseCategoryMinor();
        minor.setMajorCategoryId(request.getMajorCategoryId());
        minor.setName(request.getName());
        minor.setExpenseType(request.getExpenseType());
        minor.setSortOrder(request.getSortOrder());
        minor.setDescription(request.getDescription());
        minor.setIsActive(true);
        minor.setIsDefault(false);

        return toDTO(minorCategoryRepository.save(minor));
    }

    /**
     * 停用子分类（软删除）
     */
    @Transactional
    public void disableMinorCategory(Long id) {
        ExpenseCategoryMinor minor = minorCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子分类不存在"));

        boolean hasRecords = minorCategoryRepository.hasExpenseRecords(id);

        if (hasRecords) {
            // 有记录，只能停用
            minor.setIsActive(false);
            minorCategoryRepository.save(minor);
        } else {
            // 无记录，直接删除
            minorCategoryRepository.deleteById(id);
        }
    }

    // ==================== 支出记录管理 ====================

    /**
     * 批量保存支出记录
     */
    @Transactional
    public List<ExpenseRecordDTO> batchSaveExpenseRecords(
        BatchExpenseRecordRequest request
    ) {
        List<ExpenseRecord> savedRecords = new ArrayList<>();

        for (ExpenseRecordItem item : request.getRecords()) {
            // 查找子分类所属的大类
            ExpenseCategoryMinor minor = minorCategoryRepository
                .findById(item.getMinorCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "子分类不存在: " + item.getMinorCategoryId()
                ));

            // 检查是否已存在（包含货币，支持同一分类多货币记录）
            Optional<ExpenseRecord> existing = expenseRecordRepository
                .findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(
                    request.getFamilyId(),
                    request.getExpensePeriod(),
                    item.getMinorCategoryId(),
                    item.getCurrency()
                );

            ExpenseRecord record;
            if (existing.isPresent()) {
                // 更新现有记录
                record = existing.get();
                record.setAmount(item.getAmount());
                record.setExpenseType(item.getExpenseType());
                record.setDescription(item.getDescription());
            } else {
                // 创建新记录
                record = buildExpenseRecord(request, item, minor);
            }

            savedRecords.add(expenseRecordRepository.save(record));
        }

        return savedRecords.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

#### ExpenseAnalysisService

**职责**：年度分析、预算分析

**核心方法**：

```java
@Service
public class ExpenseAnalysisService {

    @Autowired
    private ExpenseRecordRepository expenseRecordRepository;

    @Autowired
    private ExpenseBudgetRepository expenseBudgetRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * 获取年度大类汇总
     */
    public List<ExpenseAnnualMajorCategoryDTO> getAnnualMajorCategorySummary(
        Long familyId, Integer year, String currency
    ) {
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";

        List<ExpenseRecord> records = expenseRecordRepository
            .findByFamilyIdAndExpensePeriodBetween(familyId, startPeriod, endPeriod);

        // 加载汇率（All模式）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 按大类分组并汇总
        Map<Long, BigDecimal> majorCategoryTotals = new HashMap<>();

        for (ExpenseRecord record : records) {
            // 过滤货币
            if (!"All".equalsIgnoreCase(currency)
                && !currency.equals(record.getCurrency())) {
                continue;
            }

            Long majorId = record.getMinorCategory().getMajorCategory().getId();
            BigDecimal amount;

            if ("All".equalsIgnoreCase(currency)) {
                // All模式：转换为USD
                amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    rateMap
                );
            } else {
                amount = record.getAmount();
            }

            majorCategoryTotals.merge(majorId, amount, BigDecimal::add);
        }

        // 转换为DTO并排序
        return buildMajorCategoryDTOs(majorCategoryTotals, currency);
    }

    /**
     * 获取预算执行分析
     */
    public List<BudgetExecutionDTO> getBudgetExecution(
        Long familyId, Integer budgetYear, String currency
    ) {
        // 查询预算
        List<ExpenseBudget> budgets = expenseBudgetRepository
            .findByFamilyIdAndBudgetYear(familyId, budgetYear);

        // 查询实际支出
        String startPeriod = budgetYear + "-01";
        String endPeriod = budgetYear + "-12";
        List<ExpenseRecord> records = expenseRecordRepository
            .findByFamilyIdAndExpensePeriodBetween(
                familyId, startPeriod, endPeriod
            );

        // 按小类+货币分组统计实际支出
        Map<String, BigDecimal> actualTotals = calculateActualTotals(
            records, currency
        );

        // 对比预算和实际，计算执行率
        return buildBudgetExecutionDTOs(
            budgets, actualTotals, currency
        );
    }

    /**
     * 调用存储过程计算年度支出汇总
     */
    @Transactional
    public void calculateAnnualExpenseSummary(Long familyId, Integer year) {
        entityManager.createNativeQuery(
            "CALL calculate_annual_expense_summary_v2(:familyId, :year)"
        )
        .setParameter("familyId", familyId)
        .setParameter("year", year)
        .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }
}
```

#### ExpenseBudgetService

**职责**：预算管理

**核心方法**：

```java
@Service
@RequiredArgsConstructor
public class ExpenseBudgetService {

    private final ExpenseBudgetRepository budgetRepository;

    /**
     * 批量保存预算
     */
    @Transactional
    public Map<String, Integer> batchSaveBudgets(
        BatchBudgetRequest request
    ) {
        int created = 0;
        int updated = 0;
        int deleted = 0;

        for (BudgetItem item : request.getBudgets()) {
            Optional<ExpenseBudget> existing = budgetRepository
                .findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(
                    request.getFamilyId(),
                    request.getBudgetYear(),
                    item.getMinorCategoryId(),
                    request.getCurrency()
                );

            if (item.getBudgetAmount() == null
                || item.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                // 删除预算
                existing.ifPresent(budget -> {
                    budgetRepository.delete(budget);
                    deleted++;
                });
            } else if (existing.isPresent()) {
                // 更新预算
                ExpenseBudget budget = existing.get();
                budget.setBudgetAmount(item.getBudgetAmount());
                budget.setNotes(item.getNotes());
                budgetRepository.save(budget);
                updated++;
            } else {
                // 创建预算
                ExpenseBudget budget = buildBudget(request, item);
                budgetRepository.save(budget);
                created++;
            }
        }

        return Map.of(
            "created", created,
            "updated", updated,
            "deleted", deleted
        );
    }
}
```

### 3.5 Controller层

#### ExpenseController

```java
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }

    @PostMapping("/categories/minor")
    public ResponseEntity<MinorCategoryDTO> createMinorCategory(
        @RequestBody CreateMinorCategoryRequest request
    ) {
        return ResponseEntity.ok(
            expenseService.createMinorCategory(request)
        );
    }

    @PutMapping("/categories/minor/{id}")
    public ResponseEntity<MinorCategoryDTO> updateMinorCategory(
        @PathVariable Long id,
        @RequestBody CreateMinorCategoryRequest request
    ) {
        return ResponseEntity.ok(
            expenseService.updateMinorCategory(id, request)
        );
    }

    @DeleteMapping("/categories/minor/{id}")
    public ResponseEntity<Void> disableMinorCategory(@PathVariable Long id) {
        expenseService.disableMinorCategory(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 支出记录管理 ====================

    @PostMapping("/records/batch")
    public ResponseEntity<List<ExpenseRecordDTO>> batchSaveExpenseRecords(
        @RequestBody BatchExpenseRecordRequest request
    ) {
        return ResponseEntity.ok(
            expenseService.batchSaveExpenseRecords(request)
        );
    }

    @GetMapping("/records")
    public ResponseEntity<List<ExpenseRecordDTO>> getExpenseRecordsByPeriod(
        @RequestParam Long familyId,
        @RequestParam String period
    ) {
        return ResponseEntity.ok(
            expenseService.getExpenseRecordsByPeriod(familyId, period)
        );
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteExpenseRecord(@PathVariable Long id) {
        expenseService.deleteExpenseRecord(id);
        return ResponseEntity.ok().build();
    }
}
```

#### ExpenseAnalysisController

```java
@RestController
@RequestMapping("/api/expenses/analysis")
@RequiredArgsConstructor
public class ExpenseAnalysisController {

    private final ExpenseAnalysisService analysisService;

    @GetMapping("/annual/major-categories")
    public ResponseEntity<List<ExpenseAnnualMajorCategoryDTO>>
        getAnnualMajorCategorySummary(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(
            analysisService.getAnnualMajorCategorySummary(
                familyId, year, currency
            )
        );
    }

    @GetMapping("/annual/minor-categories")
    public ResponseEntity<List<ExpenseAnnualMinorCategoryDTO>>
        getAnnualMinorCategorySummary(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(
            analysisService.getAnnualMinorCategorySummary(
                familyId, year, majorCategoryId, currency
            )
        );
    }

    @GetMapping("/budget-execution")
    public ResponseEntity<List<BudgetExecutionDTO>> getBudgetExecution(
        @RequestParam Long familyId,
        @RequestParam Integer budgetYear,
        @RequestParam(defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(
            analysisService.getBudgetExecution(
                familyId, budgetYear, currency
            )
        );
    }

    @PostMapping("/calculate-annual-summary")
    public ResponseEntity<Void> calculateAnnualExpenseSummary(
        @RequestParam Long familyId,
        @RequestParam Integer year
    ) {
        analysisService.calculateAnnualExpenseSummary(familyId, year);
        return ResponseEntity.ok().build();
    }
}
```

## 4. 前端设计

### 4.1 技术栈

- **框架**: Vue 3
- **状态管理**: Composition API + ref/reactive
- **UI框架**: Tailwind CSS
- **图表**: ECharts
- **HTTP**: Axios

### 4.2 API封装

#### expense.js

```javascript
import request from '@/utils/request'

// 分类管理
export const expenseCategoryAPI = {
  getAll: () => request.get('/api/expenses/categories'),
  createMinor: (data) => request.post('/api/expenses/categories/minor', data),
  updateMinor: (id, data) => request.put(`/api/expenses/categories/minor/${id}`, data),
  deleteMinor: (id) => request.delete(`/api/expenses/categories/minor/${id}`)
}

// 支出记录
export const expenseRecordAPI = {
  batchSave: (data) => request.post('/api/expenses/records/batch', data),
  getByPeriod: (familyId, period) => request.get('/api/expenses/records', {
    params: { familyId, period }
  }),
  delete: (id) => request.delete(`/api/expenses/records/${id}`)
}

// 预算管理
export const expenseBudgetAPI = {
  batchSave: (data) => request.post('/api/expenses/budgets/batch', data),
  getByYear: (familyId, year, currency) => request.get('/api/expenses/budgets', {
    params: { familyId, budgetYear: year, currency }
  })
}

// 分析报表
export const expenseAnalysisAPI = {
  getAnnualMajorCategories: (familyId, year, currency) =>
    request.get('/api/expenses/analysis/annual/major-categories', {
      params: { familyId, year, currency }
    }),
  getAnnualMinorCategories: (familyId, year, majorCategoryId, currency) =>
    request.get('/api/expenses/analysis/annual/minor-categories', {
      params: { familyId, year, majorCategoryId, currency }
    }),
  getBudgetExecution: (familyId, budgetYear, currency) =>
    request.get('/api/expenses/analysis/budget-execution', {
      params: { familyId, budgetYear, currency }
    })
}
```

### 4.3 核心组件

#### ExpenseBatchUpdate.vue

**功能**：批量录入当月支出

**核心状态**：
```javascript
const selectedFamilyId = ref(null)
const selectedCurrency = ref('USD')
const recordPeriod = ref('2025-02') // YYYY-MM
const allMinorCategories = ref([])
const categoryAmounts = ref({}) // {categoryId: amount}
const historyData = ref({}) // {categoryId: {month1, month2, month3}}
const changedRecords = ref(new Set())
```

**计算属性**：
```javascript
// 过滤后的分类列表（排序规则）
const filteredCategories = computed(() => {
  return allMinorCategories.value.sort((a, b) => {
    // 1. 按类型排序：固定日常在前
    const typeOrder = { 'FIXED_DAILY': 1, 'LARGE_IRREGULAR': 2 }
    const typeA = typeOrder[a.expenseType] || 3
    const typeB = typeOrder[b.expenseType] || 3

    if (typeA !== typeB) return typeA - typeB

    // 2. 同类型内，有历史数据的排前面
    const hasHistoryA = historyData.value[a.id] &&
      Object.values(historyData.value[a.id]).some(amount => amount > 0)
    const hasHistoryB = historyData.value[b.id] &&
      Object.values(historyData.value[b.id]).some(amount => amount > 0)

    if (hasHistoryA && !hasHistoryB) return -1
    if (!hasHistoryA && hasHistoryB) return 1

    // 3. 按ID排序
    return a.id - b.id
  })
})

// 统计数据
const summary = computed(() => {
  const total = filteredCategories.value.reduce((sum, c) => {
    return sum + parseFloat(categoryAmounts.value[c.id] || 0)
  }, 0)

  const fixed = filteredCategories.value
    .filter(c => c.expenseType === 'FIXED_DAILY')
    .reduce((sum, c) => {
      return sum + parseFloat(categoryAmounts.value[c.id] || 0)
    }, 0)

  const large = total - fixed
  const fixedPercent = total > 0 ? (fixed / total * 100).toFixed(1) : 0
  const largePercent = total > 0 ? (large / total * 100).toFixed(1) : 0

  return { total, fixed, large, fixedPercent, largePercent }
})
```

**核心方法**：
```javascript
// 加载历史数据（前3个月）
async function loadHistoryData() {
  const periods = [
    getPreviousPeriod(recordPeriod.value, 3),
    getPreviousPeriod(recordPeriod.value, 2),
    getPreviousPeriod(recordPeriod.value, 1)
  ]

  const results = await Promise.all(
    periods.map(period =>
      expenseRecordAPI.getByPeriod(selectedFamilyId.value, period)
    )
  )

  results.forEach((response, index) => {
    const records = response.data.filter(r => r.currency === selectedCurrency.value)
    records.forEach(record => {
      if (!historyData.value[record.minorCategoryId]) {
        historyData.value[record.minorCategoryId] = {}
      }
      const monthKey = `month${3 - index}`
      historyData.value[record.minorCategoryId][monthKey] = record.amount
    })
  })
}

// 保存全部
async function saveAll() {
  const recordsToSave = []
  const recordsToDelete = []

  Array.from(changedRecords.value).forEach(categoryId => {
    const amount = parseFloat(categoryAmounts.value[categoryId])
    const category = allMinorCategories.value.find(c => c.id === categoryId)

    if (!amount || amount <= 0) {
      recordsToDelete.push(categoryId)
    } else {
      recordsToSave.push({
        minorCategoryId: categoryId,
        amount: amount,
        currency: selectedCurrency.value,
        expenseType: category?.expenseType || 'FIXED_DAILY',
        description: ''
      })
    }
  })

  // 保存有金额的记录
  if (recordsToSave.length > 0) {
    await expenseRecordAPI.batchSave({
      familyId: selectedFamilyId.value,
      expensePeriod: recordPeriod.value,
      records: recordsToSave
    })
  }

  // 删除金额为0的记录
  for (const recordId of recordsToDelete) {
    await expenseRecordAPI.delete(recordId)
  }

  changedRecords.value.clear()
  await loadHistoryData()
  await loadCurrentMonthData()
}
```

#### ExpenseAnnual.vue

**功能**：年度支出分析（大类汇总、小类明细、月度趋势）

**核心状态**：
```javascript
const selectedFamilyId = ref(null)
const selectedYear = ref(2025)
const selectedCurrency = ref('All')
const selectedMajorCategory = ref(null)
const selectedMinorCategory = ref(null)

const majorCategorySummary = ref([])
const minorCategorySummary = ref([])
const monthlyTrend = ref([])
```

**数据加载**：
```javascript
// 加载大类汇总
async function loadMajorCategorySummary() {
  const response = await expenseAnalysisAPI.getAnnualMajorCategories(
    selectedFamilyId.value,
    selectedYear.value,
    selectedCurrency.value
  )
  majorCategorySummary.value = response.data
}

// 加载小类明细
async function loadMinorCategorySummary() {
  if (!selectedMajorCategory.value) return

  const response = await expenseAnalysisAPI.getAnnualMinorCategories(
    selectedFamilyId.value,
    selectedYear.value,
    selectedMajorCategory.value.id,
    selectedCurrency.value
  )
  minorCategorySummary.value = response.data
}
```

**图表展示**：
```javascript
// ECharts饼图配置
const pieChartOption = computed(() => ({
  title: { text: `${selectedYear.value}年度支出大类分布` },
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: '70%',
    data: majorCategorySummary.value.map(item => ({
      name: item.majorCategoryName,
      value: item.totalAmount
    }))
  }]
}))
```

## 5. 性能优化

### 5.1 数据库优化

1. **索引优化**：
   - 复合索引覆盖查询条件
   - 定期分析索引使用情况

2. **查询优化**：
   - 避免N+1查询问题（使用JOIN FETCH）
   - 分页查询大数据集
   - 使用数据库视图/存储过程预计算

3. **连接池配置**：
   - HikariCP连接池优化
   - 合理配置最大连接数

### 5.2 后端优化

1. **缓存策略**：
   - Redis缓存分类列表（更新频率低）
   - 本地缓存汇率数据

2. **批量操作**：
   - 批量查询减少数据库往返
   - 批量插入/更新使用batch

3. **异步处理**：
   - 年度汇总计算使用异步任务
   - 大数据导出使用后台任务

### 5.3 前端优化

1. **数据加载**：
   - 并行请求前3个月历史数据
   - 懒加载大类明细

2. **虚拟滚动**：
   - 大数据列表使用虚拟滚动
   - 减少DOM渲染开销

3. **防抖节流**：
   - 输入框变更事件防抖
   - 滚动事件节流

## 6. 错误处理

### 6.1 后端异常处理

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
        IllegalArgumentException ex
    ) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException ex
    ) {
        String message = "数据完整性约束违反";
        if (ex.getMessage().contains("uk_period_category")) {
            message = "该期间该分类已有支出记录";
        }
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(message));
    }
}
```

### 6.2 前端错误处理

```javascript
// 全局错误拦截
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const message = error.response.data.message || '操作失败'
      ElMessage.error(message)
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)
```

## 7. 测试策略

### 7.1 单元测试

```java
@SpringBootTest
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Test
    void testBatchSaveExpenseRecords() {
        BatchExpenseRecordRequest request = new BatchExpenseRecordRequest();
        // ... 设置测试数据

        List<ExpenseRecordDTO> result =
            expenseService.batchSaveExpenseRecords(request);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
}
```

### 7.2 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllCategories() throws Exception {
        mockMvc.perform(get("/api/expenses/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

## 8. 部署说明

### 8.1 数据库初始化

```bash
# 1. 执行建表脚本
mysql -u root -p finance < V001__create_expense_tables.sql

# 2. 执行初始化数据
mysql -u root -p finance < V002__insert_expense_initial_data.sql

# 3. 创建存储过程
mysql -u root -p finance < V006__create_annual_expense_summary_v2.sql
```

### 8.2 应用配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finance?useSSL=false
    username: finance_user
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### 8.3 定时任务

```java
@Scheduled(cron = "0 0 2 1 * ?") // 每月1日凌晨2点执行
public void calculateMonthlyExpenseSummary() {
    // 计算上月汇总
    LocalDate lastMonth = LocalDate.now().minusMonths(1);
    int year = lastMonth.getYear();

    analysisService.calculateAnnualExpenseSummary(
        defaultFamilyId,
        year
    );
}
```
