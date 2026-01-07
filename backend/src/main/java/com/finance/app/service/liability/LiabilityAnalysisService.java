package com.finance.app.service.liability;

import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.User;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityRecordRepository;
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
public class LiabilityAnalysisService {

    private final LiabilityAccountRepository accountRepository;
    private final LiabilityRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;

    // 获取按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId) {
        return getLiabilityAllocationByType(userId, null, null, "All");
    }

    // 获取指定日期的按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId, LocalDate asOfDate) {
        return getLiabilityAllocationByType(userId, null, asOfDate, "All");
    }

    // 获取指定家庭和日期的按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId, Long familyId, LocalDate asOfDate) {
        return getLiabilityAllocationByType(userId, familyId, asOfDate, "All");
    }

    // 获取指定家庭、日期和货币的按类型的负债配置
    public Map<String, Object> getLiabilityAllocationByType(Long userId, Long familyId, LocalDate asOfDate, String currency) {
        // 优先级：familyId > userId > 所有账户
        List<LiabilityAccount> accounts;
        if (familyId != null) {
            accounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 货币筛选
        if (!"All".equalsIgnoreCase(currency)) {
            accounts = accounts.stream()
                .filter(acc -> acc.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
        }

        Map<String, BigDecimal> liabilitiesByType = new HashMap<>();
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (LiabilityAccount account : accounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal amount;
                if ("All".equalsIgnoreCase(currency)) {
                    // All模式：转换为USD
                    amount = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                } else {
                    // 单货币模式：使用原始金额
                    amount = liabilityRecord.getOutstandingBalance();
                }
                totalLiabilities = totalLiabilities.add(amount);

                // 按类型汇总
                String typeName = account.getLiabilityType() != null ?
                    account.getLiabilityType().getType() : "OTHER";
                liabilitiesByType.merge(typeName, amount, BigDecimal::add);
            }
        }

        // 类型中文名映射
        Map<String, String> typeNames = Map.of(
            "MORTGAGE", "抵押贷款",
            "CREDIT_CARD", "信用卡",
            "AUTO_LOAN", "汽车贷款",
            "STUDENT_LOAN", "学生贷款",
            "PERSONAL_LOAN", "个人贷款",
            "OTHER", "其他"
        );

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : liabilitiesByType.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", typeNames.getOrDefault(entry.getKey(), entry.getKey()));
            item.put("value", entry.getValue());

            // 计算百分比
            if (totalLiabilities.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = entry.getValue()
                    .divide(totalLiabilities, 4, RoundingMode.HALF_UP)
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
        result.put("total", totalLiabilities);
        result.put("data", data);

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
            accounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getLiabilityType() != null && categoryType.equals(acc.getLiabilityType().getType()))
            .collect(Collectors.toList());

        // 获取日期范围内的记录并按日期汇总
        Map<LocalDate, BigDecimal> totalByDate = new HashMap<>();
        for (LiabilityAccount account : filteredAccounts) {
            List<LiabilityRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                // 使用记录日期的汇率重新计算金额
                BigDecimal amount = convertToUSD(
                    record.getOutstandingBalance(),
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

    // 获取指定类型和日期的负债账户及其余额
    public List<Map<String, Object>> getLiabilityAccountsWithBalancesByType(String categoryType, Long userId, Long familyId, LocalDate asOfDate) {
        // 获取所有活跃账户
        List<LiabilityAccount> accounts;
        if (familyId != null) {
            accounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }

        // 过滤出匹配类型的账户
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getLiabilityType() != null && categoryType.equals(acc.getLiabilityType().getType()))
            .collect(Collectors.toList());

        // 获取每个账户在指定日期的余额
        List<Map<String, Object>> result = new ArrayList<>();
        for (LiabilityAccount account : filteredAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal balance = convertToUSD(
                    liabilityRecord.getOutstandingBalance(),
                    liabilityRecord.getCurrency(),
                    asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                );
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("accountName", account.getAccountName());
                accountData.put("balance", balance);
                result.add(accountData);
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
        List<LiabilityAccount> filteredAccounts = accounts.stream()
            .filter(acc -> acc.getLiabilityType() != null && categoryType.equals(acc.getLiabilityType().getType()))
            .collect(Collectors.toList());

        // 为每个账户获取趋势数据
        Map<String, List<AccountTrendDataPointDTO>> result = new HashMap<>();

        for (LiabilityAccount account : filteredAccounts) {
            List<LiabilityRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
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

    // 获取指定日期或之前最近的负债记录
    private Optional<LiabilityRecord> getLiabilityRecordAsOfDate(Long accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            // 如果没有指定日期，获取最新记录
            return recordRepository.findLatestByAccountId(accountId);
        } else {
            // 获取指定日期或之前最近的记录
            List<LiabilityRecord> records = recordRepository.findByAccountIdAndRecordDateBeforeOrEqual(accountId, asOfDate);
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
