package com.finance.app.service;

import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.FinancialMetricsDTO;
import com.finance.app.dto.OptimizationRecommendationDTO;
import com.finance.app.dto.OverallTrendDataPointDTO;
import com.finance.app.dto.RiskAssessmentDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.User;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetCategoryRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityRecordRepository;
import com.finance.app.repository.NetAssetCategoryRepository;
import com.finance.app.repository.NetAssetCategoryAssetTypeMappingRepository;
import com.finance.app.repository.NetAssetCategoryLiabilityTypeMappingRepository;
import com.finance.app.repository.UserProfileRepository;
import com.finance.app.repository.UserRepository;
import com.finance.app.model.NetAssetCategory;
import com.finance.app.model.NetAssetCategoryAssetTypeMapping;
import com.finance.app.model.NetAssetCategoryLiabilityTypeMapping;
import com.finance.app.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AssetAccountRepository accountRepository;
    private final AssetRecordRepository recordRepository;
    private final AssetCategoryRepository categoryRepository;
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;
    private final NetAssetCategoryRepository netAssetCategoryRepository;
    private final NetAssetCategoryAssetTypeMappingRepository assetTypeMappingRepository;
    private final NetAssetCategoryLiabilityTypeMappingRepository liabilityTypeMappingRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;

    // 获取资产总览
    public AssetSummaryDTO getAssetSummary(Long userId) {
        return getAssetSummary(userId, null);
    }

    // 获取指定日期的资产总览
    public AssetSummaryDTO getAssetSummary(Long userId, LocalDate asOfDate) {
        return getAssetSummary(userId, asOfDate, false);
    }

    // 获取指定日期的资产总览（可选择是否包含自住房）
    public AssetSummaryDTO getAssetSummary(Long userId, LocalDate asOfDate, boolean includePrimaryResidence) {
        // 如果userId为null，获取所有活跃账户；否则只获取该用户的账户
        List<AssetAccount> accounts;
        if (userId == null) {
            accounts = accountRepository.findByIsActiveTrue();
        } else {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        BigDecimal totalAssets = BigDecimal.ZERO;
        Map<String, BigDecimal> assetsByCategory = new HashMap<>();
        Map<String, BigDecimal> assetsByType = new HashMap<>();
        LocalDate actualDate = null;  // 追踪实际使用的数据日期（最新记录的日期）

        for (AssetAccount account : accounts) {
            // 如果不包含自住房，跳过标记为自住房的账户
            if (!includePrimaryResidence && Boolean.TRUE.equals(account.getIsPrimaryResidence())) {
                continue;
            }

            // 根据asOfDate获取记录
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用查询日期的汇率重新计算金额，而不是使用记录时的amountInBaseCurrency
                AssetRecord assetRecord = record.get();
                BigDecimal amount = convertToUSD(
                    assetRecord.getAmount(),
                    assetRecord.getCurrency(),
                    asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                );
                totalAssets = totalAssets.add(amount);

                // 追踪实际使用的最新数据日期
                LocalDate recordDate = record.get().getRecordDate();
                if (actualDate == null || recordDate.isAfter(actualDate)) {
                    actualDate = recordDate;
                }

                // 按分类汇总
                String categoryName = account.getCategory() != null ?
                    account.getCategory().getName() : "未分类";
                assetsByCategory.merge(categoryName, amount, BigDecimal::add);

                // 按类型汇总
                String typeName = account.getCategory() != null ?
                    account.getCategory().getType() : "OTHER";
                assetsByType.merge(typeName, amount, BigDecimal::add);
            }
        }

        // 计算总负债
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用查询日期的汇率重新计算金额，而不是使用记录时的balanceInBaseCurrency
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal balance = convertToUSD(
                    liabilityRecord.getOutstandingBalance(),
                    liabilityRecord.getCurrency(),
                    asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                );
                totalLiabilities = totalLiabilities.add(balance);

                // 追踪实际使用的最新数据日期
                LocalDate recordDate = liabilityRecord.getRecordDate();
                if (actualDate == null || recordDate.isAfter(actualDate)) {
                    actualDate = recordDate;
                }
            }
        }

        AssetSummaryDTO summary = new AssetSummaryDTO();
        summary.setTotalAssets(totalAssets);
        summary.setTotalLiabilities(totalLiabilities);
        summary.setNetWorth(totalAssets.subtract(totalLiabilities));
        summary.setAssetsByCategory(assetsByCategory);
        summary.setAssetsByType(assetsByType);
        summary.setActualDate(actualDate);  // 设置实际使用的数据日期

        return summary;
    }

    // 获取指定日期或之前最近的资产记录
    private Optional<AssetRecord> getAssetRecordAsOfDate(Long accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            // 如果没有指定日期，获取最新记录
            return recordRepository.findLatestByAccountId(accountId);
        } else {
            // 获取指定日期或之前最近的记录
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBeforeOrEqual(accountId, asOfDate);
            return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        }
    }

    // 获取指定日期或之前最近的负债记录
    private Optional<LiabilityRecord> getLiabilityRecordAsOfDate(Long accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            // 如果没有指定日期，获取最新记录
            return liabilityRecordRepository.findLatestByAccountId(accountId);
        } else {
            // 获取指定日期或之前最近的记录
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBeforeOrEqual(accountId, asOfDate);
            return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        }
    }

    /**
     * 根据指定日期的汇率将金额转换为基准货币（USD）
     * @param amount 原始金额
     * @param currency 原始货币
     * @param asOfDate 查询日期（使用该日期的汇率）
     * @param baseCurrency 基准货币（默认USD）
     * @return 转换后的金额
     */
    private BigDecimal convertToBaseCurrency(BigDecimal amount, String currency, LocalDate asOfDate, String baseCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        // 如果没有指定日期，使用当前日期
        LocalDate conversionDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        // 如果货币相同，直接返回
        if (currency == null || currency.equalsIgnoreCase(baseCurrency)) {
            return amount;
        }

        // 如果基准货币是USD
        if (baseCurrency == null || baseCurrency.equalsIgnoreCase("USD")) {
            // 获取原始货币到USD的汇率
            BigDecimal rateToUsd = exchangeRateService.getExchangeRate(currency, conversionDate);
            return amount.multiply(rateToUsd).setScale(2, RoundingMode.HALF_UP);
        }

        // 如果基准货币不是USD，需要两步转换
        // 1. 原始货币 -> USD
        BigDecimal rateToUsd = exchangeRateService.getExchangeRate(currency, conversionDate);
        BigDecimal amountInUsd = amount.multiply(rateToUsd);

        // 2. USD -> 基准货币
        BigDecimal baseRateToUsd = exchangeRateService.getExchangeRate(baseCurrency, conversionDate);
        return amountInUsd.divide(baseRateToUsd, 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据指定日期的汇率将金额转换为USD（简化版本）
     */
    private BigDecimal convertToUSD(BigDecimal amount, String currency, LocalDate asOfDate) {
        return convertToBaseCurrency(amount, currency, asOfDate, "USD");
    }

    // 获取总资产趋势
    public List<TrendDataDTO> getTotalAssetTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        final LocalDate finalStartDate = (startDate == null) ? LocalDate.now().minusMonths(12) : startDate;
        final LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        // 如果userId为null，获取所有活跃账户；否则只获取该用户的账户
        List<AssetAccount> accounts;
        if (userId == null) {
            accounts = accountRepository.findByIsActiveTrue();
        } else {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }
        List<Long> accountIds = accounts.stream().map(AssetAccount::getId).collect(Collectors.toList());

        if (accountIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有记录
        List<AssetRecord> allRecords = new ArrayList<>();
        for (Long accountId : accountIds) {
            List<AssetRecord> records = recordRepository.findByAccountIdOrderByRecordDateDesc(accountId);
            allRecords.addAll(records);
        }

        // 按日期分组并汇总
        Map<LocalDate, BigDecimal> dailyTotals = allRecords.stream()
            .filter(r -> !r.getRecordDate().isBefore(finalStartDate) && !r.getRecordDate().isAfter(finalEndDate))
            .collect(Collectors.groupingBy(
                AssetRecord::getRecordDate,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    AssetRecord::getAmountInBaseCurrency,
                    BigDecimal::add
                )
            ));

        // 转换为DTO并排序
        return dailyTotals.entrySet().stream()
            .map(entry -> new TrendDataDTO(entry.getKey(), entry.getValue(), null, null))
            .sorted(Comparator.comparing(TrendDataDTO::getDate))
            .collect(Collectors.toList());
    }

    // 获取单个账户趋势
    public List<TrendDataDTO> getAccountTrend(Long accountId) {
        List<AssetRecord> records = recordRepository.findByAccountIdOrderByRecordDateDesc(accountId);
        AssetAccount account = accountRepository.findById(accountId).orElse(null);

        return records.stream()
            .map(record -> {
                TrendDataDTO dto = new TrendDataDTO();
                dto.setDate(record.getRecordDate());
                // 使用记录日期的汇率重新计算金额
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                dto.setAmount(amount);
                if (account != null) {
                    dto.setAccountName(account.getAccountName());
                    if (account.getCategory() != null) {
                        dto.setCategoryName(account.getCategory().getName());
                    }
                }
                return dto;
            })
            .sorted(Comparator.comparing(TrendDataDTO::getDate))
            .collect(Collectors.toList());
    }

    // 获取按分类的资产配置
    public Map<String, Object> getAssetAllocationByCategory(Long userId) {
        AssetSummaryDTO summary = getAssetSummary(userId);
        Map<String, BigDecimal> assetsByCategory = summary.getAssetsByCategory();

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal total = summary.getTotalAssets();

        for (Map.Entry<String, BigDecimal> entry : assetsByCategory.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());

            // 计算百分比
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = entry.getValue()
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }

            data.add(item);
        }

        // 按金额降序排序
        data.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("data", data);

        return result;
    }

    // 获取按类型的资产配置
    public Map<String, Object> getAssetAllocationByType(Long userId) {
        return getAssetAllocationByType(userId, null);
    }

    // 获取指定日期的按类型的资产配置
    public Map<String, Object> getAssetAllocationByType(Long userId, LocalDate asOfDate) {
        AssetSummaryDTO summary = getAssetSummary(userId, asOfDate);
        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal total = summary.getTotalAssets();

        // 类型中文名映射
        Map<String, String> typeNames = Map.of(
            "CASH", "现金类",
            "STOCKS", "股票投资",
            "RETIREMENT_FUND", "退休基金",
            "INSURANCE", "保险",
            "REAL_ESTATE", "房地产",
            "CRYPTOCURRENCY", "数字货币",
            "PRECIOUS_METALS", "贵金属",
            "OTHER", "其他"
        );

        for (Map.Entry<String, BigDecimal> entry : assetsByType.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", typeNames.getOrDefault(entry.getKey(), entry.getKey()));
            item.put("value", entry.getValue());

            // 计算百分比
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = entry.getValue()
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }

            data.add(item);
        }

        // 按金额降序排序
        data.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("data", data);

        return result;
    }

    // 获取净资产配置（资产减去对应负债）
    public Map<String, Object> getNetAssetAllocation(Long userId) {
        return getNetAssetAllocation(userId, null);
    }

    // 获取指定日期的净资产配置
    public Map<String, Object> getNetAssetAllocation(Long userId, LocalDate asOfDate) {
        // 获取所有净资产类别
        List<NetAssetCategory> netAssetCategories = netAssetCategoryRepository.findAllByOrderByDisplayOrderAsc();

        // 获取资产和负债数据
        AssetSummaryDTO summary = getAssetSummary(userId, asOfDate);
        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();

        // 计算每个负债类型的总额
        Map<String, BigDecimal> liabilitiesByType = calculateLiabilitiesByType(userId, asOfDate);

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal totalNetAssets = BigDecimal.ZERO;

        for (NetAssetCategory netCategory : netAssetCategories) {
            // 获取该净资产类别对应的资产类型
            List<NetAssetCategoryAssetTypeMapping> assetMappings =
                assetTypeMappingRepository.findByNetAssetCategoryId(netCategory.getId());

            // 获取该净资产类别对应的负债类型
            List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings =
                liabilityTypeMappingRepository.findByNetAssetCategoryId(netCategory.getId());

            // 计算该类别的总资产
            BigDecimal categoryAssets = BigDecimal.ZERO;
            for (NetAssetCategoryAssetTypeMapping mapping : assetMappings) {
                BigDecimal assetAmount = assetsByType.getOrDefault(mapping.getAssetType(), BigDecimal.ZERO);
                categoryAssets = categoryAssets.add(assetAmount);
            }

            // 计算该类别的总负债
            BigDecimal categoryLiabilities = BigDecimal.ZERO;
            for (NetAssetCategoryLiabilityTypeMapping mapping : liabilityMappings) {
                BigDecimal liabilityAmount = liabilitiesByType.getOrDefault(mapping.getLiabilityType(), BigDecimal.ZERO);
                categoryLiabilities = categoryLiabilities.add(liabilityAmount);
            }

            // 计算净资产（资产 - 负债）
            BigDecimal netAsset = categoryAssets.subtract(categoryLiabilities);

            // 只添加净值大于0的类别
            if (netAsset.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", netCategory.getName());
                item.put("code", netCategory.getCode());
                item.put("assets", categoryAssets);
                item.put("liabilities", categoryLiabilities);
                item.put("netValue", netAsset);
                item.put("color", netCategory.getColor());

                data.add(item);
                totalNetAssets = totalNetAssets.add(netAsset);
            }
        }

        // 计算百分比
        for (Map<String, Object> item : data) {
            BigDecimal netValue = (BigDecimal) item.get("netValue");
            if (totalNetAssets.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = netValue
                    .divide(totalNetAssets, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }
        }

        // 按净值降序排序
        data.sort((a, b) -> ((BigDecimal)b.get("netValue")).compareTo((BigDecimal)a.get("netValue")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalNetAssets);
        result.put("data", data);

        return result;
    }

    // 获取按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId) {
        return getLiabilityAllocationByType(userId, null);
    }

    // 获取指定日期的按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId, LocalDate asOfDate) {
        Map<String, BigDecimal> liabilitiesByType = calculateLiabilitiesByType(userId, asOfDate);

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // 类型中文名映射
        Map<String, String> typeNames = Map.ofEntries(
            entry("MORTGAGE", "房贷"),
            entry("AUTO_LOAN", "车贷"),
            entry("CREDIT_CARD", "信用卡"),
            entry("PERSONAL_LOAN", "个人借债"),
            entry("STUDENT_LOAN", "学生贷款"),
            entry("BUSINESS_LOAN", "商业贷款"),
            entry("OTHER", "其他")
        );

        for (Map.Entry<String, BigDecimal> entry : liabilitiesByType.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", typeNames.getOrDefault(entry.getKey(), entry.getKey()));
            item.put("value", entry.getValue());

            total = total.add(entry.getValue());
            data.add(item);
        }

        // 计算百分比
        for (Map<String, Object> item : data) {
            BigDecimal value = (BigDecimal) item.get("value");
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = value
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }
        }

        // 按金额降序排序
        data.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("data", data);

        return result;
    }

    // 计算每个负债类型的总额
    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId) {
        return calculateLiabilitiesByType(userId, null);
    }

    // 计算指定日期的每个负债类型的总额
    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId, LocalDate asOfDate) {
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        Map<String, BigDecimal> liabilitiesByType = new HashMap<>();

        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                BigDecimal balance = record.get().getBalanceInBaseCurrency();

                String typeName = account.getCategory() != null ?
                    account.getCategory().getType() : "OTHER";
                liabilitiesByType.merge(typeName, balance, BigDecimal::add);
            }
        }

        return liabilitiesByType;
    }

    // 获取综合趋势数据（净资产、总资产、总负债）
    public List<OverallTrendDataPointDTO> getOverallTrend(String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            assetAccounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            liabilityAccounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // 获取日期范围内所有资产记录
        Map<LocalDate, BigDecimal> assetsByDate = new HashMap<>();
        for (AssetAccount account : assetAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                assetsByDate.merge(record.getRecordDate(), amount, BigDecimal::add);
            }
        }

        // 获取日期范围内所有负债记录
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : liabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal balance = convertToUSD(
                    record.getOutstandingBalance(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                liabilitiesByDate.merge(record.getRecordDate(), balance, BigDecimal::add);
            }
        }

        // 合并所有日期
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(assetsByDate.keySet());
        allDates.addAll(liabilitiesByDate.keySet());

        // 创建趋势数据点（按日期）
        Map<LocalDate, OverallTrendDataPointDTO> dailyData = new HashMap<>();
        for (LocalDate date : allDates) {
            BigDecimal assets = assetsByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal liabilities = liabilitiesByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal netWorth = assets.subtract(liabilities);

            OverallTrendDataPointDTO point = new OverallTrendDataPointDTO();
            point.setDate(date.toString());
            point.setTotalAssets(assets);
            point.setTotalLiabilities(liabilities);
            point.setNetWorth(netWorth);

            dailyData.put(date, point);
        }

        // 按年度聚合数据 - 每年只保留最后一天的数据
        Map<Integer, OverallTrendDataPointDTO> yearlyData = new HashMap<>();
        for (LocalDate date : allDates) {
            int year = date.getYear();
            OverallTrendDataPointDTO currentPoint = dailyData.get(date);

            // 如果该年还没有数据，或者当前日期比已有的日期更晚，则更新
            if (!yearlyData.containsKey(year) ||
                date.isAfter(LocalDate.parse(yearlyData.get(year).getDate()))) {
                yearlyData.put(year, currentPoint);
            }
        }

        // 将年度数据转换为列表并按年份排序
        List<OverallTrendDataPointDTO> result = new ArrayList<>(yearlyData.values());
        result.sort(Comparator.comparing(OverallTrendDataPointDTO::getDate));

        return result;
    }

    // 获取资产分类趋势数据
    public List<TrendDataPointDTO> getAssetCategoryTrend(String categoryType, String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有资产账户
        List<AssetAccount> accounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            accounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<AssetAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取日期范围内的记录并按日期汇总
        Map<LocalDate, BigDecimal> totalByDate = new HashMap<>();
        for (AssetAccount account : filteredAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                totalByDate.merge(record.getRecordDate(), amount, BigDecimal::add);
            }
        }

        // 转换为DTO列表
        List<TrendDataPointDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : totalByDate.entrySet()) {
            TrendDataPointDTO point = new TrendDataPointDTO();
            point.setDate(entry.getKey().toString());
            point.setTotal(entry.getValue());
            result.add(point);
        }

        // 按日期排序
        result.sort(Comparator.comparing(TrendDataPointDTO::getDate));

        return result;
    }

    // 获取负债分类趋势数据
    public List<TrendDataPointDTO> getLiabilityCategoryTrend(String categoryType, String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有负债账户
        List<LiabilityAccount> accounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            accounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            accounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取日期范围内的记录并按日期汇总
        Map<LocalDate, BigDecimal> totalByDate = new HashMap<>();
        for (LiabilityAccount account : filteredAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal balance = convertToUSD(
                    record.getOutstandingBalance(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                totalByDate.merge(record.getRecordDate(), balance, BigDecimal::add);
            }
        }

        // 转换为DTO列表
        List<TrendDataPointDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : totalByDate.entrySet()) {
            TrendDataPointDTO point = new TrendDataPointDTO();
            point.setDate(entry.getKey().toString());
            point.setTotal(entry.getValue());
            result.add(point);
        }

        // 按日期排序
        result.sort(Comparator.comparing(TrendDataPointDTO::getDate));

        return result;
    }

    // 获取净资产分类趋势数据
    public List<TrendDataPointDTO> getNetAssetCategoryTrend(String categoryCode, String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取净资产分类
        Optional<NetAssetCategory> categoryOpt = netAssetCategoryRepository.findByCode(categoryCode);
        if (categoryOpt.isEmpty()) {
            return new ArrayList<>();
        }
        NetAssetCategory category = categoryOpt.get();

        // 获取该分类关联的资产类型
        List<NetAssetCategoryAssetTypeMapping> assetMappings = assetTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> assetTypes = assetMappings.stream()
            .map(NetAssetCategoryAssetTypeMapping::getAssetType)
            .collect(Collectors.toSet());

        // 获取该分类关联的负债类型
        List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings = liabilityTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> liabilityTypes = liabilityMappings.stream()
            .map(NetAssetCategoryLiabilityTypeMapping::getLiabilityType)
            .collect(Collectors.toSet());

        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            assetAccounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的资产账户
        List<AssetAccount> filteredAssetAccounts = assetAccounts.stream()
            .filter(acc -> acc.getCategory() != null && assetTypes.contains(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            // 获取该家庭所有成员的账户
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            liabilityAccounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的负债账户
        List<LiabilityAccount> filteredLiabilityAccounts = liabilityAccounts.stream()
            .filter(acc -> acc.getCategory() != null && liabilityTypes.contains(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取日期范围内的资产记录并按日期汇总
        Map<LocalDate, BigDecimal> assetsByDate = new HashMap<>();
        for (AssetAccount account : filteredAssetAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                assetsByDate.merge(record.getRecordDate(), amount, BigDecimal::add);
            }
        }

        // 获取日期范围内的负债记录并按日期汇总
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : filteredLiabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal balance = convertToUSD(
                    record.getOutstandingBalance(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                liabilitiesByDate.merge(record.getRecordDate(), balance, BigDecimal::add);
            }
        }

        // 合并所有日期
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(assetsByDate.keySet());
        allDates.addAll(liabilitiesByDate.keySet());

        // 计算每个日期的净资产（资产 - 负债）
        List<TrendDataPointDTO> result = new ArrayList<>();
        for (LocalDate date : allDates) {
            BigDecimal assets = assetsByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal liabilities = liabilitiesByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal netValue = assets.subtract(liabilities);

            TrendDataPointDTO point = new TrendDataPointDTO();
            point.setDate(date.toString());
            point.setTotal(netValue);
            result.add(point);
        }

        // 按日期排序
        result.sort(Comparator.comparing(TrendDataPointDTO::getDate));

        return result;
    }

    // 获取指定类型和日期的资产账户及其余额
    public List<Map<String, Object>> getAssetAccountsWithBalancesByType(String categoryType, Long userId, LocalDate asOfDate) {
        // 获取所有活跃账户
        List<AssetAccount> accounts;
        if (userId == null) {
            accounts = accountRepository.findByIsActiveTrue();
        } else {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 过滤出匹配类型的账户
        List<AssetAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取每个账户在指定日期的余额
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetAccount account : filteredAccounts) {
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("accountName", account.getAccountName());
                accountData.put("balance", record.get().getAmountInBaseCurrency());
                result.add(accountData);
            }
        }

        return result;
    }

    // 获取指定类型和日期的负债账户及其余额
    public List<Map<String, Object>> getLiabilityAccountsWithBalancesByType(String categoryType, Long userId, LocalDate asOfDate) {
        // 获取所有活跃账户
        List<LiabilityAccount> accounts;
        if (userId == null) {
            accounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            accounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 过滤出匹配类型的账户
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取每个账户在指定日期的余额
        List<Map<String, Object>> result = new ArrayList<>();
        for (LiabilityAccount account : filteredAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("accountName", account.getAccountName());
                accountData.put("balance", record.get().getBalanceInBaseCurrency());
                result.add(accountData);
            }
        }

        return result;
    }

    // 获取资产分类下所有账户的趋势数据
    public Map<String, List<AccountTrendDataPointDTO>> getAssetAccountsTrendByCategory(
            String categoryType, String startDateStr, String endDateStr, Long userId, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有资产账户
        List<AssetAccount> accounts;
        if (familyId != null) {
            // 如果提供了家庭ID，获取该家庭所有成员的账户
            List<User> familyUsers = userRepository.findByFamilyId(familyId);
            List<Long> userIds = familyUsers.stream().map(User::getId).collect(Collectors.toList());
            accounts = new ArrayList<>();
            for (Long uid : userIds) {
                accounts.addAll(accountRepository.findByUserIdAndIsActiveTrue(uid));
            }
        } else if (userId != null) {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<AssetAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 为每个账户获取趋势数据
        Map<String, List<AccountTrendDataPointDTO>> result = new HashMap<>();

        for (AssetAccount account : filteredAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);

            if (!records.isEmpty()) {
                List<AccountTrendDataPointDTO> accountTrend = new ArrayList<>();
                for (AssetRecord record : records) {
                    AccountTrendDataPointDTO point = new AccountTrendDataPointDTO();
                    point.setDate(record.getRecordDate().toString());
                    // 使用记录日期的汇率重新计算金额
                    BigDecimal balance = convertToUSD(
                        record.getAmount(),
                        record.getCurrency(),
                        record.getRecordDate()
                    );
                    point.setBalance(balance);
                    point.setAccountName(account.getAccountName());
                    accountTrend.add(point);
                }

                // 按日期排序（升序）
                accountTrend.sort(Comparator.comparing(AccountTrendDataPointDTO::getDate));

                // 使用账户ID作为key
                result.put(account.getId().toString(), accountTrend);
            }
        }

        return result;
    }

    // 获取负债分类下所有账户的趋势数据
    public Map<String, List<AccountTrendDataPointDTO>> getLiabilityAccountsTrendByCategory(
            String categoryType, String startDateStr, String endDateStr, Long userId, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有负债账户
        List<LiabilityAccount> accounts;
        if (familyId != null) {
            // 如果提供了家庭ID,获取该家庭所有成员的账户
            List<User> familyUsers = userRepository.findByFamilyId(familyId);
            List<Long> userIds = familyUsers.stream().map(User::getId).collect(Collectors.toList());
            accounts = new ArrayList<>();
            for (Long uid : userIds) {
                accounts.addAll(liabilityAccountRepository.findByUserIdAndIsActiveTrue(uid));
            }
        } else if (userId != null) {
            accounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getCategory() != null && categoryType.equals(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 为每个账户获取趋势数据
        Map<String, List<AccountTrendDataPointDTO>> result = new HashMap<>();

        for (LiabilityAccount account : filteredAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);

            if (!records.isEmpty()) {
                List<AccountTrendDataPointDTO> accountTrend = new ArrayList<>();
                for (LiabilityRecord record : records) {
                    AccountTrendDataPointDTO point = new AccountTrendDataPointDTO();
                    point.setDate(record.getRecordDate().toString());
                    // 使用记录日期的汇率重新计算金额
                    BigDecimal balance = convertToUSD(
                        record.getOutstandingBalance(),
                        record.getCurrency(),
                        record.getRecordDate()
                    );
                    point.setBalance(balance);
                    point.setAccountName(account.getAccountName());
                    accountTrend.add(point);
                }

                // 按日期排序（升序）
                accountTrend.sort(Comparator.comparing(AccountTrendDataPointDTO::getDate));

                // 使用账户ID作为key
                result.put(account.getId().toString(), accountTrend);
            }
        }

        return result;
    }

    // 获取净资产类别下的所有账户详情（包含资产账户和负债账户）
    public Map<String, Object> getNetAssetCategoryAccounts(String categoryCode, Long userId, LocalDate asOfDate) {
        // 获取净资产分类
        Optional<NetAssetCategory> categoryOpt = netAssetCategoryRepository.findByCode(categoryCode);
        if (categoryOpt.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("assetAccounts", new ArrayList<>());
            emptyResult.put("liabilityAccounts", new ArrayList<>());
            return emptyResult;
        }
        NetAssetCategory category = categoryOpt.get();

        // 获取该分类关联的资产类型
        List<NetAssetCategoryAssetTypeMapping> assetMappings = assetTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> assetTypes = assetMappings.stream()
            .map(NetAssetCategoryAssetTypeMapping::getAssetType)
            .collect(Collectors.toSet());

        // 获取该分类关联的负债类型
        List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings = liabilityTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> liabilityTypes = liabilityMappings.stream()
            .map(NetAssetCategoryLiabilityTypeMapping::getLiabilityType)
            .collect(Collectors.toSet());

        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (userId == null) {
            assetAccounts = accountRepository.findByIsActiveTrue();
        } else {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 过滤出匹配类型的资产账户并获取余额
        List<Map<String, Object>> assetAccountsData = new ArrayList<>();
        for (AssetAccount account : assetAccounts) {
            if (account.getCategory() != null && assetTypes.contains(account.getCategory().getType())) {
                Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("accountId", account.getId());
                    accountData.put("accountName", account.getAccountName());
                    accountData.put("categoryType", account.getCategory().getType());
                    accountData.put("categoryName", account.getCategory().getName());
                    accountData.put("balance", record.get().getAmountInBaseCurrency());
                    assetAccountsData.add(accountData);
                }
            }
        }

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 过滤出匹配类型的负债账户并获取余额
        List<Map<String, Object>> liabilityAccountsData = new ArrayList<>();
        for (LiabilityAccount account : liabilityAccounts) {
            if (account.getCategory() != null && liabilityTypes.contains(account.getCategory().getType())) {
                Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("accountId", account.getId());
                    accountData.put("accountName", account.getAccountName());
                    accountData.put("categoryType", account.getCategory().getType());
                    accountData.put("categoryName", account.getCategory().getName());
                    accountData.put("balance", record.get().getBalanceInBaseCurrency());
                    liabilityAccountsData.add(accountData);
                }
            }
        }

        // 按余额降序排序
        assetAccountsData.sort((a, b) -> ((BigDecimal)b.get("balance")).compareTo((BigDecimal)a.get("balance")));
        liabilityAccountsData.sort((a, b) -> ((BigDecimal)b.get("balance")).compareTo((BigDecimal)a.get("balance")));

        Map<String, Object> result = new HashMap<>();
        result.put("assetAccounts", assetAccountsData);
        result.put("liabilityAccounts", liabilityAccountsData);
        result.put("categoryName", category.getName());
        result.put("categoryCode", categoryCode);

        return result;
    }

    // 获取按税收状态的净资产配置
    public Map<String, Object> getNetWorthByTaxStatus(Long userId, LocalDate asOfDate) {
        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (userId == null) {
            assetAccounts = accountRepository.findByIsActiveTrue();
        } else {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 按税收状态分组资产
        Map<String, BigDecimal> assetsByTaxStatus = new HashMap<>();
        assetsByTaxStatus.put("TAXABLE", BigDecimal.ZERO);
        assetsByTaxStatus.put("TAX_FREE", BigDecimal.ZERO);
        assetsByTaxStatus.put("TAX_DEFERRED", BigDecimal.ZERO);

        for (AssetAccount account : assetAccounts) {
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用查询日期的汇率重新计算金额
                AssetRecord assetRecord = record.get();
                BigDecimal amount = convertToUSD(
                    assetRecord.getAmount(),
                    assetRecord.getCurrency(),
                    asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                );
                String taxStatus = account.getTaxStatus() != null ? account.getTaxStatus().name() : "TAXABLE";
                assetsByTaxStatus.merge(taxStatus, amount, BigDecimal::add);
            }
        }

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用查询日期的汇率重新计算金额
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal balance = convertToUSD(
                    liabilityRecord.getOutstandingBalance(),
                    liabilityRecord.getCurrency(),
                    asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                );
                totalLiabilities = totalLiabilities.add(balance);
            }
        }

        // 负债优先抵扣应税资产，然后抵扣免税资产，最后抵扣延税资产
        BigDecimal remainingLiabilities = totalLiabilities;

        // 1. 先抵扣应税资产
        BigDecimal taxableAssets = assetsByTaxStatus.get("TAXABLE");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxableAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxableAssets.compareTo(remainingLiabilities) >= 0) {
                // 应税资产足够抵扣所有负债
                assetsByTaxStatus.put("TAXABLE", taxableAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                // 应税资产不够，全部用于抵扣
                remainingLiabilities = remainingLiabilities.subtract(taxableAssets);
                assetsByTaxStatus.put("TAXABLE", BigDecimal.ZERO);
            }
        }

        // 2. 再抵扣免税资产
        BigDecimal taxFreeAssets = assetsByTaxStatus.get("TAX_FREE");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxFreeAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxFreeAssets.compareTo(remainingLiabilities) >= 0) {
                // 免税资产足够抵扣剩余负债
                assetsByTaxStatus.put("TAX_FREE", taxFreeAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                // 免税资产不够，全部用于抵扣
                remainingLiabilities = remainingLiabilities.subtract(taxFreeAssets);
                assetsByTaxStatus.put("TAX_FREE", BigDecimal.ZERO);
            }
        }

        // 3. 最后抵扣延税资产
        BigDecimal taxDeferredAssets = assetsByTaxStatus.get("TAX_DEFERRED");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxDeferredAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxDeferredAssets.compareTo(remainingLiabilities) >= 0) {
                // 延税资产足够抵扣剩余负债
                assetsByTaxStatus.put("TAX_DEFERRED", taxDeferredAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                // 延税资产不够，全部用于抵扣
                remainingLiabilities = remainingLiabilities.subtract(taxDeferredAssets);
                assetsByTaxStatus.put("TAX_DEFERRED", BigDecimal.ZERO);
            }
        }

        // 计算净资产总额
        BigDecimal totalNetWorth = BigDecimal.ZERO;
        for (BigDecimal value : assetsByTaxStatus.values()) {
            totalNetWorth = totalNetWorth.add(value);
        }

        // 税收状态中文名映射
        Map<String, String> taxStatusNames = Map.of(
            "TAXABLE", "应税",
            "TAX_FREE", "免税",
            "TAX_DEFERRED", "延税"
        );

        // 构建返回数据
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : assetsByTaxStatus.entrySet()) {
            // 只添加净值大于0的类别
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("taxStatus", entry.getKey());
                item.put("name", taxStatusNames.getOrDefault(entry.getKey(), entry.getKey()));
                item.put("value", entry.getValue());

                // 计算百分比
                if (totalNetWorth.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = entry.getValue()
                        .divide(totalNetWorth, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    item.put("percentage", percentage);
                } else {
                    item.put("percentage", BigDecimal.ZERO);
                }

                data.add(item);
            }
        }

        // 按金额降序排序
        data.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalNetWorth);
        result.put("data", data);

        return result;
    }

    // 获取按家庭成员的净资产配置
    public Map<String, Object> getNetWorthByMember(Long familyId, LocalDate asOfDate) {
        List<User> users;
        if (familyId != null) {
            // 获取指定家庭的所有活跃成员
            users = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else {
            // 如果没有指定家庭，获取所有活跃用户
            users = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .collect(Collectors.toList());
        }

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> memberData = new ArrayList<>();
        BigDecimal totalNetWorth = BigDecimal.ZERO;

        // 计算每个成员的净资产
        for (User user : users) {
            // 获取该用户的所有资产账户
            List<AssetAccount> assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(user.getId());
            BigDecimal userTotalAssets = BigDecimal.ZERO;

            for (AssetAccount account : assetAccounts) {
                Optional<AssetRecord> recordOpt = getAssetRecordAsOfDate(account.getId(), asOfDate);
                if (recordOpt.isPresent()) {
                    // 使用查询日期的汇率重新计算金额
                    AssetRecord assetRecord = recordOpt.get();
                    BigDecimal value = convertToUSD(
                        assetRecord.getAmount(),
                        assetRecord.getCurrency(),
                        asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                    );
                    userTotalAssets = userTotalAssets.add(value);
                }
            }

            // 获取该用户的所有负债账户
            List<LiabilityAccount> liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(user.getId());
            BigDecimal userTotalLiabilities = BigDecimal.ZERO;

            for (LiabilityAccount account : liabilityAccounts) {
                Optional<LiabilityRecord> recordOpt = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
                if (recordOpt.isPresent()) {
                    // 使用查询日期的汇率重新计算金额
                    LiabilityRecord liabilityRecord = recordOpt.get();
                    BigDecimal value = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                    userTotalLiabilities = userTotalLiabilities.add(value);
                }
            }

            // 计算该用户的净资产
            BigDecimal userNetWorth = userTotalAssets.subtract(userTotalLiabilities);

            // 只添加净资产不为零的成员
            if (userNetWorth.compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", user.getId());
                memberInfo.put("userName", user.getUsername());
                memberInfo.put("displayName", user.getFullName() != null ? user.getFullName() : user.getUsername());
                memberInfo.put("value", userNetWorth);
                memberInfo.put("assets", userTotalAssets);
                memberInfo.put("liabilities", userTotalLiabilities);

                memberData.add(memberInfo);
                totalNetWorth = totalNetWorth.add(userNetWorth);
            }
        }

        // 计算百分比
        for (Map<String, Object> member : memberData) {
            BigDecimal value = (BigDecimal) member.get("value");
            BigDecimal percentage = totalNetWorth.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : value.divide(totalNetWorth, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            member.put("percentage", percentage);
        }

        // 按净资产值降序排序
        memberData.sort((a, b) -> {
            BigDecimal valueA = (BigDecimal) a.get("value");
            BigDecimal valueB = (BigDecimal) b.get("value");
            return valueB.compareTo(valueA);
        });

        result.put("total", totalNetWorth);
        result.put("data", memberData);
        result.put("currency", "USD");
        result.put("asOfDate", asOfDate.toString());

        return result;
    }

    // 获取财务指标
    public FinancialMetricsDTO getFinancialMetrics(Long userId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        FinancialMetricsDTO metrics = new FinancialMetricsDTO();
        metrics.setAsOfDate(targetDate);

        // 1. 获取当前日期的基础指标
        AssetSummaryDTO currentSummary = getAssetSummary(userId, targetDate);
        metrics.setTotalAssets(currentSummary.getTotalAssets());
        metrics.setTotalLiabilities(currentSummary.getTotalLiabilities());
        metrics.setNetWorth(currentSummary.getNetWorth());

        // 2. 计算资产负债率
        if (currentSummary.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = currentSummary.getTotalLiabilities()
                .divide(currentSummary.getTotalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setDebtToAssetRatio(debtRatio);
        } else {
            metrics.setDebtToAssetRatio(BigDecimal.ZERO);
        }

        // 3. 计算流动性比率 (现金类资产占比)
        BigDecimal cashAmount = currentSummary.getAssetsByType().getOrDefault("CASH", BigDecimal.ZERO);
        metrics.setCashAmount(cashAmount);
        if (currentSummary.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal liquidityRatio = cashAmount
                .divide(currentSummary.getTotalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setLiquidityRatio(liquidityRatio);
        } else {
            metrics.setLiquidityRatio(BigDecimal.ZERO);
        }

        // 4. 计算月度变化（按比例折算）
        LocalDate previousMonth = targetDate.minusMonths(1);
        metrics.setPreviousMonthDate(previousMonth);
        AssetSummaryDTO previousMonthSummary = getAssetSummary(userId, previousMonth);
        metrics.setPreviousMonthNetWorth(previousMonthSummary.getNetWorth());

        BigDecimal monthlyChange = currentSummary.getNetWorth().subtract(previousMonthSummary.getNetWorth());
        metrics.setMonthlyChange(monthlyChange);

        // 根据实际的时间差按比例折算月度变化率
        if (previousMonthSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0 &&
            previousMonthSummary.getActualDate() != null) {
            // 计算实际的时间差（天数）
            long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                previousMonthSummary.getActualDate(), targetDate);
            long targetDays = 30;  // 目标月度间隔（30天）

            if (actualDays > 0) {
                // 变化率 = (变化金额 / 之前净值) * 100
                BigDecimal rawChangeRate = monthlyChange
                    .divide(previousMonthSummary.getNetWorth(), 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

                // 按比例折算: 折算后变化率 = 原始变化率 * (目标天数 / 实际天数)
                BigDecimal annualizedRate = rawChangeRate
                    .multiply(new BigDecimal(targetDays))
                    .divide(new BigDecimal(actualDays), 4, RoundingMode.HALF_UP);

                metrics.setMonthlyChangeRate(annualizedRate);
            } else {
                metrics.setMonthlyChangeRate(BigDecimal.ZERO);
            }
        } else {
            metrics.setMonthlyChangeRate(BigDecimal.ZERO);
        }

        // 5. 计算年度变化（按比例折算）
        LocalDate previousYear = targetDate.minusYears(1);
        metrics.setPreviousYearDate(previousYear);
        AssetSummaryDTO previousYearSummary = getAssetSummary(userId, previousYear);
        metrics.setPreviousYearNetWorth(previousYearSummary.getNetWorth());

        BigDecimal yearlyChange = currentSummary.getNetWorth().subtract(previousYearSummary.getNetWorth());
        metrics.setYearlyChange(yearlyChange);

        // 根据实际的时间差按比例折算年度变化率
        if (previousYearSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0 &&
            previousYearSummary.getActualDate() != null) {
            // 计算实际的时间差（天数）
            long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                previousYearSummary.getActualDate(), targetDate);
            long targetDays = 365;  // 目标年度间隔（365天）

            if (actualDays > 0) {
                // 变化率 = (变化金额 / 之前净值) * 100
                BigDecimal rawChangeRate = yearlyChange
                    .divide(previousYearSummary.getNetWorth(), 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

                // 按比例折算: 折算后变化率 = 原始变化率 * (目标天数 / 实际天数)
                BigDecimal annualizedRate = rawChangeRate
                    .multiply(new BigDecimal(targetDays))
                    .divide(new BigDecimal(actualDays), 4, RoundingMode.HALF_UP);

                metrics.setYearlyChangeRate(annualizedRate);
            } else {
                metrics.setYearlyChangeRate(BigDecimal.ZERO);
            }
        } else {
            metrics.setYearlyChangeRate(BigDecimal.ZERO);
        }

        return metrics;
    }

    // 获取风险评估
    public RiskAssessmentDTO getRiskAssessment(Long userId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        RiskAssessmentDTO assessment = new RiskAssessmentDTO();
        assessment.setAsOfDate(targetDate);

        // 1. 获取基础财务数据
        AssetSummaryDTO summary = getAssetSummary(userId, targetDate);
        BigDecimal totalAssets = summary.getTotalAssets();
        BigDecimal totalLiabilities = summary.getTotalLiabilities();
        BigDecimal netWorth = summary.getNetWorth();
        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();

        // 2. 资产集中度风险评估 (Concentration Risk)
        RiskAssessmentDTO.ConcentrationRisk concentrationRisk = assessConcentrationRisk(assetsByType, totalAssets);
        assessment.setConcentrationRisk(concentrationRisk);

        // 3. 负债压力评估 (Debt Pressure)
        RiskAssessmentDTO.DebtPressure debtPressure = assessDebtPressure(totalAssets, totalLiabilities);
        assessment.setDebtPressure(debtPressure);

        // 4. 流动性风险评估 (Liquidity Risk)
        BigDecimal cashAmount = assetsByType.getOrDefault("CASH", BigDecimal.ZERO);
        RiskAssessmentDTO.LiquidityRisk liquidityRisk = assessLiquidityRisk(cashAmount, totalAssets);
        assessment.setLiquidityRisk(liquidityRisk);

        // 5. 市场风险评估 (Market Risk)
        RiskAssessmentDTO.MarketRisk marketRisk = assessMarketRisk(assetsByType, totalAssets);
        assessment.setMarketRisk(marketRisk);

        // 6. 计算综合风险评分和等级
        double overallScore = calculateOverallRiskScore(
            concentrationRisk.getScore(),
            debtPressure.getScore(),
            liquidityRisk.getScore(),
            marketRisk.getScore()
        );
        assessment.setOverallRiskScore(overallScore);
        assessment.setOverallRiskLevel(getRiskLevel(overallScore));

        // 7. 生成综合建议
        List<String> recommendations = generateRecommendations(
            concentrationRisk, debtPressure, liquidityRisk, marketRisk
        );
        assessment.setRecommendations(recommendations);

        return assessment;
    }

    // 评估资产集中度风险
    private RiskAssessmentDTO.ConcentrationRisk assessConcentrationRisk(
            Map<String, BigDecimal> assetsByType, BigDecimal totalAssets) {

        RiskAssessmentDTO.ConcentrationRisk risk = new RiskAssessmentDTO.ConcentrationRisk();

        if (totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            risk.setLevel("LOW");
            risk.setScore(0.0);
            risk.setDescription("暂无资产数据");
            risk.setSuggestions(new ArrayList<>());
            return risk;
        }

        // 计算每个类别的占比
        Map<String, Double> percentages = new HashMap<>();
        String topCategory = null;
        double topPercentage = 0.0;

        for (Map.Entry<String, BigDecimal> entry : assetsByType.entrySet()) {
            double percentage = entry.getValue()
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
            percentages.put(entry.getKey(), percentage);

            if (percentage > topPercentage) {
                topPercentage = percentage;
                topCategory = entry.getKey();
            }
        }

        // 计算赫芬达尔指数 (Herfindahl Index)
        double herfindahlIndex = 0.0;
        for (double percentage : percentages.values()) {
            herfindahlIndex += Math.pow(percentage / 100.0, 2);
        }

        // 类型中文名映射
        Map<String, String> typeNames = Map.of(
            "CASH", "现金类",
            "STOCKS", "股票投资",
            "RETIREMENT_FUND", "退休基金",
            "INSURANCE", "保险",
            "REAL_ESTATE", "房地产",
            "CRYPTOCURRENCY", "数字货币",
            "PRECIOUS_METALS", "贵金属",
            "OTHER", "其他"
        );

        risk.setTopConcentratedCategory(typeNames.getOrDefault(topCategory, topCategory));
        risk.setTopConcentrationPercentage(topPercentage);
        risk.setHerfindahlIndex(herfindahlIndex);

        // 评估集中度风险等级和评分
        // HHI: 0.0-0.15 低集中度, 0.15-0.25 中等集中度, >0.25 高集中度
        List<String> suggestions = new ArrayList<>();
        String level;
        double score;
        String description;

        if (herfindahlIndex > 0.5 || topPercentage > 70) {
            level = "HIGH";
            score = 80.0;
            description = String.format("资产高度集中于%s(%.2f%%), 赫芬达尔指数%.2f, 存在较大集中度风险",
                risk.getTopConcentratedCategory(), topPercentage, herfindahlIndex);
            suggestions.add("建议降低" + risk.getTopConcentratedCategory() + "的占比至60%以下");
            suggestions.add("考虑增加其他类型资产的配置,提高资产多样性");
            suggestions.add("定期再平衡投资组合,避免单一资产过度集中");
        } else if (herfindahlIndex > 0.25 || topPercentage > 50) {
            level = "MEDIUM";
            score = 50.0;
            description = String.format("资产集中度中等,主要集中于%s(%.2f%%), 赫芬达尔指数%.2f",
                risk.getTopConcentratedCategory(), topPercentage, herfindahlIndex);
            suggestions.add("适当分散" + risk.getTopConcentratedCategory() + "的投资");
            suggestions.add("考虑增加2-3个其他类型的资产配置");
        } else {
            level = "LOW";
            score = 20.0;
            description = String.format("资产分布较为均衡,赫芬达尔指数%.2f, 集中度风险较低", herfindahlIndex);
            suggestions.add("继续保持资产多样化配置");
        }

        risk.setLevel(level);
        risk.setScore(score);
        risk.setDescription(description);
        risk.setSuggestions(suggestions);

        return risk;
    }

    // 评估负债压力
    private RiskAssessmentDTO.DebtPressure assessDebtPressure(
            BigDecimal totalAssets, BigDecimal totalLiabilities) {

        RiskAssessmentDTO.DebtPressure pressure = new RiskAssessmentDTO.DebtPressure();
        pressure.setTotalAssets(totalAssets.doubleValue());
        pressure.setTotalLiabilities(totalLiabilities.doubleValue());

        // 计算资产负债率
        double debtToAssetRatio = 0.0;
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            debtToAssetRatio = totalLiabilities
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
        }
        pressure.setDebtToAssetRatio(debtToAssetRatio);

        // TODO: 高息负债占比 - 需要在 LiabilityAccount 中添加利率字段
        pressure.setHighInterestDebtRatio(0.0);

        // 评估负债压力等级和评分
        List<String> suggestions = new ArrayList<>();
        String level;
        double score;
        String description;

        if (debtToAssetRatio > 70) {
            level = "HIGH";
            score = 85.0;
            description = String.format("资产负债率%.2f%%, 负债压力过大", debtToAssetRatio);
            suggestions.add("负债比例过高,建议优先偿还高息债务");
            suggestions.add("暂缓新增负债,专注于降低现有负债");
            suggestions.add("考虑出售部分非核心资产用于偿债");
        } else if (debtToAssetRatio > 50) {
            level = "MEDIUM";
            score = 55.0;
            description = String.format("资产负债率%.2f%%, 负债压力中等", debtToAssetRatio);
            suggestions.add("建议制定债务偿还计划,逐步降低负债率");
            suggestions.add("优先偿还利率较高的债务");
            suggestions.add("增加收入来源,加快债务偿还");
        } else if (debtToAssetRatio > 30) {
            level = "LOW";
            score = 30.0;
            description = String.format("资产负债率%.2f%%, 负债压力较小", debtToAssetRatio);
            suggestions.add("负债比例合理,继续保持良好的债务管理");
            suggestions.add("可适当利用低息债务进行投资");
        } else {
            level = "LOW";
            score = 10.0;
            description = String.format("资产负债率%.2f%%, 负债压力很小", debtToAssetRatio);
            suggestions.add("财务状况健康,可根据需要适当利用财务杠杆");
        }

        pressure.setLevel(level);
        pressure.setScore(score);
        pressure.setDescription(description);
        pressure.setSuggestions(suggestions);

        return pressure;
    }

    // 评估流动性风险
    private RiskAssessmentDTO.LiquidityRisk assessLiquidityRisk(
            BigDecimal cashAmount, BigDecimal totalAssets) {

        RiskAssessmentDTO.LiquidityRisk risk = new RiskAssessmentDTO.LiquidityRisk();
        risk.setCashAmount(cashAmount.doubleValue());
        risk.setTotalAssets(totalAssets.doubleValue());

        // 计算流动性比率
        double liquidityRatio = 0.0;
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            liquidityRatio = cashAmount
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
        }
        risk.setLiquidityRatio(liquidityRatio);

        // 建议紧急储备金 (通常为3-6个月支出, 这里简化为总资产的10-20%)
        BigDecimal recommendedFund = totalAssets.multiply(new BigDecimal("0.15"));
        risk.setRecommendedEmergencyFund(recommendedFund.doubleValue());

        // 评估流动性风险等级和评分
        List<String> suggestions = new ArrayList<>();
        String level;
        double score;
        String description;

        if (liquidityRatio < 5) {
            level = "HIGH";
            score = 75.0;
            description = String.format("流动性严重不足(%.2f%%), 应急能力较弱", liquidityRatio);
            suggestions.add("紧急建立应急储备金,至少达到总资产的10%");
            suggestions.add("考虑将部分投资转换为现金或现金等价物");
            suggestions.add("避免将所有资金投入流动性差的资产");
        } else if (liquidityRatio < 10) {
            level = "MEDIUM";
            score = 45.0;
            description = String.format("流动性偏低(%.2f%%), 建议增加现金储备", liquidityRatio);
            suggestions.add("建议将现金储备提高至总资产的15%左右");
            suggestions.add("保持一定比例的活期存款和货币基金");
        } else if (liquidityRatio < 20) {
            level = "LOW";
            score = 20.0;
            description = String.format("流动性适中(%.2f%%), 应急能力良好", liquidityRatio);
            suggestions.add("保持当前的现金储备水平");
            suggestions.add("可适当增加中长期投资");
        } else if (liquidityRatio < 40) {
            level = "LOW";
            score = 15.0;
            description = String.format("流动性充足(%.2f%%), 资金使用灵活", liquidityRatio);
            suggestions.add("流动性充足,可考虑增加部分中长期投资");
        } else {
            level = "MEDIUM";
            score = 40.0;
            description = String.format("现金占比过高(%.2f%%), 资金利用率较低", liquidityRatio);
            suggestions.add("现金占比过高,考虑增加投资以提高收益");
            suggestions.add("保留3-6个月的应急储备即可,其余可投资");
        }

        risk.setLevel(level);
        risk.setScore(score);
        risk.setDescription(description);
        risk.setSuggestions(suggestions);

        return risk;
    }

    // 评估市场风险
    private RiskAssessmentDTO.MarketRisk assessMarketRisk(
            Map<String, BigDecimal> assetsByType, BigDecimal totalAssets) {

        RiskAssessmentDTO.MarketRisk risk = new RiskAssessmentDTO.MarketRisk();

        if (totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            risk.setLevel("LOW");
            risk.setScore(0.0);
            risk.setDescription("暂无资产数据");
            risk.setSuggestions(new ArrayList<>());
            return risk;
        }

        // 计算股票投资占比
        BigDecimal stocksAmount = assetsByType.getOrDefault("STOCKS", BigDecimal.ZERO);
        double stockPercentage = stocksAmount
            .divide(totalAssets, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .doubleValue();
        risk.setStockAllocationPercentage(stockPercentage);

        // 计算数字货币占比
        BigDecimal cryptoAmount = assetsByType.getOrDefault("CRYPTOCURRENCY", BigDecimal.ZERO);
        double cryptoPercentage = cryptoAmount
            .divide(totalAssets, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .doubleValue();
        risk.setCryptoAllocationPercentage(cryptoPercentage);

        // 计算高风险资产总占比 (股票 + 数字货币)
        double highRiskPercentage = stockPercentage + cryptoPercentage;
        risk.setHighRiskAssetsPercentage(highRiskPercentage);

        // 评估市场风险等级和评分
        List<String> suggestions = new ArrayList<>();
        String level;
        double score;
        String description;

        if (highRiskPercentage > 60) {
            level = "HIGH";
            score = 75.0;
            description = String.format("高风险资产占比%.2f%%(股票%.2f%%+数字货币%.2f%%), 市场风险较大",
                highRiskPercentage, stockPercentage, cryptoPercentage);
            suggestions.add("高风险资产占比过高,建议降低至50%以下");
            if (cryptoPercentage > 20) {
                suggestions.add("数字货币风险极高,建议占比控制在10%以内");
            }
            suggestions.add("增加债券、货币基金等低风险资产配置");
            suggestions.add("定期审视市场环境,及时调整仓位");
        } else if (highRiskPercentage > 40) {
            level = "MEDIUM";
            score = 45.0;
            description = String.format("高风险资产占比%.2f%%(股票%.2f%%+数字货币%.2f%%), 市场风险中等",
                highRiskPercentage, stockPercentage, cryptoPercentage);
            suggestions.add("高风险资产占比适中,注意风险控制");
            if (cryptoPercentage > 10) {
                suggestions.add("数字货币占比较高,建议控制在10%以内");
            }
            suggestions.add("保持适当的股债平衡");
        } else if (highRiskPercentage > 20) {
            level = "LOW";
            score = 25.0;
            description = String.format("高风险资产占比%.2f%%, 市场风险较低", highRiskPercentage);
            suggestions.add("风险资产配置合理,可根据风险承受能力调整");
        } else if (highRiskPercentage > 0) {
            level = "LOW";
            score = 15.0;
            description = String.format("高风险资产占比%.2f%%, 市场风险很小", highRiskPercentage);
            suggestions.add("风险资产占比较低,如风险承受能力允许,可适当增加");
        } else {
            level = "LOW";
            score = 10.0;
            description = "未配置风险资产,市场风险极低";
            suggestions.add("可考虑配置少量风险资产以提高收益");
        }

        risk.setLevel(level);
        risk.setScore(score);
        risk.setDescription(description);
        risk.setSuggestions(suggestions);

        return risk;
    }

    // 计算综合风险评分
    private double calculateOverallRiskScore(
            double concentrationScore, double debtScore, double liquidityScore, double marketScore) {
        // 加权平均: 集中度风险25%, 负债压力30%, 流动性风险25%, 市场风险20%
        return concentrationScore * 0.25 + debtScore * 0.30 + liquidityScore * 0.25 + marketScore * 0.20;
    }

    // 获取风险等级
    private String getRiskLevel(double score) {
        if (score >= 70) return "CRITICAL";  // 严重风险
        if (score >= 50) return "HIGH";      // 高风险
        if (score >= 30) return "MEDIUM";    // 中等风险
        return "LOW";                        // 低风险
    }

    // 生成综合建议
    private List<String> generateRecommendations(
            RiskAssessmentDTO.ConcentrationRisk concentrationRisk,
            RiskAssessmentDTO.DebtPressure debtPressure,
            RiskAssessmentDTO.LiquidityRisk liquidityRisk,
            RiskAssessmentDTO.MarketRisk marketRisk) {

        List<String> recommendations = new ArrayList<>();

        // 根据风险等级优先级排序建议
        if ("HIGH".equals(debtPressure.getLevel()) || "CRITICAL".equals(debtPressure.getLevel())) {
            recommendations.add("【优先】负债压力过大,建议优先偿还债务,降低财务风险");
        }

        if ("HIGH".equals(liquidityRisk.getLevel())) {
            recommendations.add("【紧急】流动性严重不足,立即建立应急储备金");
        }

        if ("HIGH".equals(concentrationRisk.getLevel())) {
            recommendations.add("【重要】资产过度集中,需要分散投资降低集中度风险");
        }

        if ("HIGH".equals(marketRisk.getLevel())) {
            recommendations.add("【重要】高风险资产占比过高,建议降低仓位,增加防御性资产");
        }

        // 如果所有风险都较低,给予正面建议
        if ("LOW".equals(concentrationRisk.getLevel()) &&
            "LOW".equals(debtPressure.getLevel()) &&
            "LOW".equals(liquidityRisk.getLevel()) &&
            "LOW".equals(marketRisk.getLevel())) {
            recommendations.add("财务状况整体健康,建议保持当前的资产配置策略");
            recommendations.add("可考虑根据个人目标和风险承受能力,适度增加投资");
        }

        // 一般性建议
        recommendations.add("定期(每季度)审视资产配置,根据市场变化及时调整");
        recommendations.add("建立长期财务规划,明确投资目标和风险承受能力");

        return recommendations;
    }

    // 获取优化建议
    public OptimizationRecommendationDTO getOptimizationRecommendations(Long userId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        // 获取基础数据
        AssetSummaryDTO summary = getAssetSummary(userId, targetDate);
        RiskAssessmentDTO riskAssessment = getRiskAssessment(userId, targetDate);
        FinancialMetricsDTO metrics = getFinancialMetrics(userId, targetDate);

        OptimizationRecommendationDTO optimization = new OptimizationRecommendationDTO();
        optimization.setAsOfDate(targetDate);

        // 1. 计算综合评分
        double overallScore = calculateOverallHealthScore(summary, riskAssessment, metrics);
        optimization.setOverallScore(overallScore);
        optimization.setHealthLevel(getHealthLevel(overallScore));

        // 2. 生成各维度优化建议
        optimization.setAssetAllocationOptimization(
            generateAssetAllocationOptimization(summary, riskAssessment, userId, targetDate)
        );
        optimization.setDebtManagementOptimization(
            generateDebtManagementOptimization(summary, riskAssessment, userId, targetDate)
        );
        optimization.setLiquidityOptimization(
            generateLiquidityOptimization(summary, riskAssessment, userId)
        );
        optimization.setRiskOptimization(
            generateRiskOptimization(riskAssessment)
        );
        optimization.setTaxOptimization(
            generateTaxOptimization(userId, targetDate)
        );

        // 3. 生成优先行动计划
        optimization.setPrioritizedActions(
            generatePrioritizedActions(optimization)
        );

        // 4. 计算预期效果
        optimization.setExpectedImpact(
            calculateExpectedImpact(optimization)
        );

        return optimization;
    }

    // 计算综合健康度评分
    private double calculateOverallHealthScore(AssetSummaryDTO summary,
                                               RiskAssessmentDTO riskAssessment,
                                               FinancialMetricsDTO metrics) {
        // 基础分100分
        double score = 100.0;

        // 风险评分影响 (-40分)
        score -= (riskAssessment.getOverallRiskScore() * 0.4);

        // 资产负债率影响 (-20分)
        if (metrics.getDebtToAssetRatio().compareTo(BigDecimal.ZERO) > 0) {
            double debtRatio = metrics.getDebtToAssetRatio().doubleValue();
            if (debtRatio > 70) {
                score -= 20;
            } else if (debtRatio > 50) {
                score -= 15;
            } else if (debtRatio > 30) {
                score -= 10;
            }
        }

        // 流动性影响 (-20分)
        if (metrics.getLiquidityRatio().compareTo(BigDecimal.ZERO) > 0) {
            double liquidityRatio = metrics.getLiquidityRatio().doubleValue();
            if (liquidityRatio < 5) {
                score -= 20;
            } else if (liquidityRatio < 10) {
                score -= 10;
            } else if (liquidityRatio > 40) {
                score -= 5;
            }
        }

        // 净资产变化影响 (-20分 或 +20分)
        if (metrics.getYearlyChangeRate().compareTo(BigDecimal.ZERO) < 0) {
            score -= 20;  // 净资产负增长
        } else if (metrics.getYearlyChangeRate().compareTo(new BigDecimal("10")) > 0) {
            score = Math.min(100, score + 10);  // 年增长超过10%,加分
        }

        return Math.max(0, Math.min(100, score));
    }

    // 获取健康度等级
    private String getHealthLevel(double score) {
        if (score >= 85) return "EXCELLENT";  // 优秀
        if (score >= 70) return "GOOD";       // 良好
        if (score >= 50) return "FAIR";       // 一般
        return "POOR";                         // 较差
    }

    // 生成资产配置优化建议
    private OptimizationRecommendationDTO.AssetAllocationOptimization generateAssetAllocationOptimization(
            AssetSummaryDTO summary, RiskAssessmentDTO riskAssessment, Long userId, LocalDate targetDate) {

        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();
        BigDecimal totalAssets = summary.getTotalAssets();

        OptimizationRecommendationDTO.AssetAllocationOptimization optimization =
            new OptimizationRecommendationDTO.AssetAllocationOptimization();

        // 计算当前配置百分比
        OptimizationRecommendationDTO.AllocationSnapshot current = calculateAllocationSnapshot(assetsByType, totalAssets);
        optimization.setCurrentAllocation(current);

        // 基于风险评估生成建议配置
        OptimizationRecommendationDTO.AllocationSnapshot recommended = generateRecommendedAllocation(current, riskAssessment);
        optimization.setRecommendedAllocation(recommended);

        // 计算评分
        double score = calculateAllocationScore(current, riskAssessment);
        optimization.setCurrentScore(score);

        // 确定优先级和状态
        if (score < 50) {
            optimization.setPriority("HIGH");
            optimization.setStatus("NEEDS_ATTENTION");
        } else if (score < 75) {
            optimization.setPriority("MEDIUM");
            optimization.setStatus("ACCEPTABLE");
        } else {
            optimization.setPriority("LOW");
            optimization.setStatus("OPTIMAL");
        }

        // 生成具体建议
        List<String> suggestions = new ArrayList<>();
        RiskAssessmentDTO.ConcentrationRisk concentrationRisk = riskAssessment.getConcentrationRisk();

        if (concentrationRisk != null && "HIGH".equals(concentrationRisk.getLevel())) {
            suggestions.add("降低" + concentrationRisk.getTopConcentratedCategory() + "的集中度,目前占比" +
                          String.format("%.1f", concentrationRisk.getTopConcentrationPercentage()) + "%");
        }

        // 现金类资产建议
        if (current.getCashPercentage() < 10) {
            suggestions.add("增加现金储备至总资产的10-15%,提高流动性");
        } else if (current.getCashPercentage() > 40) {
            suggestions.add("现金占比过高(" + String.format("%.1f", current.getCashPercentage()) + "%),建议配置到其他资产类别");
        }

        // 股票投资建议
        if (current.getStocksPercentage() > 60) {
            suggestions.add("股票占比过高,建议降至50%以下,降低市场风险");
        } else if (current.getStocksPercentage() < 20 && current.getCashPercentage() > 30) {
            suggestions.add("在风险承受能力允许的情况下,可适当增加股票配置以提高收益");
        }

        // 退休基金建议
        if (current.getRetirementPercentage() < 15) {
            suggestions.add("增加退休基金配置,建议至少占总资产的15-20%");
        }

        optimization.setSuggestions(suggestions);
        optimization.setSummary(generateAllocationSummary(optimization));
        optimization.setExpectedBenefit("通过优化资产配置,预计可降低10-15%的投资风险,同时提升3-5%的长期收益率");

        return optimization;
    }

    // 计算配置快照
    private OptimizationRecommendationDTO.AllocationSnapshot calculateAllocationSnapshot(
            Map<String, BigDecimal> assetsByType, BigDecimal totalAssets) {

        OptimizationRecommendationDTO.AllocationSnapshot snapshot = new OptimizationRecommendationDTO.AllocationSnapshot();

        if (totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            snapshot.setCashPercentage(0.0);
            snapshot.setStocksPercentage(0.0);
            snapshot.setRetirementPercentage(0.0);
            snapshot.setRealEstatePercentage(0.0);
            snapshot.setOtherPercentage(0.0);
            return snapshot;
        }

        snapshot.setCashPercentage(calculatePercentage(assetsByType.getOrDefault("CASH", BigDecimal.ZERO), totalAssets));
        snapshot.setStocksPercentage(calculatePercentage(assetsByType.getOrDefault("STOCKS", BigDecimal.ZERO), totalAssets));
        snapshot.setRetirementPercentage(calculatePercentage(assetsByType.getOrDefault("RETIREMENT_FUND", BigDecimal.ZERO), totalAssets));
        snapshot.setRealEstatePercentage(calculatePercentage(assetsByType.getOrDefault("REAL_ESTATE", BigDecimal.ZERO), totalAssets));

        BigDecimal others = assetsByType.getOrDefault("INSURANCE", BigDecimal.ZERO)
            .add(assetsByType.getOrDefault("CRYPTOCURRENCY", BigDecimal.ZERO))
            .add(assetsByType.getOrDefault("PRECIOUS_METALS", BigDecimal.ZERO))
            .add(assetsByType.getOrDefault("OTHER", BigDecimal.ZERO));
        snapshot.setOtherPercentage(calculatePercentage(others, totalAssets));

        return snapshot;
    }

    // 计算百分比
    private double calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return amount.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
    }

    // 生成建议配置
    private OptimizationRecommendationDTO.AllocationSnapshot generateRecommendedAllocation(
            OptimizationRecommendationDTO.AllocationSnapshot current,
            RiskAssessmentDTO riskAssessment) {

        // 理想配置(中等风险)
        OptimizationRecommendationDTO.AllocationSnapshot recommended = new OptimizationRecommendationDTO.AllocationSnapshot();
        recommended.setCashPercentage(15.0);
        recommended.setStocksPercentage(35.0);
        recommended.setRetirementPercentage(25.0);
        recommended.setRealEstatePercentage(15.0);
        recommended.setOtherPercentage(10.0);

        // 根据当前风险状况调整
        if (riskAssessment.getLiquidityRisk() != null &&
            "HIGH".equals(riskAssessment.getLiquidityRisk().getLevel())) {
            recommended.setCashPercentage(20.0);
            recommended.setStocksPercentage(30.0);
        }

        if (riskAssessment.getMarketRisk() != null &&
            "HIGH".equals(riskAssessment.getMarketRisk().getLevel())) {
            recommended.setStocksPercentage(25.0);
            recommended.setRetirementPercentage(30.0);
            recommended.setCashPercentage(20.0);
        }

        return recommended;
    }

    // 计算配置评分
    private double calculateAllocationScore(OptimizationRecommendationDTO.AllocationSnapshot current,
                                           RiskAssessmentDTO riskAssessment) {
        double score = 100.0;

        // 集中度扣分
        if (riskAssessment.getConcentrationRisk() != null) {
            score -= riskAssessment.getConcentrationRisk().getScore() * 0.4;
        }

        // 流动性扣分
        if (current.getCashPercentage() < 10) {
            score -= 20;
        } else if (current.getCashPercentage() > 40) {
            score -= 15;
        }

        // 风险资产过高扣分
        double highRiskPercentage = current.getStocksPercentage() +
                                    (current.getOtherPercentage() * 0.5); // 假设其他资产中一半是高风险
        if (highRiskPercentage > 60) {
            score -= 20;
        }

        return Math.max(0, Math.min(100, score));
    }

    // 生成配置摘要
    private String generateAllocationSummary(OptimizationRecommendationDTO.AssetAllocationOptimization optimization) {
        if ("OPTIMAL".equals(optimization.getStatus())) {
            return "资产配置合理,各类别分布均衡,风险可控";
        } else if ("ACCEPTABLE".equals(optimization.getStatus())) {
            return "资产配置基本合理,但存在一定优化空间";
        } else {
            return "资产配置需要调整,当前配置存在较大风险隐患";
        }
    }

    // 生成负债管理优化建议
    private OptimizationRecommendationDTO.DebtManagementOptimization generateDebtManagementOptimization(
            AssetSummaryDTO summary, RiskAssessmentDTO riskAssessment, Long userId, LocalDate targetDate) {

        OptimizationRecommendationDTO.DebtManagementOptimization optimization =
            new OptimizationRecommendationDTO.DebtManagementOptimization();

        BigDecimal totalLiabilities = summary.getTotalLiabilities();
        BigDecimal totalAssets = summary.getTotalAssets();

        // 计算评分
        double debtRatio = 0.0;
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            debtRatio = totalLiabilities.divide(totalAssets, 4, RoundingMode.HALF_UP)
                                       .multiply(new BigDecimal("100"))
                                       .doubleValue();
        }

        double score = 100.0;
        if (debtRatio > 70) {
            score = 20.0;
            optimization.setPriority("HIGH");
            optimization.setStatus("NEEDS_ATTENTION");
        } else if (debtRatio > 50) {
            score = 50.0;
            optimization.setPriority("MEDIUM");
            optimization.setStatus("ACCEPTABLE");
        } else {
            score = 85.0;
            optimization.setPriority("LOW");
            optimization.setStatus("OPTIMAL");
        }

        optimization.setCurrentScore(score);

        // 推荐债务偿还策略
        if (debtRatio > 50) {
            optimization.setRecommendedStrategy("AVALANCHE");  // 高息优先
        } else if (debtRatio > 30) {
            optimization.setRecommendedStrategy("SNOWBALL");   // 小额优先
        } else {
            optimization.setRecommendedStrategy("BALANCED");   // 均衡偿还
        }

        // 生成建议
        List<String> suggestions = new ArrayList<>();
        if (debtRatio > 50) {
            suggestions.add("债务负担较重,建议采用雪崩法(Avalanche)优先偿还高利率债务");
            suggestions.add("暂缓新增投资,将资金优先用于偿债");
            suggestions.add("制定3-5年的债务偿还计划,每年降低10%负债率");
        } else if (debtRatio > 30) {
            suggestions.add("保持良好的债务管理习惯,继续按计划偿还");
            suggestions.add("可考虑债务整合,降低综合利率");
        } else if (debtRatio > 0) {
            suggestions.add("债务水平健康,可适当利用低息贷款进行投资");
        } else {
            suggestions.add("无负债状态,财务灵活性好");
            suggestions.add("可考虑适度利用财务杠杆,提高资产收益率");
        }

        optimization.setSuggestions(suggestions);
        optimization.setHighInterestDebts(new ArrayList<>());  // TODO: 需要获取实际债务数据
        optimization.setSummary(generateDebtSummary(debtRatio));
        optimization.setExpectedSavings(calculateDebtSavings(totalLiabilities));

        return optimization;
    }

    private String generateDebtSummary(double debtRatio) {
        if (debtRatio > 70) {
            return "债务压力严重,需要立即采取行动降低负债";
        } else if (debtRatio > 50) {
            return "债务压力较大,建议优先偿还高息债务";
        } else if (debtRatio > 30) {
            return "债务水平适中,保持良好的偿还节奏";
        } else if (debtRatio > 0) {
            return "债务水平健康,合理利用财务杠杆";
        } else {
            return "无负债状态,财务自由度高";
        }
    }

    private double calculateDebtSavings(BigDecimal totalLiabilities) {
        // 假设平均利率5%,提前偿还可节省的利息
        return totalLiabilities.multiply(new BigDecimal("0.05")).doubleValue();
    }

    // 生成流动性优化建议
    private OptimizationRecommendationDTO.LiquidityOptimization generateLiquidityOptimization(
            AssetSummaryDTO summary, RiskAssessmentDTO riskAssessment, Long userId) {

        OptimizationRecommendationDTO.LiquidityOptimization optimization =
            new OptimizationRecommendationDTO.LiquidityOptimization();

        BigDecimal cashAmount = summary.getAssetsByType().getOrDefault("CASH", BigDecimal.ZERO);

        // 获取用户配置的年度支出
        BigDecimal recommendedCash;
        UserProfile userProfile = userId != null ?
            userProfileRepository.findByUserId(userId).orElse(null) : null;

        if (userProfile != null && userProfile.getEstimatedAnnualExpenses() != null &&
            userProfile.getEstimatedAnnualExpenses().compareTo(BigDecimal.ZERO) > 0) {
            // 使用年度支出 + 50K 作为建议现金储备
            recommendedCash = userProfile.getEstimatedAnnualExpenses()
                .add(new BigDecimal("50000"));
        } else {
            // 如果没有配置年度支出,回退到总资产的15%
            BigDecimal totalAssets = summary.getTotalAssets();
            recommendedCash = totalAssets.multiply(new BigDecimal("0.15"));
        }

        optimization.setCurrentCash(cashAmount.doubleValue());
        optimization.setRecommendedCash(recommendedCash.doubleValue());
        optimization.setGap(cashAmount.subtract(recommendedCash).doubleValue());

        // 计算评分
        RiskAssessmentDTO.LiquidityRisk liquidityRisk = riskAssessment.getLiquidityRisk();
        double score = liquidityRisk != null ? (100 - liquidityRisk.getScore()) : 80.0;
        optimization.setCurrentScore(score);

        if (score < 50) {
            optimization.setPriority("HIGH");
            optimization.setStatus("NEEDS_ATTENTION");
        } else if (score < 75) {
            optimization.setPriority("MEDIUM");
            optimization.setStatus("ACCEPTABLE");
        } else {
            optimization.setPriority("LOW");
            optimization.setStatus("OPTIMAL");
        }

        // 生成建议
        List<String> suggestions = new ArrayList<>();
        if (liquidityRisk != null && liquidityRisk.getSuggestions() != null) {
            suggestions.addAll(liquidityRisk.getSuggestions());
        }

        optimization.setSuggestions(suggestions);
        optimization.setSummary(generateLiquiditySummary(liquidityRisk));

        return optimization;
    }

    private String generateLiquiditySummary(RiskAssessmentDTO.LiquidityRisk liquidityRisk) {
        if (liquidityRisk == null) {
            return "流动性状况良好";
        }
        return liquidityRisk.getDescription();
    }

    // 生成风险优化建议
    private OptimizationRecommendationDTO.RiskOptimization generateRiskOptimization(
            RiskAssessmentDTO riskAssessment) {

        OptimizationRecommendationDTO.RiskOptimization optimization =
            new OptimizationRecommendationDTO.RiskOptimization();

        optimization.setCurrentRiskLevel(riskAssessment.getOverallRiskLevel());
        double riskScore = riskAssessment.getOverallRiskScore();
        optimization.setCurrentScore(100 - riskScore);  // 风险分越高,评分越低

        if (riskScore > 70) {
            optimization.setPriority("HIGH");
            optimization.setStatus("NEEDS_ATTENTION");
        } else if (riskScore > 50) {
            optimization.setPriority("MEDIUM");
            optimization.setStatus("ACCEPTABLE");
        } else {
            optimization.setPriority("LOW");
            optimization.setStatus("OPTIMAL");
        }

        // 生成风险调整建议
        List<OptimizationRecommendationDTO.RiskAdjustment> adjustments = new ArrayList<>();
        RiskAssessmentDTO.MarketRisk marketRisk = riskAssessment.getMarketRisk();

        if (marketRisk != null && "HIGH".equals(marketRisk.getLevel())) {
            if (marketRisk.getStockAllocationPercentage() > 50) {
                OptimizationRecommendationDTO.RiskAdjustment adj = new OptimizationRecommendationDTO.RiskAdjustment();
                adj.setAssetType("股票投资");
                adj.setCurrentPercentage(marketRisk.getStockAllocationPercentage());
                adj.setRecommendedPercentage(40.0);
                adj.setReason("降低股市风险暴露");
                adjustments.add(adj);
            }

            if (marketRisk.getCryptoAllocationPercentage() > 10) {
                OptimizationRecommendationDTO.RiskAdjustment adj = new OptimizationRecommendationDTO.RiskAdjustment();
                adj.setAssetType("数字货币");
                adj.setCurrentPercentage(marketRisk.getCryptoAllocationPercentage());
                adj.setRecommendedPercentage(5.0);
                adj.setReason("数字货币波动性极大,建议控制在5%以内");
                adjustments.add(adj);
            }
        }

        optimization.setAdjustments(adjustments);

        // 收集所有风险建议
        List<String> suggestions = new ArrayList<>();
        if (riskAssessment.getRecommendations() != null) {
            suggestions.addAll(riskAssessment.getRecommendations());
        }
        optimization.setSuggestions(suggestions);
        optimization.setSummary("综合风险评级: " + getRiskLevelName(riskAssessment.getOverallRiskLevel()));

        return optimization;
    }

    // 生成税务优化建议
    private OptimizationRecommendationDTO.TaxOptimization generateTaxOptimization(Long userId, LocalDate targetDate) {
        OptimizationRecommendationDTO.TaxOptimization optimization =
            new OptimizationRecommendationDTO.TaxOptimization();

        // 获取按税收状态的净资产配置
        Map<String, Object> netWorthByTax = getNetWorthByTaxStatus(userId, targetDate);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> taxData = (List<Map<String, Object>>) netWorthByTax.get("data");

        double taxablePercentage = 0.0;
        double total = 0.0;

        for (Map<String, Object> item : taxData) {
            BigDecimal value = (BigDecimal) item.get("value");
            total += value.doubleValue();
            if ("TAXABLE".equals(item.get("taxStatus"))) {
                taxablePercentage = ((BigDecimal) item.get("percentage")).doubleValue();
            }
        }

        optimization.setTaxablePercentage(taxablePercentage);

        // 计算优化空间
        double optimizationPotential = Math.max(0, taxablePercentage - 60);  // 建议应税资产不超过60%
        optimization.setOptimizationPotential(optimizationPotential);

        // 计算评分
        double score = 100.0;
        if (taxablePercentage > 80) {
            score = 40.0;
            optimization.setPriority("MEDIUM");
            optimization.setStatus("NEEDS_ATTENTION");
        } else if (taxablePercentage > 60) {
            score = 70.0;
            optimization.setPriority("LOW");
            optimization.setStatus("ACCEPTABLE");
        } else {
            score = 90.0;
            optimization.setPriority("LOW");
            optimization.setStatus("OPTIMAL");
        }
        optimization.setCurrentScore(score);

        // 生成建议
        List<String> suggestions = new ArrayList<>();
        if (taxablePercentage > 70) {
            suggestions.add("应税资产占比过高(" + String.format("%.1f", taxablePercentage) + "%),建议增加退休账户和免税资产配置");
            suggestions.add("充分利用401(k)、IRA等税收优惠账户");
            suggestions.add("考虑税务递延策略,优化长期收益");
        } else if (taxablePercentage > 60) {
            suggestions.add("可以进一步优化税务结构,增加税收优惠账户投资");
        } else {
            suggestions.add("税务结构合理,继续保持良好的税务规划");
        }

        optimization.setSuggestions(suggestions);
        optimization.setSummary(generateTaxSummary(taxablePercentage));

        return optimization;
    }

    private String generateTaxSummary(double taxablePercentage) {
        if (taxablePercentage > 70) {
            return "税务优化空间较大,建议增加税收优惠账户配置";
        } else if (taxablePercentage > 60) {
            return "税务结构基本合理,有一定优化空间";
        } else {
            return "税务结构优秀,充分利用了税收优惠政策";
        }
    }

    // 生成优先行动计划
    private List<OptimizationRecommendationDTO.ActionItem> generatePrioritizedActions(
            OptimizationRecommendationDTO optimization) {

        List<OptimizationRecommendationDTO.ActionItem> actions = new ArrayList<>();
        int order = 1;

        // 债务管理 - 如果债务压力高,优先处理
        if ("HIGH".equals(optimization.getDebtManagementOptimization().getPriority())) {
            for (String suggestion : optimization.getDebtManagementOptimization().getSuggestions()) {
                OptimizationRecommendationDTO.ActionItem action = new OptimizationRecommendationDTO.ActionItem();
                action.setCategory("DEBT");
                action.setPriority("CRITICAL");
                action.setAction(suggestion);
                action.setTimeframe("IMMEDIATE");
                action.setExpectedImpact("降低财务风险,减少利息支出");
                action.setOrder(order++);
                actions.add(action);
            }
        }

        // 流动性 - 如果流动性不足,紧急处理
        if ("HIGH".equals(optimization.getLiquidityOptimization().getPriority())) {
            OptimizationRecommendationDTO.ActionItem action = new OptimizationRecommendationDTO.ActionItem();
            action.setCategory("LIQUIDITY");
            action.setPriority("CRITICAL");
            action.setAction("建立紧急储备金至建议水平");
            action.setTimeframe("SHORT_TERM");
            action.setExpectedImpact("提高应急能力,降低流动性风险");
            action.setOrder(order++);
            actions.add(action);
        }

        // 资产配置调整
        if ("HIGH".equals(optimization.getAssetAllocationOptimization().getPriority())) {
            OptimizationRecommendationDTO.ActionItem action = new OptimizationRecommendationDTO.ActionItem();
            action.setCategory("ASSET_ALLOCATION");
            action.setPriority("HIGH");
            action.setAction(optimization.getAssetAllocationOptimization().getSuggestions().get(0));
            action.setTimeframe("MEDIUM_TERM");
            action.setExpectedImpact("优化风险收益比,提升长期收益");
            action.setOrder(order++);
            actions.add(action);
        }

        // 风险调整
        if (optimization.getRiskOptimization().getAdjustments() != null &&
            !optimization.getRiskOptimization().getAdjustments().isEmpty()) {
            OptimizationRecommendationDTO.ActionItem action = new OptimizationRecommendationDTO.ActionItem();
            action.setCategory("RISK");
            action.setPriority("MEDIUM");
            action.setAction("调整高风险资产配置,降低整体风险水平");
            action.setTimeframe("MEDIUM_TERM");
            action.setExpectedImpact("降低投资组合波动性");
            action.setOrder(order++);
            actions.add(action);
        }

        // 税务优化
        if ("MEDIUM".equals(optimization.getTaxOptimization().getPriority())) {
            OptimizationRecommendationDTO.ActionItem action = new OptimizationRecommendationDTO.ActionItem();
            action.setCategory("TAX");
            action.setPriority("LOW");
            action.setAction(optimization.getTaxOptimization().getSuggestions().get(0));
            action.setTimeframe("LONG_TERM");
            action.setExpectedImpact("减少税务负担,提高税后收益");
            action.setOrder(order++);
            actions.add(action);
        }

        return actions;
    }

    // 计算预期效果
    private OptimizationRecommendationDTO.ExpectedImpact calculateExpectedImpact(
            OptimizationRecommendationDTO optimization) {

        OptimizationRecommendationDTO.ExpectedImpact impact = new OptimizationRecommendationDTO.ExpectedImpact();

        // 基于各维度的改善空间估算
        double avgScore = (
            optimization.getAssetAllocationOptimization().getCurrentScore() +
            optimization.getDebtManagementOptimization().getCurrentScore() +
            optimization.getLiquidityOptimization().getCurrentScore() +
            optimization.getRiskOptimization().getCurrentScore() +
            optimization.getTaxOptimization().getCurrentScore()
        ) / 5.0;

        // 改善潜力 = (100 - 当前平均分) * 0.7 (假设可以改善70%的差距)
        double improvementPotential = (100 - avgScore) * 0.7;

        impact.setNetWorthIncrease(improvementPotential * 0.5);  // 净资产增长潜力
        impact.setRiskReduction(improvementPotential * 0.8);     // 风险降低潜力
        impact.setReturnImprovement(improvementPotential * 0.3); // 收益提升潜力
        impact.setTaxSavings(optimization.getTaxOptimization().getOptimizationPotential() * 1000);  // 粗略估算

        // 生成综合描述
        if (optimization.getOverallScore() < 50) {
            impact.setOverallImprovement("通过系统性优化,预计可将财务健康度提升至良好水平,显著降低财务风险");
        } else if (optimization.getOverallScore() < 70) {
            impact.setOverallImprovement("通过针对性调整,预计可进一步提升财务健康度至优秀水平");
        } else if (optimization.getOverallScore() < 85) {
            impact.setOverallImprovement("财务状况良好,通过精细化管理可进一步优化");
        } else {
            impact.setOverallImprovement("财务状况优秀,保持当前策略并根据市场变化适时调整");
        }

        return impact;
    }

    private String getRiskLevelName(String level) {
        Map<String, String> names = Map.of(
            "LOW", "低风险",
            "MEDIUM", "中等风险",
            "HIGH", "高风险",
            "CRITICAL", "严重风险"
        );
        return names.getOrDefault(level, "未知");
    }

    // 获取按货币的净资产配置
    public Map<String, Object> getNetWorthByCurrency(Long userId, LocalDate asOfDate) {
        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (userId == null) {
            assetAccounts = accountRepository.findByIsActiveTrue();
        } else {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 按货币分组资产
        Map<String, BigDecimal> assetsByCurrency = new HashMap<>();

        for (AssetAccount account : assetAccounts) {
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用账户的原始货币和金额（不转换）
                String currency = account.getCurrency() != null ? account.getCurrency() : "CNY";
                BigDecimal amount = record.get().getAmount(); // 使用原始金额，不是基础货币金额
                assetsByCurrency.merge(currency, amount, BigDecimal::add);
            }
        }

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 按货币分组负债
        Map<String, BigDecimal> liabilitiesByCurrency = new HashMap<>();

        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                // 使用账户的原始货币和余额（不转换）
                String currency = account.getCurrency() != null ? account.getCurrency() : "CNY";
                BigDecimal balance = record.get().getOutstandingBalance(); // 使用原始余额，不是基础货币余额
                liabilitiesByCurrency.merge(currency, balance, BigDecimal::add);
            }
        }

        // 合并所有货币
        Set<String> allCurrencies = new HashSet<>();
        allCurrencies.addAll(assetsByCurrency.keySet());
        allCurrencies.addAll(liabilitiesByCurrency.keySet());

        // 计算每种货币的净资产
        Map<String, BigDecimal> netWorthByCurrency = new HashMap<>();
        BigDecimal totalNetWorthInBaseCurrency = BigDecimal.ZERO;

        for (String currency : allCurrencies) {
            BigDecimal assets = assetsByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal liabilities = liabilitiesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal netWorth = assets.subtract(liabilities);

            if (netWorth.compareTo(BigDecimal.ZERO) != 0) {
                netWorthByCurrency.put(currency, netWorth);
            }
        }

        // 计算总净资产（使用基础货币金额进行汇总）
        // 同时计算每种货币的净资产（基础货币金额）
        Map<String, BigDecimal> netWorthInBaseCurrencyByOriginalCurrency = new HashMap<>();

        for (String currency : allCurrencies) {
            BigDecimal assetsInOriginal = assetsByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal liabilitiesInOriginal = liabilitiesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal netWorthInOriginal = assetsInOriginal.subtract(liabilitiesInOriginal);

            // 只添加非零的净资产
            if (netWorthInOriginal.compareTo(BigDecimal.ZERO) != 0) {
                netWorthByCurrency.put(currency, netWorthInOriginal);
            }

            // 获取该货币的账户，计算基础货币金额
            BigDecimal assetsInBase = BigDecimal.ZERO;
            BigDecimal liabilitiesInBase = BigDecimal.ZERO;

            // 计算资产的基础货币金额
            List<AssetAccount> currencyAssetAccounts = assetAccounts.stream()
                .filter(acc -> currency.equals(acc.getCurrency() != null ? acc.getCurrency() : "CNY"))
                .collect(java.util.stream.Collectors.toList());

            for (AssetAccount account : currencyAssetAccounts) {
                Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    // 使用查询日期的汇率重新计算金额
                    AssetRecord assetRecord = record.get();
                    BigDecimal amountInUSD = convertToUSD(
                        assetRecord.getAmount(),
                        assetRecord.getCurrency(),
                        asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                    );
                    assetsInBase = assetsInBase.add(amountInUSD);
                }
            }

            // 计算负债的基础货币金额
            List<LiabilityAccount> currencyLiabilityAccounts = liabilityAccounts.stream()
                .filter(acc -> currency.equals(acc.getCurrency() != null ? acc.getCurrency() : "CNY"))
                .collect(java.util.stream.Collectors.toList());

            for (LiabilityAccount account : currencyLiabilityAccounts) {
                Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    // 使用查询日期的汇率重新计算金额
                    LiabilityRecord liabilityRecord = record.get();
                    BigDecimal balanceInUSD = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                    liabilitiesInBase = liabilitiesInBase.add(balanceInUSD);
                }
            }

            BigDecimal netWorthInBase = assetsInBase.subtract(liabilitiesInBase);
            if (netWorthInBase.compareTo(BigDecimal.ZERO) != 0) {
                netWorthInBaseCurrencyByOriginalCurrency.put(currency, netWorthInBase);
                totalNetWorthInBaseCurrency = totalNetWorthInBaseCurrency.add(netWorthInBase);
            }
        }

        // 货币中文名映射
        Map<String, String> currencyNames = Map.of(
            "CNY", "人民币",
            "USD", "美元",
            "EUR", "欧元",
            "GBP", "英镑",
            "JPY", "日元",
            "HKD", "港币",
            "AUD", "澳元",
            "CAD", "加元"
        );

        // 构建返回数据
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : netWorthByCurrency.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            String currency = entry.getKey();
            BigDecimal netWorth = entry.getValue();

            item.put("currency", currency);
            item.put("name", currencyNames.getOrDefault(currency, currency));
            item.put("value", netWorth);
            item.put("assets", assetsByCurrency.getOrDefault(currency, BigDecimal.ZERO));
            item.put("liabilities", liabilitiesByCurrency.getOrDefault(currency, BigDecimal.ZERO));

            // 计算百分比（基于基础货币金额的总净资产）
            BigDecimal netWorthInBase = netWorthInBaseCurrencyByOriginalCurrency.getOrDefault(currency, BigDecimal.ZERO);
            if (totalNetWorthInBaseCurrency.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal percentage = netWorthInBase
                    .divide(totalNetWorthInBaseCurrency, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }

            // 添加基础货币金额，用于前端饼图的扇形大小计算
            item.put("valueInBaseCurrency", netWorthInBase);

            data.add(item);
        }

        // 按净值降序排序
        data.sort((a, b) -> {
            BigDecimal valueA = (BigDecimal) a.get("value");
            BigDecimal valueB = (BigDecimal) b.get("value");
            return valueB.compareTo(valueA);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalNetWorthInBaseCurrency);
        result.put("data", data);
        result.put("asOfDate", asOfDate != null ? asOfDate.toString() : LocalDate.now().toString());

        return result;
    }
}
