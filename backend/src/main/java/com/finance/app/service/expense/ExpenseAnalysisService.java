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

        // 添加默认汇率（以防某些货币查不到）
        if (!rateMap.containsKey("CNY")) {
            rateMap.put("CNY", new BigDecimal("0.1414"));
        }

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
            // 默认汇率
            if ("CNY".equals(currency)) {
                rate = new BigDecimal("0.1414");
            } else {
                rate = BigDecimal.ONE;
            }
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

        List<AnnualExpenseSummary> summaries = annualExpenseSummaryRepository
                .findMajorCategorySummary(familyId, year, currency);

        List<AnnualExpenseSummaryDTO> result = new ArrayList<>();

        for (AnnualExpenseSummary summary : summaries) {
            // 获取大类信息
            ExpenseCategoryMajor major = summary.getMajorCategory();

            AnnualExpenseSummaryDTO dto = new AnnualExpenseSummaryDTO();
            dto.setSummaryYear(summary.getSummaryYear());
            dto.setMajorCategoryId(summary.getMajorCategoryId());
            dto.setMajorCategoryName(major != null ? major.getName() : null);
            dto.setMajorCategoryIcon(major != null ? major.getIcon() : null);
            dto.setMajorCategoryCode(major != null ? major.getCode() : null);
            dto.setMinorCategoryId(null); // 大类汇总没有小类ID
            dto.setMinorCategoryName(null);

            dto.setBaseExpenseAmount(summary.getBaseExpenseAmount());
            dto.setAssetAdjustment(summary.getAssetAdjustment());
            dto.setLiabilityAdjustment(summary.getLiabilityAdjustment());
            dto.setActualExpenseAmount(summary.getActualExpenseAmount());
            dto.setCurrency(summary.getCurrency());
            dto.setAdjustmentDetails(summary.getAdjustmentDetails());

            result.add(dto);
        }

        // 如果需要总计,添加总计记录
        if (includeTotals) {
            AnnualExpenseSummary totalSummary = annualExpenseSummaryRepository
                    .findTotalSummary(familyId, year, currency);

            if (totalSummary != null) {
                AnnualExpenseSummaryDTO totalDTO = new AnnualExpenseSummaryDTO();
                totalDTO.setSummaryYear(totalSummary.getSummaryYear());
                totalDTO.setMajorCategoryId(0L);
                totalDTO.setMajorCategoryName("总计");
                totalDTO.setMajorCategoryIcon(null);
                totalDTO.setMajorCategoryCode("TOTAL");
                totalDTO.setBaseExpenseAmount(totalSummary.getBaseExpenseAmount());
                totalDTO.setAssetAdjustment(totalSummary.getAssetAdjustment());
                totalDTO.setLiabilityAdjustment(totalSummary.getLiabilityAdjustment());
                totalDTO.setActualExpenseAmount(totalSummary.getActualExpenseAmount());
                totalDTO.setCurrency(totalSummary.getCurrency());
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
            // 调用存储过程 calculate_annual_expense_summary_v2
            entityManager.createNativeQuery("CALL calculate_annual_expense_summary_v2(:familyId, :year)")
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
}
