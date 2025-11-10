package com.finance.app.service;

import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.TrendDataDTO;
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

        for (AssetAccount account : accounts) {
            // 获取最新记录
            Optional<AssetRecord> latestRecord = recordRepository.findLatestByAccountId(account.getId());
            if (latestRecord.isPresent()) {
                BigDecimal amount = latestRecord.get().getAmountInBaseCurrency();
                totalAssets = totalAssets.add(amount);

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
            Optional<LiabilityRecord> latestRecord = liabilityRecordRepository.findLatestByAccountId(account.getId());
            if (latestRecord.isPresent()) {
                BigDecimal balance = latestRecord.get().getBalanceInBaseCurrency();
                totalLiabilities = totalLiabilities.add(balance);
            }
        }

        AssetSummaryDTO summary = new AssetSummaryDTO();
        summary.setTotalAssets(totalAssets);
        summary.setTotalLiabilities(totalLiabilities);
        summary.setNetWorth(totalAssets.subtract(totalLiabilities));
        summary.setAssetsByCategory(assetsByCategory);
        summary.setAssetsByType(assetsByType);

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
        AssetSummaryDTO summary = getAssetSummary(userId);
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
        // 获取所有净资产类别
        List<NetAssetCategory> netAssetCategories = netAssetCategoryRepository.findAllByOrderByDisplayOrderAsc();

        // 获取资产和负债数据
        AssetSummaryDTO summary = getAssetSummary(userId);
        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();

        // 计算每个负债类型的总额
        Map<String, BigDecimal> liabilitiesByType = calculateLiabilitiesByType(userId);

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
        Map<String, BigDecimal> liabilitiesByType = calculateLiabilitiesByType(userId);

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
        List<LiabilityAccount> liabilityAccounts;
        if (userId == null) {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        } else {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        }

        Map<String, BigDecimal> liabilitiesByType = new HashMap<>();

        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> latestRecord = liabilityRecordRepository.findLatestByAccountId(account.getId());
            if (latestRecord.isPresent()) {
                BigDecimal balance = latestRecord.get().getBalanceInBaseCurrency();

                String typeName = account.getCategory() != null ?
                    account.getCategory().getType() : "OTHER";
                liabilitiesByType.merge(typeName, balance, BigDecimal::add);
            }
        }

        return liabilitiesByType;
    }
}
