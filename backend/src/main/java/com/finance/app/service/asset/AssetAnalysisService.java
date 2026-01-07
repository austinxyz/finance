package com.finance.app.service.asset;

import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.User;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.UserRepository;
import com.finance.app.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetAnalysisService {

    private final AssetAccountRepository accountRepository;
    private final AssetRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;

    // 获取资产总览
    public AssetSummaryDTO getAssetSummary(Long userId) {
        return getAssetSummary(userId, null, null);
    }

    // 获取指定日期的资产总览
    public AssetSummaryDTO getAssetSummary(Long userId, LocalDate asOfDate) {
        return getAssetSummary(userId, null, asOfDate, false);
    }

    // 获取指定日期的资产总览（支持familyId）
    public AssetSummaryDTO getAssetSummary(Long userId, Long familyId, LocalDate asOfDate) {
        return getAssetSummary(userId, familyId, asOfDate, false);
    }

    // 获取指定日期的资产总览（可选择是否包含自住房）
    public AssetSummaryDTO getAssetSummary(Long userId, Long familyId, LocalDate asOfDate, boolean includePrimaryResidence) {
        return getAssetSummary(userId, familyId, asOfDate, includePrimaryResidence, "All");
    }

    // 获取指定日期的资产总览（支持货币筛选）
    public AssetSummaryDTO getAssetSummary(Long userId, Long familyId, LocalDate asOfDate, boolean includePrimaryResidence, String currency) {
        // 优先级：familyId > userId > 所有账户
        List<AssetAccount> accounts;
        if (familyId != null) {
            // 按家庭ID查询
            accounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            // 按用户ID查询
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            // 获取所有活跃账户
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 货币筛选：如果不是"All"，则只包含指定货币的账户
        if (!"All".equalsIgnoreCase(currency)) {
            accounts = accounts.stream()
                .filter(acc -> acc.getCurrency().equalsIgnoreCase(currency))
                .collect(java.util.stream.Collectors.toList());
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
                BigDecimal amount;
                if ("All".equalsIgnoreCase(currency)) {
                    // All模式：转换为USD
                    amount = convertToUSD(
                        assetRecord.getAmount(),
                        assetRecord.getCurrency(),
                        asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                    );
                } else {
                    // 单货币模式：使用原始金额，不转换
                    amount = assetRecord.getAmount();
                }
                totalAssets = totalAssets.add(amount);

                // 追踪实际使用的最新数据日期
                LocalDate recordDate = record.get().getRecordDate();
                if (actualDate == null || recordDate.isAfter(actualDate)) {
                    actualDate = recordDate;
                }

                // 按分类汇总
                String categoryName = account.getAssetType() != null ?
                    account.getAssetType().getChineseName() : "未分类";
                assetsByCategory.merge(categoryName, amount, BigDecimal::add);

                // 按类型汇总
                String typeName = account.getAssetType() != null ?
                    account.getAssetType().getType() : "OTHER";
                assetsByType.merge(typeName, amount, BigDecimal::add);
            }
        }

        AssetSummaryDTO summary = new AssetSummaryDTO();
        summary.setTotalAssets(totalAssets);
        summary.setAssetsByCategory(assetsByCategory);
        summary.setAssetsByType(assetsByType);
        summary.setActualDate(actualDate);  // 设置实际使用的数据日期

        return summary;
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
                    record -> convertToUSD(record.getAmount(), record.getCurrency(), record.getRecordDate()),
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
                    if (account.getAssetType() != null) {
                        dto.setCategoryName(account.getAssetType().getChineseName());
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
        return getAssetAllocationByType(userId, null, null, "All");
    }

    // 获取指定日期的按类型的资产配置
    public Map<String, Object> getAssetAllocationByType(Long userId, LocalDate asOfDate) {
        return getAssetAllocationByType(userId, null, asOfDate, "All");
    }

    // 获取指定家庭和日期的按类型的资产配置
    public Map<String, Object> getAssetAllocationByType(Long userId, Long familyId, LocalDate asOfDate) {
        return getAssetAllocationByType(userId, familyId, asOfDate, "All");
    }

    // 获取指定家庭、日期和货币的按类型的资产配置
    public Map<String, Object> getAssetAllocationByType(Long userId, Long familyId, LocalDate asOfDate, String currency) {
        AssetSummaryDTO summary = getAssetSummary(userId, familyId, asOfDate, false, currency);
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
            .filter(acc -> acc.getAssetType() != null && categoryType.equals(acc.getAssetType().getType()))
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

    // 获取指定类型和日期的资产账户及其余额
    public List<Map<String, Object>> getAssetAccountsWithBalancesByType(String categoryType, Long userId, Long familyId, LocalDate asOfDate) {
        // 获取所有活跃账户
        List<AssetAccount> accounts;
        if (familyId != null) {
            accounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<AssetAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getAssetType() != null && categoryType.equals(acc.getAssetType().getType()))
            .collect(Collectors.toList());

        // 获取每个账户在指定日期的余额
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetAccount account : filteredAccounts) {
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                AssetRecord assetRecord = record.get();
                BigDecimal balance = convertToUSD(
                    assetRecord.getAmount(),
                    assetRecord.getCurrency(),
                    asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                );
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("accountName", account.getAccountName());
                accountData.put("balance", balance);
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
            .filter(acc -> acc.getAssetType() != null && categoryType.equals(acc.getAssetType().getType()))
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
}
