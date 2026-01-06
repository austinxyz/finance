package com.finance.app.service.income;

import com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO;
import com.finance.app.dto.income.IncomeAnnualMinorCategoryDTO;
import com.finance.app.dto.income.IncomeMonthlyTrendDTO;
import com.finance.app.model.ExchangeRate;
import com.finance.app.model.IncomeCategoryMajor;
import com.finance.app.model.IncomeCategoryMinor;
import com.finance.app.model.IncomeRecord;
import com.finance.app.repository.ExchangeRateRepository;
import com.finance.app.repository.IncomeCategoryMajorRepository;
import com.finance.app.repository.IncomeCategoryMinorRepository;
import com.finance.app.repository.IncomeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncomeAnalysisService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private IncomeCategoryMajorRepository majorCategoryRepository;

    @Autowired
    private IncomeCategoryMinorRepository minorCategoryRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * 获取年度大类汇总
     */
    public List<IncomeAnnualMajorCategoryDTO> getAnnualMajorCategorySummary(Long familyId, Integer year, String currency) {
        // 1. 构建起止期间
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";

        // 2. 查询所有记录
        List<IncomeRecord> records = incomeRecordRepository.findByFamilyIdAndPeriodBetween(
                familyId, startPeriod, endPeriod);

        // 3. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 4. 按大类分组并汇总
        Map<Long, BigDecimal> majorCategoryTotals = new HashMap<>();
        Map<String, BigDecimal> finalRateMap = rateMap;  // 用于lambda

        for (IncomeRecord record : records) {
            // 过滤货币（如果不是"All"模式）
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                continue;
            }

            Long majorId = record.getMajorCategoryId();
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
        List<IncomeAnnualMajorCategoryDTO> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : majorCategoryTotals.entrySet()) {
            IncomeCategoryMajor major = majorCategoryRepository.findById(entry.getKey())
                    .orElse(null);
            if (major != null) {
                result.add(new IncomeAnnualMajorCategoryDTO(
                        major.getId(),
                        major.getName(),
                        major.getChineseName(),
                        major.getIcon(),
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
    public List<IncomeAnnualMinorCategoryDTO> getAnnualMinorCategorySummary(Long familyId, Integer year,
                                                                            Long majorCategoryId, String currency) {
        // 1. 构建起止期间
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";

        // 2. 查询所有记录
        List<IncomeRecord> records = incomeRecordRepository.findByFamilyIdAndPeriodBetween(
                familyId, startPeriod, endPeriod);

        // 3. 过滤出该大类下的记录
        records = records.stream()
                .filter(r -> r.getMajorCategoryId().equals(majorCategoryId))
                .collect(Collectors.toList());

        // 4. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 5. 按小类分组并汇总
        Map<Long, BigDecimal> minorCategoryTotals = new HashMap<>();
        Map<String, BigDecimal> finalRateMap = rateMap;

        for (IncomeRecord record : records) {
            // 过滤货币
            if (!"All".equalsIgnoreCase(currency) && !currency.equals(record.getCurrency())) {
                continue;
            }

            // 收入小类可能为null，用0表示无小类
            Long minorId = record.getMinorCategoryId() != null ? record.getMinorCategoryId() : 0L;
            BigDecimal amount;

            if ("All".equalsIgnoreCase(currency)) {
                amount = convertToUSD(record.getAmount(), record.getCurrency(), finalRateMap);
            } else {
                amount = record.getAmount();
            }

            minorCategoryTotals.merge(minorId, amount, BigDecimal::add);
        }

        // 6. 构建DTO列表
        List<IncomeAnnualMinorCategoryDTO> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : minorCategoryTotals.entrySet()) {
            Long minorId = entry.getKey();

            if (minorId == 0L) {
                // 无小类的记录
                IncomeCategoryMajor major = majorCategoryRepository.findById(majorCategoryId).orElse(null);
                if (major != null) {
                    result.add(new IncomeAnnualMinorCategoryDTO(
                            null,
                            "(无子分类)",
                            null,
                            major.getId(),
                            major.getName(),
                            major.getChineseName(),
                            major.getIcon(),
                            entry.getValue(),
                            "All".equalsIgnoreCase(currency) ? "USD" : currency
                    ));
                }
            } else {
                IncomeCategoryMinor minor = minorCategoryRepository.findById(minorId).orElse(null);
                if (minor != null) {
                    IncomeCategoryMajor major = majorCategoryRepository.findById(majorCategoryId).orElse(null);
                    result.add(new IncomeAnnualMinorCategoryDTO(
                            minor.getId(),
                            minor.getName(),
                            minor.getChineseName(),
                            major != null ? major.getId() : null,
                            major != null ? major.getName() : null,
                            major != null ? major.getChineseName() : null,
                            major != null ? major.getIcon() : null,
                            entry.getValue(),
                            "All".equalsIgnoreCase(currency) ? "USD" : currency
                    ));
                }
            }
        }

        // 7. 按金额降序排序
        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        return result;
    }

    /**
     * 获取月度趋势（某个小类的12个月数据，或无小类的大类数据）
     */
    public List<IncomeMonthlyTrendDTO> getAnnualMonthlyTrend(Long familyId, Integer year,
                                                              Long majorCategoryId,
                                                              Long minorCategoryId,
                                                              String currency) {
        // 1. 加载汇率（如果currency="All"）
        Map<String, BigDecimal> rateMap = new HashMap<>();
        if ("All".equalsIgnoreCase(currency)) {
            rateMap = loadExchangeRates(year);
        }

        // 2. 构建12个月的结果
        List<IncomeMonthlyTrendDTO> result = new ArrayList<>();
        Map<String, BigDecimal> finalRateMap = rateMap;

        for (int month = 1; month <= 12; month++) {
            String period = String.format("%d-%02d", year, month);

            // 查询该月该小类的所有记录
            List<IncomeRecord> records;
            if (minorCategoryId != null && minorCategoryId > 0) {
                // 有小类：查询该小类
                records = incomeRecordRepository.findByFamilyIdAndPeriod(familyId, period).stream()
                        .filter(r -> minorCategoryId.equals(r.getMinorCategoryId()))
                        .collect(Collectors.toList());
            } else {
                // 无小类：查询该大类下所有无小类的记录
                records = incomeRecordRepository.findByFamilyIdAndPeriod(familyId, period).stream()
                        .filter(r -> majorCategoryId.equals(r.getMajorCategoryId()) && r.getMinorCategoryId() == null)
                        .collect(Collectors.toList());
            }

            BigDecimal monthTotal = BigDecimal.ZERO;

            for (IncomeRecord record : records) {
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

            result.add(new IncomeMonthlyTrendDTO(
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
}
