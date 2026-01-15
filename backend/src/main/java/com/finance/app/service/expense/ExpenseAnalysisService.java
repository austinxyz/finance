package com.finance.app.service.expense;

import com.finance.app.dto.expense.AnnualExpenseSummaryDTO;
import com.finance.app.dto.expense.BudgetExecutionDTO;
import com.finance.app.dto.expense.ExpenseAnnualMajorCategoryDTO;
import com.finance.app.dto.expense.ExpenseAnnualMinorCategoryDTO;
import com.finance.app.dto.expense.ExpenseMonthlyTrendDTO;
import com.finance.app.model.AnnualExpenseSummary;
import com.finance.app.model.ExchangeRate;
import com.finance.app.model.ExpenseBudget;
import com.finance.app.model.ExpenseCategoryMajor;
import com.finance.app.model.ExpenseCategoryMinor;
import com.finance.app.model.ExpenseRecord;
import com.finance.app.repository.AnnualExpenseSummaryRepository;
import com.finance.app.repository.ExchangeRateRepository;
import com.finance.app.repository.ExpenseBudgetRepository;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.ExpenseCategoryMinorRepository;
import com.finance.app.repository.ExpenseRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseAnalysisService {

    @Autowired
    private ExpenseRecordRepository expenseRecordRepository;

    @Autowired
    private ExpenseCategoryMajorRepository majorCategoryRepository;

    @Autowired
    private ExpenseBudgetRepository expenseBudgetRepository;

    @Autowired
    private ExpenseCategoryMinorRepository minorCategoryRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private AnnualExpenseSummaryRepository annualExpenseSummaryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 获取年度大类汇总
     */
    public List<ExpenseAnnualMajorCategoryDTO> getAnnualMajorCategorySummary(Long familyId, Integer year, String currency) {
        // 1. 构建起止期间
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";

        // 2. 查询所有记录
        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndExpensePeriodBetween(
                familyId, startPeriod, endPeriod);

        // 3. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 4. 按大类分组并汇总
        Map<Long, BigDecimal> majorCategoryTotals = new HashMap<>();
        Map<String, BigDecimal> finalRateMap = rateMap;  // 用于lambda

        for (ExpenseRecord record : records) {
            // 过滤货币（如果不是"All"模式）
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                continue;
            }

            Long majorId = record.getMinorCategory().getMajorCategory().getId();
            BigDecimal amount;

            if ("All".equalsIgnoreCase(currency)) {
                // All模式：转换为USD
                amount = convertToUSD(record.getAmount(), record.getCurrency(), finalRateMap);
            } else {
                // 特定货币模式：直接累加
                amount = record.getAmount();
            }

            majorCategoryTotals.merge(majorId, amount, BigDecimal::add);
        }

        // 5. 构建DTO列表
        List<ExpenseAnnualMajorCategoryDTO> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : majorCategoryTotals.entrySet()) {
            ExpenseCategoryMajor major = majorCategoryRepository.findById(entry.getKey())
                    .orElse(null);
            if (major != null) {
                result.add(new ExpenseAnnualMajorCategoryDTO(
                        major.getId(),
                        major.getName(),
                        major.getIcon(),
                        major.getCode(),
                        entry.getValue(),
                        "All".equalsIgnoreCase(currency) ? "USD" : currency
                ));
            }
        }

        // 6. 按金额降序排序
        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        return result;
    }

    /**
     * 获取年度小类汇总（某个大类下）
     */
    public List<ExpenseAnnualMinorCategoryDTO> getAnnualMinorCategorySummary(Long familyId, Integer year,
                                                                             Long majorCategoryId, String currency) {
        // 1. 构建起止期间
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";

        // 2. 查询所有记录
        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndExpensePeriodBetween(
                familyId, startPeriod, endPeriod);

        // 3. 过滤出该大类下的记录
        records = records.stream()
                .filter(r -> r.getMinorCategory().getMajorCategory().getId().equals(majorCategoryId))
                .collect(Collectors.toList());

        // 4. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 5. 按小类分组并汇总
        Map<Long, BigDecimal> minorCategoryTotals = new HashMap<>();
        Map<String, BigDecimal> finalRateMap = rateMap;

        for (ExpenseRecord record : records) {
            // 过滤货币
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                continue;
            }

            Long minorId = record.getMinorCategory().getId();
            BigDecimal amount;

            if ("All".equalsIgnoreCase(currency)) {
                amount = convertToUSD(record.getAmount(), record.getCurrency(), finalRateMap);
            } else {
                amount = record.getAmount();
            }

            minorCategoryTotals.merge(minorId, amount, BigDecimal::add);
        }

        // 6. 构建DTO列表
        List<ExpenseAnnualMinorCategoryDTO> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : minorCategoryTotals.entrySet()) {
            ExpenseCategoryMinor minor = minorCategoryRepository.findById(entry.getKey())
                    .orElse(null);
            if (minor != null) {
                ExpenseCategoryMajor major = minor.getMajorCategory();
                result.add(new ExpenseAnnualMinorCategoryDTO(
                        minor.getId(),
                        minor.getName(),
                        major.getId(),
                        major.getName(),
                        major.getIcon(),
                        minor.getExpenseType(),
                        entry.getValue(),
                        "All".equalsIgnoreCase(currency) ? "USD" : currency
                ));
            }
        }

        // 7. 按金额降序排序
        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        return result;
    }

    /**
     * 获取月度趋势（某个小类的12个月数据）
     */
    public List<ExpenseMonthlyTrendDTO> getAnnualMonthlyTrend(Long familyId, Integer year,
                                                              Long minorCategoryId, String currency) {
        // 1. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 2. 构建12个月的结果
        List<ExpenseMonthlyTrendDTO> result = new ArrayList<>();
        Map<String, BigDecimal> finalRateMap = rateMap;

        for (int month = 1; month <= 12; month++) {
            String period = String.format("%d-%02d", year, month);

            // 查询该月该小类的所有记录
            List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndExpensePeriodAndMinorCategoryId(
                    familyId, period, minorCategoryId);

            BigDecimal monthTotal = BigDecimal.ZERO;

            for (ExpenseRecord record : records) {
                // 过滤货币
                if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                    continue;
                }

                BigDecimal amount;
                if ("All".equalsIgnoreCase(currency)) {
                    amount = convertToUSD(record.getAmount(), record.getCurrency(), finalRateMap);
                } else {
                    amount = record.getAmount();
                }

                monthTotal = monthTotal.add(amount);
            }

            result.add(new ExpenseMonthlyTrendDTO(
                    month,
                    period,
                    monthTotal,
                    "All".equalsIgnoreCase(currency) ? "USD" : currency
            ));
        }

        return result;
    }

    /**
     * 加载指定年份的所有汇率
     */
    private Map<String, BigDecimal> loadExchangeRates(Integer year) {
        Map<String, BigDecimal> rateMap = new HashMap<>();

        // 查询该年末的汇率（12-31）
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // 获取所有活跃的汇率
        List<ExchangeRate> rates = exchangeRateRepository.findByIsActiveTrueOrderByEffectiveDateDesc();

        // 找每个货币在该年末或之前最近的汇率
        Set<String> processedCurrencies = new HashSet<>();
        for (ExchangeRate rate : rates) {
            if (rate.getEffectiveDate().isAfter(yearEnd)) {
                continue;  // 跳过该年之后的汇率
            }
            if (processedCurrencies.contains(rate.getCurrency())) {
                continue;  // 已经有更近的汇率了
            }
            rateMap.put(rate.getCurrency(), rate.getRateToUsd());
            processedCurrencies.add(rate.getCurrency());
        }

        // 手动添加USD基准货币
        rateMap.put("USD", BigDecimal.ONE);

        // Note: Default rates for all currencies should be in DB (e.g., 2000-01-01 for CNY)
        // No hardcoded fallback needed here

        return rateMap;
    }

    /**
     * 转换为USD
     */
    private BigDecimal convertToUSD(BigDecimal amount, String currency, Map<String, BigDecimal> rateMap) {
        if ("USD".equals(currency)) {
            return amount;
        }

        BigDecimal rate = rateMap.get(currency);
        if (rate == null) {
            // If rate not found, use 1.0 (no conversion)
            // Note: Default rates should be in DB (e.g., 2000-01-01 for CNY)
            rate = BigDecimal.ONE;
        }

        return amount.multiply(rate);
    }

    /**
     * 获取预算执行分析
     */
    public List<BudgetExecutionDTO> getBudgetExecution(Long familyId, Integer budgetYear, String currency) {
        // 1. 查询该年所有预算
        List<ExpenseBudget> budgets = expenseBudgetRepository.findByFamilyIdAndBudgetYear(familyId, budgetYear);

        // 2. 查询该年所有实际支出
        String startPeriod = budgetYear + "-01";
        String endPeriod = budgetYear + "-12";
        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndExpensePeriodBetween(
                familyId, startPeriod, endPeriod);

        // 3. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(budgetYear);
        }

        // 4. 按小类+货币分组统计实际支出
        Map<String, BigDecimal> actualTotals = new HashMap<>();
        Map<String, Long> minorCategoryIds = new HashMap<>();  // 记录每个key对应的小类ID
        Map<String, BigDecimal> finalRateMap = rateMap;

        for (ExpenseRecord record : records) {
            // 过滤货币
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                continue;
            }

            String key = record.getMinorCategory().getId() + "_" + record.getCurrency();
            minorCategoryIds.put(key, record.getMinorCategory().getId());

            BigDecimal amount;

            if ("All".equalsIgnoreCase(currency)) {
                amount = convertToUSD(record.getAmount(), record.getCurrency(), finalRateMap);
            } else {
                amount = record.getAmount();
            }

            actualTotals.merge(key, amount, BigDecimal::add);
        }

        // 5. 构建预算执行结果
        List<BudgetExecutionDTO> result = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();  // 记录已处理的小类+货币组合

        // 先处理有预算的项目
        for (ExpenseBudget budget : budgets) {
            // 过滤货币
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(budget.getCurrency())) {
                continue;
            }

            // 加载小类和大类信息
            ExpenseCategoryMinor minor = minorCategoryRepository.findById(budget.getMinorCategoryId())
                    .orElse(null);
            if (minor == null) {
                continue;  // 小类不存在，跳过
            }
            ExpenseCategoryMajor major = minor.getMajorCategory();

            // 计算预算金额
            BigDecimal budgetAmount;
            if ("All".equalsIgnoreCase(currency)) {
                budgetAmount = convertToUSD(budget.getBudgetAmount(), budget.getCurrency(), finalRateMap);
            } else {
                budgetAmount = budget.getBudgetAmount();
            }

            // 查找实际金额
            String key = minor.getId() + "_" + budget.getCurrency();
            BigDecimal actualAmount = actualTotals.getOrDefault(key, BigDecimal.ZERO);
            processedKeys.add(key);  // 标记已处理

            // 计算差异和执行率
            BigDecimal variance = actualAmount.subtract(budgetAmount);
            BigDecimal executionRate = BigDecimal.ZERO;
            if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
                executionRate = actualAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            result.add(new BudgetExecutionDTO(
                    minor.getId(),
                    minor.getName(),
                    major.getId(),
                    major.getName(),
                    major.getIcon(),
                    minor.getExpenseType(),
                    budgetAmount,
                    actualAmount,
                    variance,
                    executionRate,
                    "All".equalsIgnoreCase(currency) ? "USD" : currency
            ));
        }

        // 5.5. 处理没有预算但有实际支出的项目
        for (Map.Entry<String, BigDecimal> entry : actualTotals.entrySet()) {
            String key = entry.getKey();
            if (processedKeys.contains(key)) {
                continue;  // 已经处理过了
            }

            Long minorId = minorCategoryIds.get(key);
            if (minorId == null) {
                continue;
            }

            // 加载小类和大类信息
            ExpenseCategoryMinor minor = minorCategoryRepository.findById(minorId).orElse(null);
            if (minor == null) {
                continue;
            }
            ExpenseCategoryMajor major = minor.getMajorCategory();

            BigDecimal actualAmount = entry.getValue();
            BigDecimal budgetAmount = BigDecimal.ZERO;  // 没有预算
            BigDecimal variance = actualAmount;  // 差异 = 实际金额
            BigDecimal executionRate = BigDecimal.ZERO;  // 无预算时执行率为0

            result.add(new BudgetExecutionDTO(
                    minor.getId(),
                    minor.getName(),
                    major.getId(),
                    major.getName(),
                    major.getIcon(),
                    minor.getExpenseType(),
                    budgetAmount,
                    actualAmount,
                    variance,
                    executionRate,
                    "All".equalsIgnoreCase(currency) ? "USD" : currency
            ));
        }

        // 6. 排序：先按大类ID，再按小类ID
        result.sort((a, b) -> {
            int majorCompare = a.getMajorCategoryId().compareTo(b.getMajorCategoryId());
            if (majorCompare != 0) {
                return majorCompare;
            }
            return a.getMinorCategoryId().compareTo(b.getMinorCategoryId());
        });

        return result;
    }

    /**
     * 获取年度支出汇总(包含资产/负债调整)
     * 从annual_expense_summary表查询预计算的汇总数据
     */
    public List<AnnualExpenseSummaryDTO> getAnnualExpenseSummaryWithAdjustments(
            Long familyId, Integer year, String currency, boolean includeTotals) {

        // annual_expense_summary stores USD-converted summaries
        // If user selects CNY, we convert USD amounts to CNY using year-end exchange rate
        List<AnnualExpenseSummary> summaries = annualExpenseSummaryRepository
                .findMajorCategorySummary(familyId, year);

        List<AnnualExpenseSummaryDTO> result = new ArrayList<>();

        // Determine display currency and conversion rate
        String displayCurrency = "All".equalsIgnoreCase(currency) ? "USD" : currency;
        BigDecimal conversionRate = BigDecimal.ONE;

        // If CNY is selected, get year-end exchange rate to convert USD to CNY
        if ("CNY".equalsIgnoreCase(currency)) {
            LocalDate yearEnd = LocalDate.of(year, 12, 31);
            List<ExchangeRate> rates = exchangeRateRepository.findByIsActiveTrueOrderByEffectiveDateDesc();

            for (ExchangeRate rate : rates) {
                if ("CNY".equals(rate.getCurrency()) && !rate.getEffectiveDate().isAfter(yearEnd)) {
                    // CNY to USD rate is stored, we need USD to CNY rate (inverse)
                    if (rate.getRateToUsd().compareTo(BigDecimal.ZERO) > 0) {
                        conversionRate = BigDecimal.ONE.divide(rate.getRateToUsd(), 4, RoundingMode.HALF_UP);
                    }
                    break;
                }
            }

            // If no rate found (should not happen with 2000-01-01 default rate in DB)
            // Use 1.0 as fallback to avoid division by zero
            if (conversionRate.compareTo(BigDecimal.ONE) == 0) {
                conversionRate = BigDecimal.ONE;
            }
        }

        for (AnnualExpenseSummary summary : summaries) {
            // 获取大类信息
            ExpenseCategoryMajor major = summary.getMajorCategory();

            AnnualExpenseSummaryDTO dto = new AnnualExpenseSummaryDTO();
            dto.setSummaryYear(summary.getSummaryYear());
            dto.setMajorCategoryId(summary.getMajorCategoryId());
            dto.setMajorCategoryName(major != null ? major.getName() : null);
            dto.setMajorCategoryIcon(major != null ? major.getIcon() : null);
            dto.setMajorCategoryCode(major != null ? major.getCode() : null);
            dto.setMinorCategoryId(null);
            dto.setMinorCategoryName(null);

            // Convert amounts if needed
            dto.setBaseExpenseAmount(summary.getBaseExpenseAmount().multiply(conversionRate));
            dto.setSpecialExpense(summary.getSpecialExpenseAmount().multiply(conversionRate));
            dto.setAssetAdjustment(summary.getAssetAdjustment().multiply(conversionRate));
            dto.setLiabilityAdjustment(summary.getLiabilityAdjustment().multiply(conversionRate));
            dto.setActualExpenseAmount(summary.getActualExpenseAmount().multiply(conversionRate));
            dto.setCurrency(displayCurrency);
            dto.setAdjustmentDetails(summary.getAdjustmentDetails());

            result.add(dto);
        }

        // 如果需要总计,添加总计记录
        if (includeTotals) {
            AnnualExpenseSummary totalSummary = annualExpenseSummaryRepository
                    .findTotalSummary(familyId, year);

            if (totalSummary != null) {
                AnnualExpenseSummaryDTO totalDTO = new AnnualExpenseSummaryDTO();
                totalDTO.setSummaryYear(totalSummary.getSummaryYear());
                totalDTO.setMajorCategoryId(null);
                totalDTO.setMajorCategoryName("总计");
                totalDTO.setMajorCategoryIcon(null);
                totalDTO.setMajorCategoryCode("TOTAL");

                // Convert amounts if needed
                totalDTO.setBaseExpenseAmount(totalSummary.getBaseExpenseAmount().multiply(conversionRate));
                totalDTO.setSpecialExpense(totalSummary.getSpecialExpenseAmount().multiply(conversionRate));
                totalDTO.setAssetAdjustment(totalSummary.getAssetAdjustment().multiply(conversionRate));
                totalDTO.setLiabilityAdjustment(totalSummary.getLiabilityAdjustment().multiply(conversionRate));
                totalDTO.setActualExpenseAmount(totalSummary.getActualExpenseAmount().multiply(conversionRate));
                totalDTO.setCurrency(displayCurrency);
                totalDTO.setAdjustmentDetails(totalSummary.getAdjustmentDetails());

                result.add(totalDTO);
            }
        }

        return result;
    }

    /**
     * 计算年度支出汇总（运行存储过程）
     */
    @Transactional
    public void calculateAnnualExpenseSummary(Long familyId, Integer year) {
        try {
            // 调用存储过程 calculate_annual_expense_summary_v3
            entityManager.createNativeQuery("CALL calculate_annual_expense_summary_v3(:familyId, :year)")
                    .setParameter("familyId", familyId)
                    .setParameter("year", year)
                    .executeUpdate();

            // 刷新实体管理器
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            throw new RuntimeException("计算年度支出汇总失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取多年度支出趋势分析
     * 返回最近N年的年度总支出(基础和实际)及同比增长率
     */
    public List<Map<String, Object>> getAnnualExpenseTrend(Long familyId, Integer limit, String currency) {
        // 1. 获取最近N年的年度汇总数据
        List<AnnualExpenseSummary> summaries = annualExpenseSummaryRepository
                .findByFamilyIdOrderByYearDesc(familyId);

        if (summaries.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 限制年份数量
        if (limit != null && limit > 0 && summaries.size() > limit) {
            summaries = summaries.subList(0, limit);
        }

        // 3. 按年份升序排列（从旧到新）
        summaries.sort(Comparator.comparing(AnnualExpenseSummary::getSummaryYear));

        // 4. 加载汇率（用于货币转换）
        Map<Integer, Map<String, BigDecimal>> exchangeRatesByYear = new HashMap<>();
        for (AnnualExpenseSummary summary : summaries) {
            if (!currency.equals(summary.getCurrency())) {
                exchangeRatesByYear.put(summary.getSummaryYear(), loadExchangeRates(summary.getSummaryYear()));
            }
        }

        // 5. 构建结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal prevBaseExpense = null;
        BigDecimal prevActualExpense = null;

        for (int i = 0; i < summaries.size(); i++) {
            AnnualExpenseSummary summary = summaries.get(i);
            Map<String, Object> yearData = new HashMap<>();

            // 转换货币
            BigDecimal baseExpense = summary.getBaseExpenseAmount();
            BigDecimal specialExpense = summary.getSpecialExpenseAmount();
            BigDecimal assetAdjustment = summary.getAssetAdjustment();
            BigDecimal liabilityAdjustment = summary.getLiabilityAdjustment();
            BigDecimal actualExpense = summary.getActualExpenseAmount();

            if (!currency.equals(summary.getCurrency())) {
                Map<String, BigDecimal> rates = exchangeRatesByYear.get(summary.getSummaryYear());
                baseExpense = convertCurrency(baseExpense, summary.getCurrency(), currency, rates);
                specialExpense = convertCurrency(specialExpense, summary.getCurrency(), currency, rates);
                assetAdjustment = convertCurrency(assetAdjustment, summary.getCurrency(), currency, rates);
                liabilityAdjustment = convertCurrency(liabilityAdjustment, summary.getCurrency(), currency, rates);
                actualExpense = convertCurrency(actualExpense, summary.getCurrency(), currency, rates);
            }

            // 计算总调整值（资产调整 + 负债调整）
            BigDecimal totalAdjustment = (assetAdjustment != null ? assetAdjustment : BigDecimal.ZERO)
                    .add(liabilityAdjustment != null ? liabilityAdjustment : BigDecimal.ZERO);

            // 基本信息
            yearData.put("year", summary.getSummaryYear());
            yearData.put("baseExpense", baseExpense);
            yearData.put("specialExpense", specialExpense != null ? specialExpense : BigDecimal.ZERO);
            yearData.put("assetAdjustment", assetAdjustment != null ? assetAdjustment : BigDecimal.ZERO);
            yearData.put("liabilityAdjustment", liabilityAdjustment != null ? liabilityAdjustment : BigDecimal.ZERO);
            yearData.put("totalAdjustment", totalAdjustment);
            yearData.put("actualExpense", actualExpense);
            yearData.put("currency", currency);

            // 计算同比增长（与前一年比较）
            if (prevBaseExpense != null && prevBaseExpense.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal baseChange = baseExpense.subtract(prevBaseExpense);
                BigDecimal baseChangePct = baseChange.divide(prevBaseExpense, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                yearData.put("yoyBaseChange", baseChange);
                yearData.put("yoyBaseChangePct", baseChangePct);
            } else {
                yearData.put("yoyBaseChange", null);
                yearData.put("yoyBaseChangePct", null);
            }

            if (prevActualExpense != null && prevActualExpense.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal actualChange = actualExpense.subtract(prevActualExpense);
                BigDecimal actualChangePct = actualChange.divide(prevActualExpense, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                yearData.put("yoyActualChange", actualChange);
                yearData.put("yoyActualChangePct", actualChangePct);
            } else {
                yearData.put("yoyActualChange", null);
                yearData.put("yoyActualChangePct", null);
            }

            prevBaseExpense = baseExpense;
            prevActualExpense = actualExpense;

            result.add(yearData);
        }

        return result;
    }

    /**
     * 货币转换辅助方法
     */
    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency,
                                       Map<String, BigDecimal> rates) {
        if (amount == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // 先转为USD
        BigDecimal amountInUSD = amount;
        if (!"USD".equals(fromCurrency)) {
            BigDecimal fromRate = rates.getOrDefault(fromCurrency, BigDecimal.ONE);
            amountInUSD = amount.multiply(fromRate);
        }

        // 再转为目标货币
        if (!"USD".equals(toCurrency)) {
            BigDecimal toRate = rates.getOrDefault(toCurrency, BigDecimal.ONE);
            return amountInUSD.divide(toRate, 2, RoundingMode.HALF_UP);
        }

        return amountInUSD;
    }

    /**
     * 获取各大类的多年度基础支出趋势
     * 返回每个大类最近N年的基础支出数据
     */
    public List<Map<String, Object>> getAnnualCategoryTrend(Long familyId, Integer limit, String currency) {
        // 1. 获取所有大类
        List<ExpenseCategoryMajor> majorCategories = majorCategoryRepository.findAll();

        // 2. 获取年度汇总数据（大类级别），只获取指定货币的数据
        List<AnnualExpenseSummary> allSummaries = annualExpenseSummaryRepository
                .findByFamilyIdAndMajorCategoryIdNot(familyId, 0L); // 排除总计记录

        // 过滤出指定货币的记录
        allSummaries = allSummaries.stream()
                .filter(s -> currency.equals(s.getCurrency()))
                .collect(Collectors.toList());

        if (allSummaries.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 限制年份数量 - 获取最近的N个年份
        List<Integer> allYearsList = allSummaries.stream()
                .map(AnnualExpenseSummary::getSummaryYear)
                .distinct()
                .sorted((a, b) -> b - a) // 降序
                .collect(Collectors.toList());

        if (limit != null && limit > 0 && allYearsList.size() > limit) {
            allYearsList = allYearsList.subList(0, limit);
        }

        final List<Integer> allYears = allYearsList; // 创建final变量供lambda使用

        // 4. 按大类分组数据
        Map<Long, List<AnnualExpenseSummary>> summariesByCategory = allSummaries.stream()
                .filter(s -> allYears.contains(s.getSummaryYear()))
                .collect(Collectors.groupingBy(AnnualExpenseSummary::getMajorCategoryId));

        // 5. 构建结果列表
        List<Map<String, Object>> result = new ArrayList<>();

        for (ExpenseCategoryMajor major : majorCategories) {
            List<AnnualExpenseSummary> categorySummaries = summariesByCategory.get(major.getId());
            if (categorySummaries == null || categorySummaries.isEmpty()) {
                continue; // 该大类没有数据，跳过
            }

            // 按年份排序（从旧到新）
            categorySummaries.sort(Comparator.comparing(AnnualExpenseSummary::getSummaryYear));

            // 构建年度数据列表（按年份去重，只保留每年第一条记录）
            Map<Integer, AnnualExpenseSummary> yearToSummaryMap = new LinkedHashMap<>();
            for (AnnualExpenseSummary summary : categorySummaries) {
                Integer year = summary.getSummaryYear();
                if (!yearToSummaryMap.containsKey(year)) {
                    yearToSummaryMap.put(year, summary);
                }
            }

            List<Map<String, Object>> yearlyData = new ArrayList<>();
            for (Map.Entry<Integer, AnnualExpenseSummary> entry : yearToSummaryMap.entrySet()) {
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", entry.getKey());
                yearData.put("baseExpense", entry.getValue().getBaseExpenseAmount());
                yearData.put("actualExpense", entry.getValue().getActualExpenseAmount());
                yearlyData.add(yearData);
            }

            // 构建大类数据
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("majorCategoryId", major.getId());
            categoryData.put("majorCategoryName", major.getName());
            categoryData.put("majorCategoryIcon", major.getIcon());
            categoryData.put("majorCategoryCode", major.getCode());
            categoryData.put("currency", currency);
            categoryData.put("yearlyData", yearlyData);

            result.add(categoryData);
        }

        // 6. 按大类ID排序
        result.sort((a, b) -> {
            Long idA = (Long) a.get("majorCategoryId");
            Long idB = (Long) b.get("majorCategoryId");
            return idA.compareTo(idB);
        });

        return result;
    }

    /**
     * 获取年度汇总表（返回USD基准货币数据）
     * 横坐标：年份
     * 纵坐标：大类支出项 + 总计
     * 数据格式：实际支出（基础支出），同比为实际百分比（基础百分比）
     */
    public Map<String, Object> getAnnualSummaryTable(Long familyId, Integer limit) {
        // 始终返回USD基准货币数据
        String currency = "USD";
        // 1. 获取所有大类
        List<ExpenseCategoryMajor> majorCategories = majorCategoryRepository.findAll();

        // 2. 获取年度汇总数据，只获取指定货币的数据
        List<AnnualExpenseSummary> allSummaries = annualExpenseSummaryRepository
                .findByFamilyIdAndCurrency(familyId, currency);

        if (allSummaries.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("years", new ArrayList<>());
            result.put("categories", new ArrayList<>());
            result.put("rows", new ArrayList<>());
            return result;
        }

        // 3. 获取年份列表（最近N年，降序）
        List<Integer> years = allSummaries.stream()
                .map(AnnualExpenseSummary::getSummaryYear)
                .distinct()
                .sorted((a, b) -> b - a) // 降序
                .limit(limit != null && limit > 0 ? limit : 999)
                .collect(Collectors.toList());

        // 4. 构建大类列表（按display_order排序）
        List<Map<String, Object>> categories = majorCategories.stream()
                .sorted(Comparator.comparing(ExpenseCategoryMajor::getSortOrder))
                .map(cat -> {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("id", cat.getId());
                    categoryMap.put("name", cat.getName());
                    categoryMap.put("icon", cat.getIcon());
                    categoryMap.put("code", cat.getCode());
                    categoryMap.put("color", cat.getColor());
                    return categoryMap;
                })
                .collect(Collectors.toList());

        // 5. 构建数据行（每年一行）
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < years.size(); i++) {
            Integer currentYear = years.get(i);
            Integer previousYear = i < years.size() - 1 ? years.get(i + 1) : null;

            Map<String, Object> row = new HashMap<>();
            row.put("year", currentYear);

            // 该年总计数据（major_category_id IS NULL 表示总计）
            AnnualExpenseSummary totalSummary = allSummaries.stream()
                    .filter(s -> s.getSummaryYear().equals(currentYear) && s.getMajorCategoryId() == null)
                    .findFirst()
                    .orElse(null);

            // 上一年总计数据（用于计算同比）
            AnnualExpenseSummary previousTotalSummary = null;
            if (previousYear != null) {
                previousTotalSummary = allSummaries.stream()
                        .filter(s -> s.getSummaryYear().equals(previousYear) && s.getMajorCategoryId() == null)
                        .findFirst()
                        .orElse(null);
            }

            // 构建各大类数据
            Map<String, Map<String, Object>> categoryDataMap = new HashMap<>();
            for (ExpenseCategoryMajor category : majorCategories) {
                AnnualExpenseSummary currentCategorySummary = allSummaries.stream()
                        .filter(s -> s.getSummaryYear().equals(currentYear) &&
                                    s.getMajorCategoryId() != null &&
                                    s.getMajorCategoryId().equals(category.getId()))
                        .findFirst()
                        .orElse(null);

                AnnualExpenseSummary previousCategorySummary = null;
                if (previousYear != null) {
                    previousCategorySummary = allSummaries.stream()
                            .filter(s -> s.getSummaryYear().equals(previousYear) &&
                                        s.getMajorCategoryId() != null &&
                                        s.getMajorCategoryId().equals(category.getId()))
                            .findFirst()
                            .orElse(null);
                }

                Map<String, Object> categoryData = new HashMap<>();

                if (currentCategorySummary != null) {
                    categoryData.put("actualExpense", currentCategorySummary.getActualExpenseAmount());
                    categoryData.put("baseExpense", currentCategorySummary.getBaseExpenseAmount());
                    categoryData.put("specialExpense", currentCategorySummary.getSpecialExpenseAmount());
                    categoryData.put("specialExpenseDetails", currentCategorySummary.getSpecialExpenseDetails());
                    categoryData.put("assetAdjustment", currentCategorySummary.getAssetAdjustment());
                    categoryData.put("liabilityAdjustment", currentCategorySummary.getLiabilityAdjustment());

                    // 计算同比
                    if (previousCategorySummary != null &&
                        previousCategorySummary.getActualExpenseAmount() != null &&
                        previousCategorySummary.getActualExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {

                        BigDecimal actualChange = currentCategorySummary.getActualExpenseAmount()
                                .subtract(previousCategorySummary.getActualExpenseAmount());
                        BigDecimal actualChangePct = actualChange
                                .divide(previousCategorySummary.getActualExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal("100"));

                        categoryData.put("actualChangePct", actualChangePct);

                        // 基础支出同比：检查除数是否为零
                        if (previousCategorySummary.getBaseExpenseAmount() != null &&
                            previousCategorySummary.getBaseExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {
                            BigDecimal baseChange = currentCategorySummary.getBaseExpenseAmount()
                                    .subtract(previousCategorySummary.getBaseExpenseAmount());
                            BigDecimal baseChangePct = baseChange
                                    .divide(previousCategorySummary.getBaseExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                                    .multiply(new BigDecimal("100"));
                            categoryData.put("baseChangePct", baseChangePct);
                        } else {
                            categoryData.put("baseChangePct", null);
                        }
                    } else {
                        categoryData.put("actualChangePct", null);
                        categoryData.put("baseChangePct", null);
                    }
                } else {
                    categoryData.put("actualExpense", BigDecimal.ZERO);
                    categoryData.put("baseExpense", BigDecimal.ZERO);
                    categoryData.put("specialExpense", BigDecimal.ZERO);
                    categoryData.put("specialExpenseDetails", null);
                    categoryData.put("assetAdjustment", BigDecimal.ZERO);
                    categoryData.put("liabilityAdjustment", BigDecimal.ZERO);
                    categoryData.put("actualChangePct", null);
                    categoryData.put("baseChangePct", null);
                }

                categoryDataMap.put(category.getCode(), categoryData);
            }

            row.put("categoryData", categoryDataMap);

            // 总计数据
            Map<String, Object> totalData = new HashMap<>();
            if (totalSummary != null) {
                totalData.put("actualExpense", totalSummary.getActualExpenseAmount());
                totalData.put("baseExpense", totalSummary.getBaseExpenseAmount());
                totalData.put("specialExpense", totalSummary.getSpecialExpenseAmount());
                totalData.put("assetAdjustment", totalSummary.getAssetAdjustment());
                totalData.put("liabilityAdjustment", totalSummary.getLiabilityAdjustment());
                totalData.put("adjustmentDetails", totalSummary.getAdjustmentDetails());

                if (previousTotalSummary != null &&
                    previousTotalSummary.getActualExpenseAmount() != null &&
                    previousTotalSummary.getActualExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {

                    BigDecimal actualChange = totalSummary.getActualExpenseAmount()
                            .subtract(previousTotalSummary.getActualExpenseAmount());
                    BigDecimal actualChangePct = actualChange
                            .divide(previousTotalSummary.getActualExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal("100"));

                    totalData.put("actualChangePct", actualChangePct);

                    // 基础支出同比：检查除数是否为零
                    if (previousTotalSummary.getBaseExpenseAmount() != null &&
                        previousTotalSummary.getBaseExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal baseChange = totalSummary.getBaseExpenseAmount()
                                .subtract(previousTotalSummary.getBaseExpenseAmount());
                        BigDecimal baseChangePct = baseChange
                                .divide(previousTotalSummary.getBaseExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal("100"));
                        totalData.put("baseChangePct", baseChangePct);
                    } else {
                        totalData.put("baseChangePct", null);
                    }
                } else {
                    totalData.put("actualChangePct", null);
                    totalData.put("baseChangePct", null);
                }
            } else {
                totalData.put("actualExpense", BigDecimal.ZERO);
                totalData.put("baseExpense", BigDecimal.ZERO);
                totalData.put("specialExpense", BigDecimal.ZERO);
                totalData.put("assetAdjustment", BigDecimal.ZERO);
                totalData.put("liabilityAdjustment", BigDecimal.ZERO);
                totalData.put("adjustmentDetails", null);
                totalData.put("actualChangePct", null);
                totalData.put("baseChangePct", null);
            }

            row.put("total", totalData);
            rows.add(row);
        }

        // 6. 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("years", years);
        result.put("categories", categories);
        result.put("rows", rows);
        result.put("currency", currency);

        return result;
    }
}
