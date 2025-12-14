# 投资管理模块 - 设计文档

## 1. 概述

本文档详细描述投资管理模块的技术实现，包括数据库设计、后端API设计和前端组件设计。

## 2. 数据库设计

### 2.1 表结构设计

#### 2.1.1 asset_types（资产类型表）

**用途**：定义各种资产类型，通过`isInvestment`字段标记投资类

```sql
CREATE TABLE asset_types (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  type VARCHAR(50) NOT NULL UNIQUE COMMENT '类型代码',
  chinese_name VARCHAR(50) NOT NULL COMMENT '中文名称',
  english_name VARCHAR(50) NOT NULL COMMENT '英文名称',
  icon VARCHAR(50) COMMENT '图标',
  is_investment BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否投资类',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_is_investment (is_investment),
  KEY idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产类型表';
```

**投资类资产类型**：
- STOCK（股票）- is_investment = true
- MUTUAL_FUND（基金）- is_investment = true
- BOND（债券）- is_investment = true
- CRYPTO（加密货币）- is_investment = true
- REAL_ESTATE（房地产）- is_investment = true
- 401K、529等退休金账户 - is_investment = true

#### 2.1.2 asset_accounts（资产账户表）

**用途**：存储所有资产账户，投资账户通过资产类型的`isInvestment`标记识别

```sql
CREATE TABLE asset_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  family_id BIGINT NOT NULL COMMENT '所属家庭ID（冗余字段）',
  asset_type_id BIGINT NOT NULL COMMENT '资产类型ID',
  account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
  currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型',
  institution VARCHAR(100) COMMENT '机构名称',
  account_number VARCHAR(50) COMMENT '账户号',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  linked_liability_account_id BIGINT COMMENT '关联的负债账户ID（房地产用）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (family_id) REFERENCES families(id),
  FOREIGN KEY (asset_type_id) REFERENCES asset_types(id),
  FOREIGN KEY (linked_liability_account_id) REFERENCES liability_accounts(id),

  KEY idx_user_id (user_id),
  KEY idx_family_id (family_id),
  KEY idx_asset_type_id (asset_type_id),
  KEY idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产账户表';
```

**关键字段**：
- `linked_liability_account_id`: 房地产账户关联房贷账户，用于计算净资产

#### 2.1.3 investment_transactions（投资交易记录表）

**用途**：记录月度投资交易（投入/取出），非每笔交易明细

```sql
CREATE TABLE investment_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  account_id BIGINT NOT NULL COMMENT '账户ID',
  transaction_period VARCHAR(7) NOT NULL COMMENT '交易期间（YYYY-MM）',
  transaction_type VARCHAR(20) NOT NULL COMMENT '交易类型（DEPOSIT/WITHDRAWAL）',
  amount DECIMAL(18,2) NOT NULL COMMENT '金额（账户货币）',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (account_id) REFERENCES asset_accounts(id),
  UNIQUE KEY uk_account_period_type (account_id, transaction_period, transaction_type),
  KEY idx_transaction_period (transaction_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资交易记录表';
```

**约束说明**：
- `uk_account_period_type`: 同一账户同一期间同一类型只能有一条记录

**transaction_type 枚举值**：
- `DEPOSIT`: 投入
- `WITHDRAWAL`: 取出

#### 2.1.4 asset_records（资产记录表）

**用途**：记录资产账户的价值变化（月度快照）

```sql
CREATE TABLE asset_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  account_id BIGINT NOT NULL COMMENT '账户ID',
  record_date DATE NOT NULL COMMENT '记录日期',
  amount DECIMAL(18,2) NOT NULL COMMENT '资产金额',
  currency VARCHAR(10) NOT NULL COMMENT '货币类型',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (account_id) REFERENCES asset_accounts(id),
  UNIQUE KEY uk_account_date (account_id, record_date),
  KEY idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产记录表';
```

#### 2.1.5 liability_accounts（负债账户表）

**用途**：存储房贷等负债账户

```sql
CREATE TABLE liability_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  liability_type_id BIGINT NOT NULL COMMENT '负债类型ID',
  account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
  currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币类型',
  institution VARCHAR(100) COMMENT '机构名称',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (liability_type_id) REFERENCES liability_types(id),

  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='负债账户表';
```

#### 2.1.6 liability_records（负债记录表）

**用途**：记录负债账户的余额变化（月度快照）

```sql
CREATE TABLE liability_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  account_id BIGINT NOT NULL COMMENT '账户ID',
  record_date DATE NOT NULL COMMENT '记录日期',
  outstanding_balance DECIMAL(18,2) NOT NULL COMMENT '未偿还余额',
  currency VARCHAR(10) NOT NULL COMMENT '货币类型',
  description TEXT COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (account_id) REFERENCES liability_accounts(id),
  UNIQUE KEY uk_account_date (account_id, record_date),
  KEY idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='负债记录表';
```

### 2.2 索引设计

**查询优化索引**：

1. **投资账户查询**：
   - `idx_family_id`: 快速查询某家庭的所有账户
   - `idx_asset_type_id`: 按大类筛选账户
   - `idx_is_active`: 只查询启用的账户

2. **交易记录查询**：
   - `uk_account_period_type`: 唯一约束，同时用于快速查询
   - `idx_transaction_period`: 按期间范围查询

3. **资产记录查询**：
   - `uk_account_date`: 唯一约束，同时用于快速查询最新记录
   - `idx_record_date`: 按日期范围查询

## 3. 后端API设计

### 3.1 技术栈

- **框架**: Spring Boot 3.x
- **ORM**: Spring Data JPA + Hibernate
- **数据库**: MySQL 8.0
- **语言**: Java 17

### 3.2 实体模型

#### AssetType

```java
@Entity
@Table(name = "asset_types")
@Data
public class AssetType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String type;
    private String chineseName;
    private String englishName;
    private String icon;
    private Boolean isInvestment;
    private Boolean isActive;
    private Integer displayOrder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### AssetAccount

```java
@Entity
@Table(name = "asset_accounts")
@Data
public class AssetAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long familyId;
    private Long assetTypeId;
    private String accountName;
    private String currency;
    private String institution;
    private String accountNumber;
    private Boolean isActive;
    private Long linkedLiabilityAccountId; // 房地产账户关联房贷

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_type_id", insertable = false, updatable = false)
    private AssetType assetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
```

#### InvestmentTransaction

```java
@Entity
@Table(name = "investment_transactions")
@Data
public class InvestmentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;
    private String transactionPeriod; // YYYY-MM

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // DEPOSIT, WITHDRAWAL

    private BigDecimal amount;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private AssetAccount account;

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
}
```

#### AssetRecord

```java
@Entity
@Table(name = "asset_records")
@Data
public class AssetRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;
    private LocalDate recordDate;
    private BigDecimal amount;
    private String currency;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private AssetAccount account;
}
```

### 3.3 Repository层

#### InvestmentTransactionRepository

```java
@Repository
public interface InvestmentTransactionRepository
    extends JpaRepository<InvestmentTransaction, Long> {

    // 查询某账户某期间范围内的所有交易
    @Query("SELECT t FROM InvestmentTransaction t " +
           "WHERE t.accountId = :accountId " +
           "AND t.transactionPeriod >= :startPeriod " +
           "AND t.transactionPeriod <= :endPeriod " +
           "ORDER BY t.transactionPeriod")
    List<InvestmentTransaction> findByAccountIdAndPeriodRange(
        @Param("accountId") Long accountId,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    // 查询某家庭某年的所有交易
    @Query("SELECT t FROM InvestmentTransaction t " +
           "JOIN t.account a " +
           "WHERE a.familyId = :familyId " +
           "AND t.transactionPeriod LIKE :yearPattern")
    List<InvestmentTransaction> findByFamilyIdAndYearPattern(
        @Param("familyId") Long familyId,
        @Param("yearPattern") String yearPattern
    );

    // 查询某家庭某年某资产类型的所有交易
    @Query("SELECT t FROM InvestmentTransaction t " +
           "JOIN t.account a " +
           "WHERE a.familyId = :familyId " +
           "AND a.assetTypeId = :assetTypeId " +
           "AND t.transactionPeriod LIKE :yearPattern")
    List<InvestmentTransaction> findByFamilyIdAndAssetTypeIdAndYearPattern(
        @Param("familyId") Long familyId,
        @Param("assetTypeId") Long assetTypeId,
        @Param("yearPattern") String yearPattern
    );

    // 查询某账户某期间某类型的交易
    Optional<InvestmentTransaction> findByAccountIdAndTransactionPeriodAndTransactionType(
        Long accountId,
        String transactionPeriod,
        InvestmentTransaction.TransactionType transactionType
    );
}
```

#### AssetRecordRepository

```java
@Repository
public interface AssetRecordRepository
    extends JpaRepository<AssetRecord, Long> {

    // 查询账户最新资产记录
    @Query("SELECT r FROM AssetRecord r " +
           "WHERE r.accountId = :accountId " +
           "ORDER BY r.recordDate DESC")
    List<AssetRecord> findLatestByAccountId(
        @Param("accountId") Long accountId,
        Pageable pageable
    );

    // 汇总多个账户的最新资产总额
    @Query("SELECT SUM(r.amount) FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (" +
           "  SELECT MAX(r2.recordDate) " +
           "  FROM AssetRecord r2 " +
           "  WHERE r2.accountId = r.accountId" +
           ")")
    BigDecimal sumLatestAmountByAccountIds(
        @Param("accountIds") List<Long> accountIds
    );

    // 汇总多个账户截止某日期的资产总额
    @Query("SELECT SUM(r.amount) FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (" +
           "  SELECT MAX(r2.recordDate) " +
           "  FROM AssetRecord r2 " +
           "  WHERE r2.accountId = r.accountId " +
           "  AND r2.recordDate <= :asOfDate" +
           ")")
    BigDecimal sumAmountByAccountIdsAsOfDate(
        @Param("accountIds") List<Long> accountIds,
        @Param("asOfDate") LocalDate asOfDate
    );
}
```

#### LiabilityRecordRepository

```java
@Repository
public interface LiabilityRecordRepository
    extends JpaRepository<LiabilityRecord, Long> {

    // 查询账户最新负债记录
    @Query("SELECT r FROM LiabilityRecord r " +
           "WHERE r.accountId = :accountId " +
           "ORDER BY r.recordDate DESC")
    Optional<LiabilityRecord> findLatestByAccountId(
        @Param("accountId") Long accountId
    );

    // 查询账户截止某日期的最新负债记录
    @Query("SELECT r FROM LiabilityRecord r " +
           "WHERE r.accountId = :accountId " +
           "AND r.recordDate <= :asOfDate " +
           "ORDER BY r.recordDate DESC")
    Optional<LiabilityRecord> findLatestByAccountIdBeforeOrOnDate(
        @Param("accountId") Long accountId,
        @Param("asOfDate") LocalDate asOfDate
    );
}
```

### 3.4 Service层

#### InvestmentAccountService

**职责**：投资账户管理

```java
@Service
@RequiredArgsConstructor
public class InvestmentAccountService {

    private final AssetAccountRepository assetAccountRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final UserRepository userRepository;
    private final InvestmentTransactionRepository transactionRepository;
    private final AssetRecordRepository assetRecordRepository;

    /**
     * 获取所有投资账户
     */
    public List<InvestmentAccountDTO> getInvestmentAccounts(Long familyId) {
        // 1. 获取该家庭的所有用户ID
        List<Long> userIds = userRepository.findAll().stream()
            .filter(user -> familyId.equals(user.getFamilyId()))
            .map(User::getId)
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询所有投资账户（通过资产类型的isInvestment字段过滤）
        List<AssetAccount> accounts = assetAccountRepository
            .findByUserIdInAndIsActiveTrue(userIds);

        List<InvestmentAccountDTO> result = new ArrayList<>();

        for (AssetAccount account : accounts) {
            // 只包含投资类账户
            if (account.getAssetType() == null
                || !account.getAssetType().getIsInvestment()) {
                continue;
            }

            InvestmentAccountDTO dto = new InvestmentAccountDTO();
            dto.setAccountId(account.getId());
            dto.setAccountName(account.getAccountName());
            dto.setUserName(account.getUser().getFullName());
            dto.setCategoryId(account.getAssetTypeId());
            dto.setCategoryName(account.getAssetType().getChineseName());
            dto.setCategoryIcon(account.getAssetType().getIcon());
            dto.setCurrency(account.getCurrency());
            dto.setInstitution(account.getInstitution());

            // 统计交易记录数量
            long transactionCount = transactionRepository
                .countByAccountId(account.getId());
            dto.setTransactionCount(transactionCount);

            // 查询最新资产价值
            BigDecimal latestAmount = assetRecordRepository
                .sumLatestAmountByAccountIds(List.of(account.getId()));
            dto.setLatestAmount(latestAmount);

            // 查询最新记录日期
            assetRecordRepository.findLatestByAccountId(
                account.getId(),
                PageRequest.of(0, 1)
            ).stream()
                .findFirst()
                .ifPresent(record ->
                    dto.setLatestRecordDate(record.getRecordDate())
                );

            result.add(dto);
        }

        return result;
    }

    /**
     * 按大类查询账户
     */
    public List<InvestmentAccountDTO> getAccountsByCategory(
        Long familyId, Long assetTypeId
    ) {
        List<InvestmentAccountDTO> allAccounts = getInvestmentAccounts(familyId);

        if (assetTypeId == null) {
            return allAccounts;
        }

        return allAccounts.stream()
            .filter(account -> assetTypeId.equals(account.getCategoryId()))
            .collect(Collectors.toList());
    }

    /**
     * 获取所有投资分类
     */
    public List<InvestmentCategoryDTO> getInvestmentCategories() {
        List<AssetType> types = assetTypeRepository
            .findByIsInvestmentTrueOrderByDisplayOrderAsc();

        return types.stream()
            .map(type -> {
                InvestmentCategoryDTO dto = new InvestmentCategoryDTO();
                dto.setCategoryId(type.getId());
                dto.setCategoryName(type.getChineseName());
                dto.setCategoryIcon(type.getIcon());
                dto.setCategoryType(type.getType());
                return dto;
            })
            .collect(Collectors.toList());
    }
}
```

#### InvestmentTransactionService

**职责**：投资交易记录管理

```java
@Service
@RequiredArgsConstructor
public class InvestmentTransactionService {

    private final InvestmentTransactionRepository transactionRepository;
    private final AssetAccountRepository assetAccountRepository;

    /**
     * 批量保存交易记录
     */
    @Transactional
    public Map<String, Object> batchSaveTransactions(
        BatchInvestmentTransactionRequest request
    ) {
        int created = 0;
        int updated = 0;
        int deleted = 0;

        for (TransactionItem item : request.getTransactions()) {
            Long accountId = item.getAccountId();
            String period = request.getTransactionPeriod();

            // 处理投入（DEPOSIT）
            if (item.getDeposits() != null && item.getDeposits().compareTo(BigDecimal.ZERO) > 0) {
                var existing = transactionRepository.findByAccountIdAndTransactionPeriodAndTransactionType(
                    accountId, period, InvestmentTransaction.TransactionType.DEPOSIT
                );

                if (existing.isPresent()) {
                    // 更新
                    InvestmentTransaction tx = existing.get();
                    tx.setAmount(item.getDeposits());
                    tx.setDescription(item.getDescription());
                    transactionRepository.save(tx);
                    updated++;
                } else {
                    // 创建
                    InvestmentTransaction tx = new InvestmentTransaction();
                    tx.setAccountId(accountId);
                    tx.setTransactionPeriod(period);
                    tx.setTransactionType(InvestmentTransaction.TransactionType.DEPOSIT);
                    tx.setAmount(item.getDeposits());
                    tx.setDescription(item.getDescription());
                    transactionRepository.save(tx);
                    created++;
                }
            } else {
                // 删除投入记录
                transactionRepository.findByAccountIdAndTransactionPeriodAndTransactionType(
                    accountId, period, InvestmentTransaction.TransactionType.DEPOSIT
                ).ifPresent(tx -> {
                    transactionRepository.delete(tx);
                    deleted++;
                });
            }

            // 处理取出（WITHDRAWAL）
            if (item.getWithdrawals() != null && item.getWithdrawals().compareTo(BigDecimal.ZERO) > 0) {
                var existing = transactionRepository.findByAccountIdAndTransactionPeriodAndTransactionType(
                    accountId, period, InvestmentTransaction.TransactionType.WITHDRAWAL
                );

                if (existing.isPresent()) {
                    // 更新
                    InvestmentTransaction tx = existing.get();
                    tx.setAmount(item.getWithdrawals());
                    tx.setDescription(item.getDescription());
                    transactionRepository.save(tx);
                    updated++;
                } else {
                    // 创建
                    InvestmentTransaction tx = new InvestmentTransaction();
                    tx.setAccountId(accountId);
                    tx.setTransactionPeriod(period);
                    tx.setTransactionType(InvestmentTransaction.TransactionType.WITHDRAWAL);
                    tx.setAmount(item.getWithdrawals());
                    tx.setDescription(item.getDescription());
                    transactionRepository.save(tx);
                    created++;
                }
            } else {
                // 删除取出记录
                transactionRepository.findByAccountIdAndTransactionPeriodAndTransactionType(
                    accountId, period, InvestmentTransaction.TransactionType.WITHDRAWAL
                ).ifPresent(tx -> {
                    transactionRepository.delete(tx);
                    deleted++;
                });
            }
        }

        return Map.of(
            "created", created,
            "updated", updated,
            "deleted", deleted
        );
    }

    /**
     * 查询交易记录
     */
    public List<InvestmentTransactionDTO> getTransactions(
        Long accountId, String startPeriod, String endPeriod
    ) {
        List<InvestmentTransaction> transactions = transactionRepository
            .findByAccountIdAndPeriodRange(accountId, startPeriod, endPeriod);

        return transactions.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private InvestmentTransactionDTO toDTO(InvestmentTransaction tx) {
        InvestmentTransactionDTO dto = new InvestmentTransactionDTO();
        dto.setId(tx.getId());
        dto.setAccountId(tx.getAccountId());
        dto.setAccountName(tx.getAccount().getAccountName());
        dto.setTransactionPeriod(tx.getTransactionPeriod());
        dto.setTransactionType(tx.getTransactionType().name());
        dto.setAmount(tx.getAmount());
        dto.setDescription(tx.getDescription());
        return dto;
    }
}
```

#### InvestmentAnalysisService

**职责**：投资分析和回报率计算

**核心方法**：

```java
@Service
@RequiredArgsConstructor
public class InvestmentAnalysisService {

    private final InvestmentTransactionRepository transactionRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;

    /**
     * 年度大类投资分析
     */
    public List<InvestmentCategoryAnalysisDTO> getAnnualByCategory(
        Long familyId, Integer year, String currency
    ) {
        // 1. 获取所有投资类别
        List<AssetType> investmentCategories = assetTypeRepository
            .findByIsInvestmentTrueOrderByDisplayOrderAsc();

        // 2. 获取该年度所有投资交易记录
        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions = transactionRepository
            .findByFamilyIdAndYearPattern(familyId, yearPattern);

        // 3. 按大类分组统计
        Map<Long, InvestmentCategoryAnalysisDTO> categoryMap = new HashMap<>();

        for (AssetType assetType : investmentCategories) {
            InvestmentCategoryAnalysisDTO dto = new InvestmentCategoryAnalysisDTO();
            dto.setCategoryId(assetType.getId());
            dto.setCategoryName(assetType.getChineseName());
            dto.setCategoryIcon(assetType.getIcon());
            dto.setTotalDeposits(BigDecimal.ZERO);
            dto.setTotalWithdrawals(BigDecimal.ZERO);
            dto.setNetDeposits(BigDecimal.ZERO);
            categoryMap.put(assetType.getId(), dto);
        }

        // 4. 统计每个大类的投入和取出
        for (InvestmentTransaction tx : transactions) {
            Long assetTypeId = tx.getAccount().getAssetTypeId();
            InvestmentCategoryAnalysisDTO dto = categoryMap.get(assetTypeId);

            if (dto != null) {
                BigDecimal amount = tx.getAmount();

                // 货币转换到目标货币
                String accountCurrency = tx.getAccount().getCurrency();
                LocalDate transactionDate = LocalDate.parse(tx.getTransactionPeriod() + "-01");
                amount = convertCurrency(amount, accountCurrency, currency, transactionDate);

                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setTotalDeposits(dto.getTotalDeposits().add(amount));
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setTotalWithdrawals(dto.getTotalWithdrawals().add(amount));
                }
            }
        }

        // 5. 计算净投入
        categoryMap.values().forEach(dto ->
            dto.setNetDeposits(dto.getTotalDeposits().subtract(dto.getTotalWithdrawals()))
        );

        // 6. 计算投资回报率
        calculateReturnRates(familyId, year, currency, categoryMap);

        // 7. 过滤并排序
        return categoryMap.values().stream()
            .filter(dto -> dto.getCurrentAssets() != null
                && dto.getCurrentAssets().compareTo(BigDecimal.ZERO) > 0)
            .sorted((a, b) -> b.getCurrentAssets().compareTo(a.getCurrentAssets()))
            .collect(Collectors.toList());
    }

    /**
     * 计算投资回报率
     * 回报率 = (当前资产 - 去年年底资产 - 净投入) / (去年年底资产 + 净投入)
     */
    private void calculateReturnRates(
        Long familyId,
        Integer year,
        String targetCurrency,
        Map<Long, InvestmentCategoryAnalysisDTO> categoryMap
    ) {
        LocalDate currentDate = LocalDate.now();
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        // 获取该家庭的所有用户ID
        List<Long> userIds = getUserIdsByFamilyId(familyId);
        if (userIds.isEmpty()) {
            return;
        }

        // 获取所有投资账户
        List<AssetAccount> accounts = assetAccountRepository
            .findByUserIdInAndIsActiveTrue(userIds);

        // 按资产类型分组账户
        Map<Long, List<AssetAccount>> accountsByAssetType = accounts.stream()
            .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
            .collect(Collectors.groupingBy(AssetAccount::getAssetTypeId));

        // 为每个大类计算资产数据和回报率
        for (Map.Entry<Long, InvestmentCategoryAnalysisDTO> entry : categoryMap.entrySet()) {
            Long assetTypeId = entry.getKey();
            InvestmentCategoryAnalysisDTO dto = entry.getValue();

            List<AssetAccount> typeAccounts = accountsByAssetType.get(assetTypeId);
            if (typeAccounts == null || typeAccounts.isEmpty()) {
                dto.setCurrentAssets(BigDecimal.ZERO);
                dto.setLastYearEndAssets(BigDecimal.ZERO);
                dto.setReturnRate(BigDecimal.ZERO);
                continue;
            }

            BigDecimal currentAssets = BigDecimal.ZERO;
            BigDecimal lastYearEndAssets = BigDecimal.ZERO;
            BigDecimal totalPrincipalPayment = BigDecimal.ZERO;

            for (AssetAccount account : typeAccounts) {
                // 检查是否为房地产账户且有关联房贷
                boolean isRealEstateWithMortgage = "REAL_ESTATE".equals(account.getAssetType().getType())
                    && account.getLinkedLiabilityAccountId() != null;

                if (isRealEstateWithMortgage) {
                    // 房地产账户：计算净资产（房产 - 房贷）
                    LiabilityAccount mortgageAccount = liabilityAccountRepository
                        .findById(account.getLinkedLiabilityAccountId()).orElse(null);

                    if (mortgageAccount != null) {
                        // 当前净资产 = 房产 - 房贷
                        BigDecimal currentRealEstateValue = assetRecordRepository
                            .sumLatestAmountByAccountIds(List.of(account.getId()));
                        if (currentRealEstateValue == null) currentRealEstateValue = BigDecimal.ZERO;

                        BigDecimal currentMortgage = liabilityRecordRepository
                            .findLatestByAccountId(mortgageAccount.getId())
                            .map(LiabilityRecord::getOutstandingBalance)
                            .orElse(BigDecimal.ZERO);

                        BigDecimal currentNetWorth = currentRealEstateValue.subtract(currentMortgage);
                        currentNetWorth = convertCurrency(currentNetWorth, account.getCurrency(),
                                                         targetCurrency, currentDate);
                        currentAssets = currentAssets.add(currentNetWorth);

                        // 去年净资产 = 去年房产 - 去年房贷
                        BigDecimal lastYearRealEstateValue = assetRecordRepository
                            .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), lastYearEndDate);
                        if (lastYearRealEstateValue == null) lastYearRealEstateValue = BigDecimal.ZERO;

                        BigDecimal lastYearMortgage = liabilityRecordRepository
                            .findLatestByAccountIdBeforeOrOnDate(mortgageAccount.getId(), lastYearEndDate)
                            .map(LiabilityRecord::getOutstandingBalance)
                            .orElse(BigDecimal.ZERO);

                        BigDecimal lastYearNetWorth = lastYearRealEstateValue.subtract(lastYearMortgage);
                        lastYearNetWorth = convertCurrency(lastYearNetWorth, account.getCurrency(),
                                                          targetCurrency, lastYearEndDate);
                        lastYearEndAssets = lastYearEndAssets.add(lastYearNetWorth);

                        // 本金还款（负债减少部分）= 去年房贷 - 今年房贷
                        BigDecimal principalPayment = lastYearMortgage.subtract(currentMortgage);
                        principalPayment = convertCurrency(principalPayment, mortgageAccount.getCurrency(),
                                                          targetCurrency, currentDate);
                        totalPrincipalPayment = totalPrincipalPayment.add(principalPayment);
                    }
                } else {
                    // 普通投资账户：直接使用资产记录
                    BigDecimal accountCurrentAsset = assetRecordRepository
                        .sumLatestAmountByAccountIds(List.of(account.getId()));
                    if (accountCurrentAsset != null && accountCurrentAsset.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedAmount = convertCurrency(accountCurrentAsset,
                            account.getCurrency(), targetCurrency, currentDate);
                        currentAssets = currentAssets.add(convertedAmount);
                    }

                    BigDecimal accountLastYearAsset = assetRecordRepository
                        .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), lastYearEndDate);
                    if (accountLastYearAsset != null && accountLastYearAsset.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedAmount = convertCurrency(accountLastYearAsset,
                            account.getCurrency(), targetCurrency, lastYearEndDate);
                        lastYearEndAssets = lastYearEndAssets.add(convertedAmount);
                    }
                }
            }

            dto.setCurrentAssets(currentAssets);
            dto.setLastYearEndAssets(lastYearEndAssets);

            // 房地产大类：将本金还款加到净投入中
            if (totalPrincipalPayment.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal updatedNetDeposits = dto.getNetDeposits().add(totalPrincipalPayment);
                dto.setNetDeposits(updatedNetDeposits);
            }

            // 计算投资回报率
            BigDecimal netDeposits = dto.getNetDeposits();
            BigDecimal denominator = lastYearEndAssets.add(netDeposits);

            // 投资回报 = 当前资产 - 去年年底资产 - 净投入
            BigDecimal returns = currentAssets.subtract(lastYearEndAssets).subtract(netDeposits);
            dto.setReturns(returns);

            // 回报率
            if (denominator.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)); // 转换为百分比
                dto.setReturnRate(returnRate);
            } else {
                dto.setReturnRate(BigDecimal.ZERO);
            }
        }
    }

    /**
     * 货币转换辅助方法
     */
    private BigDecimal convertCurrency(
        BigDecimal amount,
        String fromCurrency,
        String toCurrency,
        LocalDate date
    ) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // 获取源货币到USD的汇率
        BigDecimal fromRate = exchangeRateService.getExchangeRate(fromCurrency, date);
        // 获取目标货币到USD的汇率
        BigDecimal toRate = exchangeRateService.getExchangeRate(toCurrency, date);

        // 转换逻辑：
        // 1. 先将源货币转换为USD：amount * fromRate
        // 2. 再将USD转换为目标货币：(amount * fromRate) / toRate
        BigDecimal amountInUsd = amount.multiply(fromRate);
        if (toRate.compareTo(BigDecimal.ZERO) == 0) {
            return amountInUsd;
        }
        return amountInUsd.divide(toRate, 2, RoundingMode.HALF_UP);
    }

    /**
     * 年度账户投资分析（按大类筛选）
     */
    public List<InvestmentAccountAnalysisDTO> getAnnualByAccount(
        Long familyId, Integer year, Long assetTypeId, String currency
    ) {
        // 获取该家庭的所有用户ID
        List<Long> userIds = getUserIdsByFamilyId(familyId);
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有投资账户（筛选条件：家庭用户 + 可选的资产类型）
        List<AssetAccount> accounts;
        if (assetTypeId != null) {
            accounts = assetAccountRepository.findByUserIdInAndIsActiveTrue(userIds).stream()
                .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
                .filter(acc -> assetTypeId.equals(acc.getAssetTypeId()))
                .collect(Collectors.toList());
        } else {
            accounts = assetAccountRepository.findByUserIdInAndIsActiveTrue(userIds).stream()
                .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
                .collect(Collectors.toList());
        }

        // 查询所有用户信息
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> userNameMap = users.stream()
            .collect(Collectors.toMap(User::getId, User::getFullName));

        // 初始化所有账户的DTO
        Map<Long, InvestmentAccountAnalysisDTO> accountMap = new HashMap<>();
        for (AssetAccount account : accounts) {
            InvestmentAccountAnalysisDTO dto = new InvestmentAccountAnalysisDTO();
            dto.setAccountId(account.getId());
            dto.setAccountName(account.getAccountName());
            dto.setCategoryName(account.getAssetType().getChineseName());
            dto.setUserName(userNameMap.getOrDefault(account.getUserId(), "Unknown"));
            dto.setCurrency(account.getCurrency());
            dto.setTotalDeposits(BigDecimal.ZERO);
            dto.setTotalWithdrawals(BigDecimal.ZERO);
            dto.setNetDeposits(BigDecimal.ZERO);
            accountMap.put(account.getId(), dto);
        }

        // 获取交易记录并统计
        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions;
        if (assetTypeId != null) {
            transactions = transactionRepository.findByFamilyIdAndAssetTypeIdAndYearPattern(
                familyId, assetTypeId, yearPattern
            );
        } else {
            transactions = transactionRepository.findByFamilyIdAndYearPattern(familyId, yearPattern);
        }

        for (InvestmentTransaction tx : transactions) {
            Long accountId = tx.getAccountId();
            InvestmentAccountAnalysisDTO dto = accountMap.get(accountId);

            if (dto != null) {
                BigDecimal amount = tx.getAmount();

                // 货币转换到目标货币
                String accountCurrency = tx.getAccount().getCurrency();
                LocalDate transactionDate = LocalDate.parse(tx.getTransactionPeriod() + "-01");
                amount = convertCurrency(amount, accountCurrency, currency, transactionDate);

                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setTotalDeposits(dto.getTotalDeposits().add(amount));
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setTotalWithdrawals(dto.getTotalWithdrawals().add(amount));
                }
            }
        }

        // 计算净投入
        accountMap.values().forEach(dto ->
            dto.setNetDeposits(dto.getTotalDeposits().subtract(dto.getTotalWithdrawals()))
        );

        // 计算投资回报率
        calculateAccountReturnRates(familyId, year, currency, accountMap);

        // 按当前资产排序
        return accountMap.values().stream()
            .filter(dto -> dto.getCurrentAssets() != null
                && dto.getCurrentAssets().compareTo(BigDecimal.ZERO) > 0)
            .sorted((a, b) -> b.getCurrentAssets().compareTo(a.getCurrentAssets()))
            .collect(Collectors.toList());
    }

    /**
     * 账户月度趋势
     */
    public AccountMonthlyTrendResponseDTO getAccountMonthlyTrend(
        Long accountId, Integer year
    ) {
        // 获取账户信息
        AssetAccount account = assetAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return null;
        }

        // 房地产账户不支持月度趋势（净资产是计算值，没有独立的月度交易）
        boolean isRealEstate = account.getAssetType() != null
            && "REAL_ESTATE".equals(account.getAssetType().getType());

        if (isRealEstate) {
            return null;
        }

        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions = transactionRepository
            .findByAccountIdAndYearPattern(accountId, yearPattern);

        // 初始化12个月的数据
        Map<Integer, InvestmentMonthlyTrendDTO> monthMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            InvestmentMonthlyTrendDTO dto = new InvestmentMonthlyTrendDTO();
            dto.setMonth(i);
            dto.setPeriod(String.format("%d-%02d", year, i));
            dto.setDeposits(BigDecimal.ZERO);
            dto.setWithdrawals(BigDecimal.ZERO);
            monthMap.put(i, dto);
        }

        // 统计每个月的投入和取出
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        String accountName = "";

        for (InvestmentTransaction tx : transactions) {
            if (accountName.isEmpty()) {
                accountName = tx.getAccount().getAccountName();
            }

            String period = tx.getTransactionPeriod();
            int month = Integer.parseInt(period.substring(5, 7));

            InvestmentMonthlyTrendDTO dto = monthMap.get(month);
            if (dto != null) {
                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setDeposits(dto.getDeposits().add(tx.getAmount()));
                    totalDeposits = totalDeposits.add(tx.getAmount());
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setWithdrawals(dto.getWithdrawals().add(tx.getAmount()));
                    totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                }
            }
        }

        // 计算净投入
        BigDecimal netDeposits = totalDeposits.subtract(totalWithdrawals);

        // 查询资产数据
        LocalDate currentDate = LocalDate.now();
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        BigDecimal currentAssets = assetRecordRepository
            .sumLatestAmountByAccountIds(List.of(accountId));
        if (currentAssets == null) {
            currentAssets = BigDecimal.ZERO;
        }

        BigDecimal lastYearEndAssets = assetRecordRepository
            .sumAmountByAccountIdsAsOfDate(List.of(accountId), lastYearEndDate);
        if (lastYearEndAssets == null) {
            lastYearEndAssets = BigDecimal.ZERO;
        }

        // 计算投资回报和回报率
        BigDecimal returns = currentAssets.subtract(lastYearEndAssets).subtract(netDeposits);
        BigDecimal returnRate = BigDecimal.ZERO;
        BigDecimal denominator = lastYearEndAssets.add(netDeposits);
        if (denominator.compareTo(BigDecimal.ZERO) != 0) {
            returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // 构建响应对象
        AccountMonthlyTrendResponseDTO response = new AccountMonthlyTrendResponseDTO();
        response.setAccountName(accountName);
        response.setCurrency(account.getCurrency());
        response.setCurrentAssets(currentAssets);
        response.setLastYearEndAssets(lastYearEndAssets);
        response.setNetDeposits(netDeposits);
        response.setReturns(returns);
        response.setReturnRate(returnRate);

        // 按月份排序
        List<InvestmentMonthlyTrendDTO> monthlyData = monthMap.values().stream()
            .sorted(Comparator.comparing(InvestmentMonthlyTrendDTO::getMonth))
            .collect(Collectors.toList());
        response.setMonthlyData(monthlyData);

        return response;
    }

    private List<Long> getUserIdsByFamilyId(Long familyId) {
        return userRepository.findAll().stream()
            .filter(user -> familyId.equals(user.getFamilyId()))
            .map(User::getId)
            .collect(Collectors.toList());
    }
}
```

### 3.5 Controller层

#### InvestmentController

```java
@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentAccountService accountService;
    private final InvestmentTransactionService transactionService;

    // ==================== 账户管理 ====================

    @GetMapping("/accounts")
    public ResponseEntity<List<InvestmentAccountDTO>> getInvestmentAccounts(
        @RequestParam Long familyId
    ) {
        return ResponseEntity.ok(accountService.getInvestmentAccounts(familyId));
    }

    @GetMapping("/accounts/by-category")
    public ResponseEntity<List<InvestmentAccountDTO>> getAccountsByCategory(
        @RequestParam Long familyId,
        @RequestParam(required = false) Long assetTypeId
    ) {
        return ResponseEntity.ok(
            accountService.getAccountsByCategory(familyId, assetTypeId)
        );
    }

    @GetMapping("/categories")
    public ResponseEntity<List<InvestmentCategoryDTO>> getCategories() {
        return ResponseEntity.ok(accountService.getInvestmentCategories());
    }

    // ==================== 交易记录管理 ====================

    @PostMapping("/transactions/batch")
    public ResponseEntity<Map<String, Object>> batchSaveTransactions(
        @RequestBody BatchInvestmentTransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.batchSaveTransactions(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<InvestmentTransactionDTO>> getTransactions(
        @RequestParam Long accountId,
        @RequestParam String startPeriod,
        @RequestParam String endPeriod
    ) {
        return ResponseEntity.ok(
            transactionService.getTransactions(accountId, startPeriod, endPeriod)
        );
    }
}
```

#### InvestmentAnalysisController

```java
@RestController
@RequestMapping("/api/investments/analysis")
@RequiredArgsConstructor
public class InvestmentAnalysisController {

    private final InvestmentAnalysisService analysisService;

    @GetMapping("/annual/by-category")
    public ResponseEntity<List<InvestmentCategoryAnalysisDTO>> getAnnualByCategory(
        @RequestParam Long familyId,
        @RequestParam Integer year,
        @RequestParam(defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(
            analysisService.getAnnualByCategory(familyId, year, currency)
        );
    }

    @GetMapping("/annual/by-account")
    public ResponseEntity<List<InvestmentAccountAnalysisDTO>> getAnnualByAccount(
        @RequestParam Long familyId,
        @RequestParam Integer year,
        @RequestParam(required = false) Long assetTypeId,
        @RequestParam(defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(
            analysisService.getAnnualByAccount(familyId, year, assetTypeId, currency)
        );
    }

    @GetMapping("/account-monthly-trend")
    public ResponseEntity<AccountMonthlyTrendResponseDTO> getAccountMonthlyTrend(
        @RequestParam Long accountId,
        @RequestParam Integer year
    ) {
        AccountMonthlyTrendResponseDTO response = analysisService
            .getAccountMonthlyTrend(accountId, year);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
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

#### investment.js

```javascript
import request from '@/utils/request'

// 账户管理
export const investmentAccountAPI = {
  getAll: (familyId) => request.get('/api/investments/accounts', {
    params: { familyId }
  }),
  getByCategory: (familyId, assetTypeId) => request.get('/api/investments/accounts/by-category', {
    params: { familyId, assetTypeId }
  }),
  getCategories: () => request.get('/api/investments/categories')
}

// 交易记录
export const investmentTransactionAPI = {
  batchSave: (data) => request.post('/api/investments/transactions/batch', data),
  getByAccount: (accountId, startPeriod, endPeriod) => request.get('/api/investments/transactions', {
    params: { accountId, startPeriod, endPeriod }
  })
}

// 分析报表
export const investmentAnalysisAPI = {
  getAnnualByCategory: (familyId, year, currency) =>
    request.get('/api/investments/analysis/annual/by-category', {
      params: { familyId, year, currency }
    }),
  getAnnualByAccount: (familyId, year, assetTypeId, currency) =>
    request.get('/api/investments/analysis/annual/by-account', {
      params: { familyId, year, assetTypeId, currency }
    }),
  getAccountMonthlyTrend: (accountId, year) =>
    request.get('/api/investments/analysis/account-monthly-trend', {
      params: { accountId, year }
    })
}
```

### 4.3 核心组件

#### InvestmentBatchEntry.vue

**功能**：批量录入投资交易记录

**核心状态**：
```javascript
// Tab状态
const activeTab = ref('by-account') // 'by-month' | 'by-account'

// 按月份模式
const selectedFamilyId = ref(null)
const transactionPeriod = ref('')
const monthAccounts = ref([])
const monthAmounts = ref({}) // {accountId: {deposits, withdrawals}}
const monthHistoryData = ref({}) // 历史3个月数据

// 按账户模式
const selectedYear = ref(new Date().getFullYear())
const selectedAccountId = ref('')
const yearMonthAmounts = ref({}) // {month: {deposits, withdrawals}}
const quickFillDeposit = ref(null)
const quickFillWithdrawal = ref(null)
```

**计算属性**：
```javascript
// 按月份模式 - 统计数据
const monthSummary = computed(() => {
  let totalDeposits = 0
  let totalWithdrawals = 0

  filteredMonthAccounts.value.forEach(account => {
    const amounts = monthAmounts.value[account.accountId]
    if (amounts) {
      totalDeposits += parseFloat(amounts.deposits) || 0
      totalWithdrawals += parseFloat(amounts.withdrawals) || 0
    }
  })

  return {
    totalDeposits,
    totalWithdrawals,
    netInvestment: totalDeposits - totalWithdrawals
  }
})

// 按账户模式 - 年度汇总
const yearSummary = computed(() => {
  let totalDeposits = 0
  let totalWithdrawals = 0

  for (let i = 1; i <= 12; i++) {
    totalDeposits += parseFloat(yearMonthAmounts.value[i].deposits) || 0
    totalWithdrawals += parseFloat(yearMonthAmounts.value[i].withdrawals) || 0
  }

  return {
    totalDeposits,
    totalWithdrawals,
    netInvestment: totalDeposits - totalWithdrawals
  }
})
```

**核心方法**：
```javascript
// 按月份模式 - 加载历史数据（前3个月）
async function loadMonthHistoryData() {
  const [year, month] = transactionPeriod.value.split('-').map(Number)

  for (const account of monthAccounts.value) {
    const history = {
      month1: 0, month2: 0, month3: 0,
      month1Withdrawals: 0, month2Withdrawals: 0, month3Withdrawals: 0
    }

    for (let i = 1; i <= 3; i++) {
      const date = new Date(year, month - 1 - i, 1)
      const period = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

      try {
        const response = await investmentTransactionAPI.getByAccount(
          account.accountId, period, period
        )
        if (response.success && response.data.length > 0) {
          const deposits = response.data
            .filter(tx => tx.transactionType === 'DEPOSIT')
            .reduce((sum, tx) => sum + parseFloat(tx.amount), 0)
          const withdrawals = response.data
            .filter(tx => tx.transactionType === 'WITHDRAWAL')
            .reduce((sum, tx) => sum + parseFloat(tx.amount), 0)
          history[`month${i}`] = deposits
          history[`month${i}Withdrawals`] = withdrawals
        }
      } catch (error) {
        console.error(`加载账户${account.accountId}历史数据失败:`, error)
      }
    }

    monthHistoryData.value[account.accountId] = history
  }
}

// 保存全部（按月份）
async function saveMonthData() {
  if (!hasMonthChanges.value) return

  savingMonth.value = true
  try {
    const transactions = monthAccounts.value.map(account => ({
      accountId: account.accountId,
      deposits: parseFloat(monthAmounts.value[account.accountId].deposits) || null,
      withdrawals: parseFloat(monthAmounts.value[account.accountId].withdrawals) || null,
      description: `${transactionPeriod.value} 批量录入`
    }))

    const response = await investmentTransactionAPI.batchSave({
      familyId: selectedFamilyId.value,
      transactionPeriod: transactionPeriod.value,
      transactions
    })

    if (response.success) {
      changedMonthAccounts.value.clear()
      alert(`保存成功！创建: ${response.data.created}, 更新: ${response.data.updated}, 删除: ${response.data.deleted}`)
    }
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败：' + error.message)
  } finally {
    savingMonth.value = false
  }
}

// 快速填充 - 定投功能
const fillAllDeposits = () => {
  if (!quickFillDeposit.value) return

  for (let month = 1; month <= 12; month++) {
    yearMonthAmounts.value[month].deposits = quickFillDeposit.value
    markYearChanged(month)
  }
}
```

#### InvestmentAnalysis.vue

**功能**：年度投资分析

**核心状态**：
```javascript
const selectedFamilyId = ref(null)
const selectedYear = ref(new Date().getFullYear())
const selectedCurrency = ref('All')
const selectedCategory = ref(null)
const selectedAccount = ref(null)

const categoryAnalysis = ref([])
const accountAnalysis = ref([])
const monthlyTrend = ref(null)
```

**数据加载**：
```javascript
// 加载大类分析
async function loadCategoryAnalysis() {
  const response = await investmentAnalysisAPI.getAnnualByCategory(
    selectedFamilyId.value,
    selectedYear.value,
    selectedCurrency.value
  )
  categoryAnalysis.value = response.data
}

// 加载账户分析
async function loadAccountAnalysis() {
  if (!selectedCategory.value) return

  const response = await investmentAnalysisAPI.getAnnualByAccount(
    selectedFamilyId.value,
    selectedYear.value,
    selectedCategory.value.categoryId,
    selectedCurrency.value
  )
  accountAnalysis.value = response.data
}

// 加载月度趋势
async function loadMonthlyTrend() {
  if (!selectedAccount.value) return

  const response = await investmentAnalysisAPI.getAccountMonthlyTrend(
    selectedAccount.value.accountId,
    selectedYear.value
  )
  monthlyTrend.value = response.data
}
```

**图表展示**：
```javascript
// 大类分布饼图
const categoryChartOption = computed(() => ({
  title: { text: `${selectedYear.value}年度投资大类分布` },
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: '70%',
    data: categoryAnalysis.value.map(item => ({
      name: item.categoryName,
      value: item.currentAssets
    }))
  }]
}))

// 月度趋势柱状图
const monthlyTrendChartOption = computed(() => {
  if (!monthlyTrend.value) return {}

  return {
    title: { text: '月度投入和取出趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: monthlyTrend.value.monthlyData.map(d => `${d.month}月`)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '投入',
        type: 'bar',
        data: monthlyTrend.value.monthlyData.map(d => d.deposits),
        itemStyle: { color: '#10b981' }
      },
      {
        name: '取出',
        type: 'bar',
        data: monthlyTrend.value.monthlyData.map(d => d.withdrawals),
        itemStyle: { color: '#ef4444' }
      }
    ]
  }
})
```

## 5. 性能优化

### 5.1 数据库优化

1. **索引优化**：
   - 复合索引覆盖查询条件
   - 定期分析索引使用情况（`EXPLAIN`）

2. **查询优化**：
   - 避免N+1查询问题（使用JOIN FETCH）
   - 子查询优化（使用EXISTS代替IN）
   - 分页查询大数据集

3. **连接池配置**：
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
   ```

### 5.2 后端优化

1. **缓存策略**：
   - Redis缓存资产类型列表（更新频率低）
   - 本地缓存汇率数据（按日期）

2. **批量操作**：
   - 批量查询减少数据库往返
   - 批量插入/更新使用batch

3. **异步处理**：
   - 年度汇总计算使用异步任务
   - 大数据导出使用后台任务

### 5.3 前端优化

1. **数据加载**：
   - 并行请求历史数据（Promise.all）
   - 懒加载账户明细

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
        if (ex.getMessage().contains("uk_account_period_type")) {
            message = "该账户该期间该类型已有交易记录";
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
class InvestmentAnalysisServiceTest {

    @Autowired
    private InvestmentAnalysisService analysisService;

    @Test
    void testGetAnnualByCategory() {
        List<InvestmentCategoryAnalysisDTO> result =
            analysisService.getAnnualByCategory(1L, 2025, "USD");

        assertNotNull(result);
        assertTrue(result.size() > 0);

        // 验证回报率计算
        result.forEach(dto -> {
            assertNotNull(dto.getReturnRate());
        });
    }

    @Test
    void testRealEstateReturnsCalculation() {
        // 测试房地产投资回报计算逻辑
        // 验证净资产 = 房产 - 房贷
        // 验证本金还款计入净投入
    }
}
```

### 7.2 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetInvestmentAccounts() throws Exception {
        mockMvc.perform(get("/api/investments/accounts")
                .param("familyId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testBatchSaveTransactions() throws Exception {
        String json = """
            {
              "familyId": 1,
              "transactionPeriod": "2025-01",
              "transactions": [
                {
                  "accountId": 1,
                  "deposits": 1000,
                  "withdrawals": 0,
                  "description": "定投"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/investments/transactions/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").exists());
    }
}
```

## 8. 部署说明

### 8.1 数据库初始化

```bash
# 1. 执行建表脚本
mysql -u root -p finance < V002__create_asset_tables.sql

# 2. 执行初始化数据
mysql -u root -p finance < V003__insert_asset_type_data.sql

# 3. 创建投资交易表
mysql -u root -p finance < V004__create_investment_transactions.sql
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

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

### 8.3 定时任务

```java
@Scheduled(cron = "0 0 3 1 * ?") // 每月1日凌晨3点执行
public void monthlyAssetSnapshot() {
    // 生成上月资产快照
    LocalDate lastMonth = LocalDate.now().minusMonths(1);
    // 为所有投资账户创建资产记录
}
```

## 9. 未来扩展

### 9.1 交易明细

- 支持记录每笔交易详情
- 包括：股票代码、数量、单价等

### 9.2 持仓分析

- 查看每个账户的持仓明细
- 成本分析、收益分析

### 9.3 智能建议

- 资产配置建议
- 再平衡提醒
- 风险评估

### 9.4 第三方集成

- 券商账户自动同步
- 银行账户自动同步
- 实时行情数据
