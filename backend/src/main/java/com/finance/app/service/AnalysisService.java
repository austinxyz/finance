package com.finance.app.service;

import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetCategoryRepository;
import com.finance.app.repository.AssetRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AssetAccountRepository accountRepository;
    private final AssetRecordRepository recordRepository;
    private final AssetCategoryRepository categoryRepository;

    // 获取资产总览
    public AssetSummaryDTO getAssetSummary(Long userId) {
        List<AssetAccount> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);

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

        AssetSummaryDTO summary = new AssetSummaryDTO();
        summary.setTotalAssets(totalAssets);
        summary.setTotalLiabilities(BigDecimal.ZERO); // TODO: 实现负债统计
        summary.setNetWorth(totalAssets);
        summary.setAssetsByCategory(assetsByCategory);
        summary.setAssetsByType(assetsByType);

        return summary;
    }

    // 获取总资产趋势
    public List<TrendDataDTO> getTotalAssetTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        final LocalDate finalStartDate = (startDate == null) ? LocalDate.now().minusMonths(12) : startDate;
        final LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        List<AssetAccount> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
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
}
