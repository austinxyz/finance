package com.finance.app.service;

import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.FinancialMetricsDTO;
import com.finance.app.dto.OverallTrendDataPointDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetCategoryRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityRecordRepository;
import com.finance.app.repository.NetAssetCategoryRepository;
import com.finance.app.repository.NetAssetCategoryAssetTypeMappingRepository;
import com.finance.app.repository.NetAssetCategoryLiabilityTypeMappingRepository;
import com.finance.app.model.NetAssetCategory;
import com.finance.app.model.NetAssetCategoryAssetTypeMapping;
import com.finance.app.model.NetAssetCategoryLiabilityTypeMapping;
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

    // 获取资产总览
    public AssetSummaryDTO getAssetSummary(Long userId) {
        return getAssetSummary(userId, null);
    }

    // 获取指定日期的资产总览
    public AssetSummaryDTO getAssetSummary(Long userId, LocalDate asOfDate) {
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
        LocalDate earliestDate = null;  // 追踪实际使用的最早数据日期

        for (AssetAccount account : accounts) {
            // 根据asOfDate获取记录
            Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                BigDecimal amount = record.get().getAmountInBaseCurrency();
                totalAssets = totalAssets.add(amount);

                // 追踪最早的记录日期
                LocalDate recordDate = record.get().getRecordDate();
                if (earliestDate == null || recordDate.isBefore(earliestDate)) {
                    earliestDate = recordDate;
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
                BigDecimal balance = record.get().getBalanceInBaseCurrency();
                totalLiabilities = totalLiabilities.add(balance);

                // 追踪最早的记录日期
                LocalDate recordDate = record.get().getRecordDate();
                if (earliestDate == null || recordDate.isBefore(earliestDate)) {
                    earliestDate = recordDate;
                }
            }
        }

        AssetSummaryDTO summary = new AssetSummaryDTO();
        summary.setTotalAssets(totalAssets);
        summary.setTotalLiabilities(totalLiabilities);
        summary.setNetWorth(totalAssets.subtract(totalLiabilities));
        summary.setAssetsByCategory(assetsByCategory);
        summary.setAssetsByType(assetsByType);
        summary.setActualDate(earliestDate);  // 设置实际使用的最早数据日期

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
                dto.setAmount(record.getAmountInBaseCurrency());
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
    public List<OverallTrendDataPointDTO> getOverallTrend(String startDateStr, String endDateStr, Long userId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取所有资产账户
        List<AssetAccount> assetAccounts;
        if (userId == null) {
            assetAccounts = accountRepository.findByIsActiveTrue();
        } else {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 获取日期范围内所有资产记录
        Map<LocalDate, BigDecimal> assetsByDate = new HashMap<>();
        for (AssetAccount account : assetAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                assetsByDate.merge(record.getRecordDate(), record.getAmountInBaseCurrency(), BigDecimal::add);
            }
        }

        // 获取日期范围内所有负债记录
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : liabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                liabilitiesByDate.merge(record.getRecordDate(), record.getBalanceInBaseCurrency(), BigDecimal::add);
            }
        }

        // 合并所有日期
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(assetsByDate.keySet());
        allDates.addAll(liabilitiesByDate.keySet());

        // 创建趋势数据点
        List<OverallTrendDataPointDTO> result = new ArrayList<>();
        for (LocalDate date : allDates) {
            BigDecimal assets = assetsByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal liabilities = liabilitiesByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal netWorth = assets.subtract(liabilities);

            OverallTrendDataPointDTO point = new OverallTrendDataPointDTO();
            point.setDate(date.toString());
            point.setTotalAssets(assets);
            point.setTotalLiabilities(liabilities);
            point.setNetWorth(netWorth);

            result.add(point);
        }

        // 按日期排序
        result.sort(Comparator.comparing(OverallTrendDataPointDTO::getDate));

        return result;
    }

    // 获取资产分类趋势数据
    public List<TrendDataPointDTO> getAssetCategoryTrend(String categoryType, String startDateStr, String endDateStr, Long userId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有资产账户
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

        // 获取日期范围内的记录并按日期汇总
        Map<LocalDate, BigDecimal> totalByDate = new HashMap<>();
        for (AssetAccount account : filteredAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                totalByDate.merge(record.getRecordDate(), record.getAmountInBaseCurrency(), BigDecimal::add);
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
    public List<TrendDataPointDTO> getLiabilityCategoryTrend(String categoryType, String startDateStr, String endDateStr, Long userId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有负债账户
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

        // 获取日期范围内的记录并按日期汇总
        Map<LocalDate, BigDecimal> totalByDate = new HashMap<>();
        for (LiabilityAccount account : filteredAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                totalByDate.merge(record.getRecordDate(), record.getBalanceInBaseCurrency(), BigDecimal::add);
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
    public List<TrendDataPointDTO> getNetAssetCategoryTrend(String categoryCode, String startDateStr, String endDateStr, Long userId) {
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
        if (userId == null) {
            assetAccounts = accountRepository.findByIsActiveTrue();
        } else {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        // 过滤出匹配类型的资产账户
        List<AssetAccount> filteredAssetAccounts = assetAccounts.stream()
            .filter(acc -> acc.getCategory() != null && assetTypes.contains(acc.getCategory().getType()))
            .collect(Collectors.toList());

        // 获取所有负债账户
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
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
                assetsByDate.merge(record.getRecordDate(), record.getAmountInBaseCurrency(), BigDecimal::add);
            }
        }

        // 获取日期范围内的负债记录并按日期汇总
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : filteredLiabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                liabilitiesByDate.merge(record.getRecordDate(), record.getBalanceInBaseCurrency(), BigDecimal::add);
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
            String categoryType, String startDateStr, String endDateStr, Long userId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有资产账户
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
                    point.setBalance(record.getAmountInBaseCurrency());
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
            String categoryType, String startDateStr, String endDateStr, Long userId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 获取该类型的所有负债账户
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
                    point.setBalance(record.getBalanceInBaseCurrency());
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
                BigDecimal amount = record.get().getAmountInBaseCurrency();
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
                BigDecimal balance = record.get().getBalanceInBaseCurrency();
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
}
