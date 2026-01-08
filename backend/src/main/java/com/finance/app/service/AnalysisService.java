package com.finance.app.service;

import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.EnhancedFinancialMetricsDTO;
import com.finance.app.dto.FinancialMetricsDTO;
import com.finance.app.dto.OptimizationRecommendationDTO;
import com.finance.app.dto.OverallTrendDataPointDTO;
import com.finance.app.dto.RiskAssessmentDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.User;
import com.finance.app.repository.AssetAccountRepository;
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
import com.finance.app.service.asset.AssetAnalysisService;
import com.finance.app.service.liability.LiabilityAnalysisService;
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
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;
    private final NetAssetCategoryRepository netAssetCategoryRepository;
    private final NetAssetCategoryAssetTypeMappingRepository assetTypeMappingRepository;
    private final NetAssetCategoryLiabilityTypeMappingRepository liabilityTypeMappingRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    private final InvestmentAnalysisService investmentAnalysisService;
    private final com.finance.app.service.expense.ExpenseAnalysisService expenseAnalysisService;
    private final com.finance.app.service.income.IncomeAnalysisService incomeAnalysisService;

    // Inject the new services
    private final AssetAnalysisService assetAnalysisService;
    private final LiabilityAnalysisService liabilityAnalysisService;

    // ==============================================
    // Net asset methods - combine asset and liability data
    // ==============================================

    /**
     * Add liability data to asset summary to get complete financial overview
     */
    public AssetSummaryDTO addLiabilityDataToSummary(AssetSummaryDTO summary, Long userId, Long familyId, LocalDate asOfDate, String currency) {
        BigDecimal totalLiabilities = calculateTotalLiabilities(userId, familyId, asOfDate, currency);
        summary.setTotalLiabilities(totalLiabilities);
        summary.setNetWorth(summary.getTotalAssets().subtract(totalLiabilities));
        return summary;
    }

    // Helper method to calculate total liabilities
    private BigDecimal calculateTotalLiabilities(Long userId, Long familyId, LocalDate asOfDate, String currency) {
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            liabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        if (!"All".equalsIgnoreCase(currency)) {
            liabilityAccounts = liabilityAccounts.stream()
                .filter(acc -> acc.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
        }

        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal balance;
                if ("All".equalsIgnoreCase(currency)) {
                    balance = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                } else {
                    balance = liabilityRecord.getOutstandingBalance();
                }
                totalLiabilities = totalLiabilities.add(balance);
            }
        }

        return totalLiabilities;
    }

    public Map<String, Object> getNetAssetAllocation(Long userId) {
        return getNetAssetAllocation(userId, null, null, "All");
    }

    public Map<String, Object> getNetAssetAllocation(Long userId, LocalDate asOfDate) {
        return getNetAssetAllocation(userId, null, asOfDate, "All");
    }

    public Map<String, Object> getNetAssetAllocation(Long userId, Long familyId, LocalDate asOfDate) {
        return getNetAssetAllocation(userId, familyId, asOfDate, "All");
    }

    public Map<String, Object> getNetAssetAllocation(Long userId, Long familyId, LocalDate asOfDate, String currency) {
        // Get all net asset categories
        List<NetAssetCategory> netAssetCategories = netAssetCategoryRepository.findAllByOrderByDisplayOrderAsc();

        // Get asset data from AssetAnalysisService
        AssetSummaryDTO summary = assetAnalysisService.getAssetSummary(userId, familyId, asOfDate, false, currency);
        Map<String, BigDecimal> assetsByType = summary.getAssetsByType();

        // Calculate liabilities by type
        Map<String, BigDecimal> liabilitiesByType = calculateLiabilitiesByType(userId, familyId, asOfDate, currency);

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal totalNetAssets = BigDecimal.ZERO;

        for (NetAssetCategory netCategory : netAssetCategories) {
            // Get asset type mappings for this net asset category
            List<NetAssetCategoryAssetTypeMapping> assetMappings =
                assetTypeMappingRepository.findByNetAssetCategoryId(netCategory.getId());

            // Get liability type mappings for this net asset category
            List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings =
                liabilityTypeMappingRepository.findByNetAssetCategoryId(netCategory.getId());

            // Calculate total assets for this category
            BigDecimal categoryAssets = BigDecimal.ZERO;
            for (NetAssetCategoryAssetTypeMapping mapping : assetMappings) {
                BigDecimal assetAmount = assetsByType.getOrDefault(mapping.getAssetType(), BigDecimal.ZERO);
                categoryAssets = categoryAssets.add(assetAmount);
            }

            // Calculate total liabilities for this category
            BigDecimal categoryLiabilities = BigDecimal.ZERO;
            for (NetAssetCategoryLiabilityTypeMapping mapping : liabilityMappings) {
                BigDecimal liabilityAmount = liabilitiesByType.getOrDefault(mapping.getLiabilityType(), BigDecimal.ZERO);
                categoryLiabilities = categoryLiabilities.add(liabilityAmount);
            }

            // Calculate net asset (assets - liabilities)
            BigDecimal netAsset = categoryAssets.subtract(categoryLiabilities);

            // Only add categories with non-zero net value (including negative values)
            if (netAsset.compareTo(BigDecimal.ZERO) != 0) {
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

        // Calculate percentages
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

        // Sort by net value descending
        data.sort((a, b) -> ((BigDecimal)b.get("netValue")).compareTo((BigDecimal)a.get("netValue")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalNetAssets);
        result.put("data", data);

        return result;
    }

    // Calculate liabilities by type
    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId) {
        return calculateLiabilitiesByType(userId, null, null, "All");
    }

    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId, LocalDate asOfDate) {
        return calculateLiabilitiesByType(userId, null, asOfDate, "All");
    }

    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId, Long familyId, LocalDate asOfDate) {
        return calculateLiabilitiesByType(userId, familyId, asOfDate, "All");
    }

    private Map<String, BigDecimal> calculateLiabilitiesByType(Long userId, Long familyId, LocalDate asOfDate, String currency) {
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            liabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        if (!"All".equalsIgnoreCase(currency)) {
            liabilityAccounts = liabilityAccounts.stream()
                .filter(acc -> acc.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
        }

        Map<String, BigDecimal> liabilitiesByType = new HashMap<>();

        for (LiabilityAccount account : liabilityAccounts) {
            Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
            if (record.isPresent()) {
                LiabilityRecord liabilityRecord = record.get();
                BigDecimal balance;
                if ("All".equalsIgnoreCase(currency)) {
                    balance = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                } else {
                    balance = liabilityRecord.getOutstandingBalance();
                }

                String typeName = account.getLiabilityType() != null ?
                    account.getLiabilityType().getType() : "OTHER";
                liabilitiesByType.merge(typeName, balance, BigDecimal::add);
            }
        }

        return liabilitiesByType;
    }

    // Get overall trend (combines assets and liabilities)
    public List<OverallTrendDataPointDTO> getOverallTrend(String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // Get all asset accounts
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            assetAccounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // Get all liability accounts
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            liabilityAccounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // Get asset records by date
        Map<LocalDate, BigDecimal> assetsByDate = new HashMap<>();
        for (AssetAccount account : assetAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                assetsByDate.merge(record.getRecordDate(), amount, BigDecimal::add);
            }
        }

        // Get liability records by date
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : liabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                BigDecimal balance = convertToUSD(
                    record.getOutstandingBalance(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                liabilitiesByDate.merge(record.getRecordDate(), balance, BigDecimal::add);
            }
        }

        // Merge all dates
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(assetsByDate.keySet());
        allDates.addAll(liabilitiesByDate.keySet());

        // Create trend data points
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

        // Convert to list and sort by date
        List<OverallTrendDataPointDTO> result = new ArrayList<>(dailyData.values());
        result.sort(Comparator.comparing(OverallTrendDataPointDTO::getDate));

        return result;
    }

    // Get net asset category trend
    public List<TrendDataPointDTO> getNetAssetCategoryTrend(String categoryCode, String startDateStr, String endDateStr, Long familyId) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // Get net asset category
        Optional<NetAssetCategory> categoryOpt = netAssetCategoryRepository.findByCode(categoryCode);
        if (categoryOpt.isEmpty()) {
            return new ArrayList<>();
        }
        NetAssetCategory category = categoryOpt.get();

        // Get asset type mappings
        List<NetAssetCategoryAssetTypeMapping> assetMappings = assetTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> assetTypes = assetMappings.stream()
            .map(NetAssetCategoryAssetTypeMapping::getAssetType)
            .collect(Collectors.toSet());

        // Get liability type mappings
        List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings = liabilityTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> liabilityTypes = liabilityMappings.stream()
            .map(NetAssetCategoryLiabilityTypeMapping::getLiabilityType)
            .collect(Collectors.toSet());

        // Get all asset accounts
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            assetAccounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // Filter matching asset accounts
        List<AssetAccount> filteredAssetAccounts = assetAccounts.stream()
            .filter(acc -> acc.getAssetType() != null && assetTypes.contains(acc.getAssetType().getType()))
            .collect(Collectors.toList());

        // Get all liability accounts
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            List<User> familyMembers = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Long> userIds = familyMembers.stream().map(User::getId).collect(Collectors.toList());
            liabilityAccounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // Filter matching liability accounts
        List<LiabilityAccount> filteredLiabilityAccounts = liabilityAccounts.stream()
            .filter(acc -> acc.getLiabilityType() != null && liabilityTypes.contains(acc.getLiabilityType().getType()))
            .collect(Collectors.toList());

        // Get asset records by date
        Map<LocalDate, BigDecimal> assetsByDate = new HashMap<>();
        for (AssetAccount account : filteredAssetAccounts) {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (AssetRecord record : records) {
                BigDecimal amount = convertToUSD(
                    record.getAmount(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                assetsByDate.merge(record.getRecordDate(), amount, BigDecimal::add);
            }
        }

        // Get liability records by date
        Map<LocalDate, BigDecimal> liabilitiesByDate = new HashMap<>();
        for (LiabilityAccount account : filteredLiabilityAccounts) {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
                account.getId(), startDate, endDate);
            for (LiabilityRecord record : records) {
                BigDecimal balance = convertToUSD(
                    record.getOutstandingBalance(),
                    record.getCurrency(),
                    record.getRecordDate()
                );
                liabilitiesByDate.merge(record.getRecordDate(), balance, BigDecimal::add);
            }
        }

        // Merge all dates
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(assetsByDate.keySet());
        allDates.addAll(liabilitiesByDate.keySet());

        // Calculate net value for each date (assets - liabilities)
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

        // Sort by date
        result.sort(Comparator.comparing(TrendDataPointDTO::getDate));

        return result;
    }

    // Get net asset category accounts
    public Map<String, Object> getNetAssetCategoryAccounts(String categoryCode, Long userId, Long familyId, LocalDate asOfDate) {
        // Get net asset category
        Optional<NetAssetCategory> categoryOpt = netAssetCategoryRepository.findByCode(categoryCode);
        if (categoryOpt.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("assetAccounts", new ArrayList<>());
            emptyResult.put("liabilityAccounts", new ArrayList<>());
            return emptyResult;
        }
        NetAssetCategory category = categoryOpt.get();

        // Get asset type mappings
        List<NetAssetCategoryAssetTypeMapping> assetMappings = assetTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> assetTypes = assetMappings.stream()
            .map(NetAssetCategoryAssetTypeMapping::getAssetType)
            .collect(Collectors.toSet());

        // Get liability type mappings
        List<NetAssetCategoryLiabilityTypeMapping> liabilityMappings = liabilityTypeMappingRepository.findByNetAssetCategoryId(category.getId());
        Set<String> liabilityTypes = liabilityMappings.stream()
            .map(NetAssetCategoryLiabilityTypeMapping::getLiabilityType)
            .collect(Collectors.toSet());

        // Get all asset accounts
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            assetAccounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // Filter and get balances for asset accounts
        List<Map<String, Object>> assetAccountsData = new ArrayList<>();
        for (AssetAccount account : assetAccounts) {
            if (account.getAssetType() != null && assetTypes.contains(account.getAssetType().getType())) {
                Optional<AssetRecord> record = getAssetRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    AssetRecord assetRecord = record.get();
                    BigDecimal balance = convertToUSD(
                        assetRecord.getAmount(),
                        assetRecord.getCurrency(),
                        asOfDate != null ? asOfDate : assetRecord.getRecordDate()
                    );
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("accountId", account.getId());
                    accountData.put("accountName", account.getAccountName());
                    accountData.put("categoryType", account.getAssetType().getType());
                    accountData.put("categoryName", account.getAssetType().getChineseName());
                    accountData.put("balance", balance);
                    assetAccountsData.add(accountData);
                }
            }
        }

        // Get all liability accounts
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            liabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        // Filter and get balances for liability accounts
        List<Map<String, Object>> liabilityAccountsData = new ArrayList<>();
        for (LiabilityAccount account : liabilityAccounts) {
            if (account.getLiabilityType() != null && liabilityTypes.contains(account.getLiabilityType().getType())) {
                Optional<LiabilityRecord> record = getLiabilityRecordAsOfDate(account.getId(), asOfDate);
                if (record.isPresent()) {
                    LiabilityRecord liabilityRecord = record.get();
                    BigDecimal balance = convertToUSD(
                        liabilityRecord.getOutstandingBalance(),
                        liabilityRecord.getCurrency(),
                        asOfDate != null ? asOfDate : liabilityRecord.getRecordDate()
                    );
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("accountId", account.getId());
                    accountData.put("accountName", account.getAccountName());
                    accountData.put("categoryType", account.getLiabilityType().getType());
                    accountData.put("categoryName", account.getLiabilityType().getChineseName());
                    accountData.put("balance", balance);
                    liabilityAccountsData.add(accountData);
                }
            }
        }

        // Sort by balance descending
        assetAccountsData.sort((a, b) -> ((BigDecimal)b.get("balance")).compareTo((BigDecimal)a.get("balance")));
        liabilityAccountsData.sort((a, b) -> ((BigDecimal)b.get("balance")).compareTo((BigDecimal)a.get("balance")));

        Map<String, Object> result = new HashMap<>();
        result.put("assetAccounts", assetAccountsData);
        result.put("liabilityAccounts", liabilityAccountsData);
        result.put("categoryName", category.getName());
        result.put("categoryCode", categoryCode);

        return result;
    }

    // Get net worth by tax status
    // OPTIMIZED: Batch query to reduce N+1 queries
    public Map<String, Object> getNetWorthByTaxStatus(Long userId, Long familyId, LocalDate asOfDate) {
        // Get all asset accounts
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            assetAccounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // Get all liability accounts
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            liabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        // OPTIMIZATION: Batch query all records
        List<Long> assetAccountIds = assetAccounts.stream().map(AssetAccount::getId).collect(Collectors.toList());
        List<Long> liabilityAccountIds = liabilityAccounts.stream().map(LiabilityAccount::getId).collect(Collectors.toList());

        Map<Long, AssetRecord> assetRecordMap = new HashMap<>();
        if (!assetAccountIds.isEmpty()) {
            List<AssetRecord> records = recordRepository.findLatestByAccountIdsBeforeOrEqualDate(assetAccountIds, asOfDate);
            for (AssetRecord record : records) {
                assetRecordMap.put(record.getAccountId(), record);
            }
        }

        Map<Long, LiabilityRecord> liabilityRecordMap = new HashMap<>();
        if (!liabilityAccountIds.isEmpty()) {
            List<LiabilityRecord> records = liabilityRecordRepository.findLatestByAccountIdsBeforeOrEqualDate(liabilityAccountIds, asOfDate);
            for (LiabilityRecord record : records) {
                liabilityRecordMap.put(record.getAccountId(), record);
            }
        }

        // Group assets by tax status
        Map<String, BigDecimal> assetsByTaxStatus = new HashMap<>();
        assetsByTaxStatus.put("TAXABLE", BigDecimal.ZERO);
        assetsByTaxStatus.put("TAX_FREE", BigDecimal.ZERO);
        assetsByTaxStatus.put("TAX_DEFERRED", BigDecimal.ZERO);

        for (AssetAccount account : assetAccounts) {
            AssetRecord assetRecord = assetRecordMap.get(account.getId());
            if (assetRecord != null) {
                BigDecimal amount = convertToUSD(
                    assetRecord.getAmount(),
                    assetRecord.getCurrency(),
                    assetRecord.getRecordDate()
                );
                String taxStatus = account.getTaxStatus() != null ? account.getTaxStatus().name() : "TAXABLE";
                assetsByTaxStatus.merge(taxStatus, amount, BigDecimal::add);
            }
        }

        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (LiabilityAccount account : liabilityAccounts) {
            LiabilityRecord liabilityRecord = liabilityRecordMap.get(account.getId());
            if (liabilityRecord != null) {
                BigDecimal balance = convertToUSD(
                    liabilityRecord.getOutstandingBalance(),
                    liabilityRecord.getCurrency(),
                    liabilityRecord.getRecordDate()
                );
                totalLiabilities = totalLiabilities.add(balance);
            }
        }

        // Deduct liabilities from assets (taxable first, then tax-free, then tax-deferred)
        BigDecimal remainingLiabilities = totalLiabilities;

        // 1. Deduct from taxable assets first
        BigDecimal taxableAssets = assetsByTaxStatus.get("TAXABLE");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxableAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxableAssets.compareTo(remainingLiabilities) >= 0) {
                assetsByTaxStatus.put("TAXABLE", taxableAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                remainingLiabilities = remainingLiabilities.subtract(taxableAssets);
                assetsByTaxStatus.put("TAXABLE", BigDecimal.ZERO);
            }
        }

        // 2. Deduct from tax-free assets
        BigDecimal taxFreeAssets = assetsByTaxStatus.get("TAX_FREE");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxFreeAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxFreeAssets.compareTo(remainingLiabilities) >= 0) {
                assetsByTaxStatus.put("TAX_FREE", taxFreeAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                remainingLiabilities = remainingLiabilities.subtract(taxFreeAssets);
                assetsByTaxStatus.put("TAX_FREE", BigDecimal.ZERO);
            }
        }

        // 3. Deduct from tax-deferred assets
        BigDecimal taxDeferredAssets = assetsByTaxStatus.get("TAX_DEFERRED");
        if (remainingLiabilities.compareTo(BigDecimal.ZERO) > 0 && taxDeferredAssets.compareTo(BigDecimal.ZERO) > 0) {
            if (taxDeferredAssets.compareTo(remainingLiabilities) >= 0) {
                assetsByTaxStatus.put("TAX_DEFERRED", taxDeferredAssets.subtract(remainingLiabilities));
                remainingLiabilities = BigDecimal.ZERO;
            } else {
                remainingLiabilities = remainingLiabilities.subtract(taxDeferredAssets);
                assetsByTaxStatus.put("TAX_DEFERRED", BigDecimal.ZERO);
            }
        }

        // Calculate total net worth
        BigDecimal totalNetWorth = BigDecimal.ZERO;
        for (BigDecimal value : assetsByTaxStatus.values()) {
            totalNetWorth = totalNetWorth.add(value);
        }

        // Tax status Chinese names
        Map<String, String> taxStatusNames = Map.of(
            "TAXABLE", "应税",
            "TAX_FREE", "免税",
            "TAX_DEFERRED", "延税"
        );

        // Build result data
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : assetsByTaxStatus.entrySet()) {
            // Only add categories with value > 0
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("taxStatus", entry.getKey());
                item.put("name", taxStatusNames.getOrDefault(entry.getKey(), entry.getKey()));
                item.put("value", entry.getValue());

                // Calculate percentage
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

        // Sort by value descending
        data.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalNetWorth);
        result.put("data", data);

        return result;
    }

    // Get net worth by family member - OPTIMIZED: Batch query to reduce N+1 queries
    public Map<String, Object> getNetWorthByMember(Long userId, Long familyId, LocalDate asOfDate) {
        List<User> users;
        if (familyId != null) {
            users = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else {
            users = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .collect(Collectors.toList());
        }

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        // Get all user IDs
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Map.of("total", BigDecimal.ZERO, "data", new ArrayList<>());
        }

        // OPTIMIZATION: Batch query all accounts for all users
        List<AssetAccount> allAssetAccounts = accountRepository.findByUserIdInAndIsActiveTrue(userIds);
        List<LiabilityAccount> allLiabilityAccounts = liabilityAccountRepository.findByUserIdInAndIsActiveTrue(userIds);

        // OPTIMIZATION: Batch query all records
        List<Long> assetAccountIds = allAssetAccounts.stream().map(AssetAccount::getId).collect(Collectors.toList());
        List<Long> liabilityAccountIds = allLiabilityAccounts.stream().map(LiabilityAccount::getId).collect(Collectors.toList());

        Map<Long, AssetRecord> assetRecordMap = new HashMap<>();
        if (!assetAccountIds.isEmpty()) {
            List<AssetRecord> records = recordRepository.findLatestByAccountIdsBeforeOrEqualDate(assetAccountIds, asOfDate);
            for (AssetRecord record : records) {
                assetRecordMap.put(record.getAccountId(), record);
            }
        }

        Map<Long, LiabilityRecord> liabilityRecordMap = new HashMap<>();
        if (!liabilityAccountIds.isEmpty()) {
            List<LiabilityRecord> records = liabilityRecordRepository.findLatestByAccountIdsBeforeOrEqualDate(liabilityAccountIds, asOfDate);
            for (LiabilityRecord record : records) {
                liabilityRecordMap.put(record.getAccountId(), record);
            }
        }

        // Group accounts by user
        Map<Long, List<AssetAccount>> assetAccountsByUser = allAssetAccounts.stream()
            .collect(Collectors.groupingBy(AssetAccount::getUserId));
        Map<Long, List<LiabilityAccount>> liabilityAccountsByUser = allLiabilityAccounts.stream()
            .collect(Collectors.groupingBy(LiabilityAccount::getUserId));

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> memberData = new ArrayList<>();
        BigDecimal totalNetWorth = BigDecimal.ZERO;

        // Calculate net worth for each member
        for (User user : users) {
            BigDecimal userTotalAssets = BigDecimal.ZERO;
            BigDecimal userTotalLiabilities = BigDecimal.ZERO;

            // Calculate assets
            List<AssetAccount> userAssets = assetAccountsByUser.getOrDefault(user.getId(), Collections.emptyList());
            for (AssetAccount account : userAssets) {
                AssetRecord record = assetRecordMap.get(account.getId());
                if (record != null) {
                    BigDecimal value = convertToUSD(
                        record.getAmount(),
                        record.getCurrency(),
                        record.getRecordDate()
                    );
                    userTotalAssets = userTotalAssets.add(value);
                }
            }

            // Calculate liabilities
            List<LiabilityAccount> userLiabilities = liabilityAccountsByUser.getOrDefault(user.getId(), Collections.emptyList());
            for (LiabilityAccount account : userLiabilities) {
                LiabilityRecord record = liabilityRecordMap.get(account.getId());
                if (record != null) {
                    BigDecimal value = convertToUSD(
                        record.getOutstandingBalance(),
                        record.getCurrency(),
                        record.getRecordDate()
                    );
                    userTotalLiabilities = userTotalLiabilities.add(value);
                }
            }

            BigDecimal userNetWorth = userTotalAssets.subtract(userTotalLiabilities);

            // Only add members with non-zero net worth
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

        // Calculate percentages
        for (Map<String, Object> member : memberData) {
            BigDecimal value = (BigDecimal) member.get("value");
            if (totalNetWorth.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = value
                    .divide(totalNetWorth, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                member.put("percentage", percentage);
            } else {
                member.put("percentage", BigDecimal.ZERO);
            }
        }

        // Sort by value descending
        memberData.sort((a, b) -> ((BigDecimal)b.get("value")).compareTo((BigDecimal)a.get("value")));

        result.put("total", totalNetWorth);
        result.put("data", memberData);

        return result;
    }

    // Get net worth by currency - OPTIMIZED: Batch query to reduce N+1 queries
    public Map<String, Object> getNetWorthByCurrency(Long userId, Long familyId, LocalDate asOfDate) {
        // Get all asset accounts
        List<AssetAccount> assetAccounts;
        if (familyId != null) {
            assetAccounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            assetAccounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            assetAccounts = accountRepository.findByIsActiveTrue();
        }

        // Get all liability accounts
        List<LiabilityAccount> liabilityAccounts;
        if (familyId != null) {
            liabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            liabilityAccounts = liabilityAccountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            liabilityAccounts = liabilityAccountRepository.findByIsActiveTrue();
        }

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        // OPTIMIZATION: Batch query all records
        List<Long> assetAccountIds = assetAccounts.stream().map(AssetAccount::getId).collect(Collectors.toList());
        List<Long> liabilityAccountIds = liabilityAccounts.stream().map(LiabilityAccount::getId).collect(Collectors.toList());

        Map<Long, AssetRecord> assetRecordMap = new HashMap<>();
        if (!assetAccountIds.isEmpty()) {
            List<AssetRecord> records = recordRepository.findLatestByAccountIdsBeforeOrEqualDate(assetAccountIds, asOfDate);
            for (AssetRecord record : records) {
                assetRecordMap.put(record.getAccountId(), record);
            }
        }

        Map<Long, LiabilityRecord> liabilityRecordMap = new HashMap<>();
        if (!liabilityAccountIds.isEmpty()) {
            List<LiabilityRecord> records = liabilityRecordRepository.findLatestByAccountIdsBeforeOrEqualDate(liabilityAccountIds, asOfDate);
            for (LiabilityRecord record : records) {
                liabilityRecordMap.put(record.getAccountId(), record);
            }
        }

        // Group assets by currency (without conversion)
        Map<String, BigDecimal> assetsByCurrency = new HashMap<>();
        for (AssetAccount account : assetAccounts) {
            AssetRecord assetRecord = assetRecordMap.get(account.getId());
            if (assetRecord != null) {
                String currency = assetRecord.getCurrency();
                BigDecimal amount = assetRecord.getAmount();
                assetsByCurrency.merge(currency, amount, BigDecimal::add);
            }
        }

        // Group liabilities by currency (without conversion)
        Map<String, BigDecimal> liabilitiesByCurrency = new HashMap<>();
        for (LiabilityAccount account : liabilityAccounts) {
            LiabilityRecord liabilityRecord = liabilityRecordMap.get(account.getId());
            if (liabilityRecord != null) {
                String currency = liabilityRecord.getCurrency();
                BigDecimal balance = liabilityRecord.getOutstandingBalance();
                liabilitiesByCurrency.merge(currency, balance, BigDecimal::add);
            }
        }

        // Calculate net worth per currency
        Set<String> allCurrencies = new HashSet<>();
        allCurrencies.addAll(assetsByCurrency.keySet());
        allCurrencies.addAll(liabilitiesByCurrency.keySet());

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal totalNetWorthInUSD = BigDecimal.ZERO;

        for (String currency : allCurrencies) {
            BigDecimal assets = assetsByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal liabilities = liabilitiesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal netWorth = assets.subtract(liabilities);

            // Only add currencies with non-zero net worth
            if (netWorth.compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("currency", currency);
                item.put("name", getCurrencyName(currency));  // Add display name
                item.put("assets", assets);
                item.put("liabilities", liabilities);
                item.put("netWorth", netWorth);
                item.put("value", netWorth);  // Alias for frontend compatibility

                // Convert to USD for total calculation
                BigDecimal netWorthInUSD = convertToUSD(netWorth, currency, asOfDate);
                item.put("netWorthInUSD", netWorthInUSD);
                item.put("valueInBaseCurrency", netWorthInUSD);  // Alias for frontend compatibility
                totalNetWorthInUSD = totalNetWorthInUSD.add(netWorthInUSD);

                data.add(item);
            }
        }

        // Calculate percentages
        for (Map<String, Object> item : data) {
            BigDecimal netWorthInUSD = (BigDecimal) item.get("netWorthInUSD");
            if (totalNetWorthInUSD.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = netWorthInUSD
                    .divide(totalNetWorthInUSD, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                item.put("percentage", percentage);
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }
        }

        // Sort by net worth in USD descending
        data.sort((a, b) -> ((BigDecimal)b.get("netWorthInUSD")).compareTo((BigDecimal)a.get("netWorthInUSD")));

        Map<String, Object> result = new HashMap<>();
        result.put("totalInUSD", totalNetWorthInUSD);
        result.put("data", data);

        return result;
    }

    // ==============================================
    // Helper methods (kept private in AnalysisService)
    // ==============================================

    private Optional<AssetRecord> getAssetRecordAsOfDate(Long accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            return recordRepository.findLatestByAccountId(accountId);
        } else {
            List<AssetRecord> records = recordRepository.findByAccountIdAndRecordDateBeforeOrEqual(accountId, asOfDate);
            return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        }
    }

    private Optional<LiabilityRecord> getLiabilityRecordAsOfDate(Long accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            return liabilityRecordRepository.findLatestByAccountId(accountId);
        } else {
            List<LiabilityRecord> records = liabilityRecordRepository.findByAccountIdAndRecordDateBeforeOrEqual(accountId, asOfDate);
            return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        }
    }

    private BigDecimal convertToBaseCurrency(BigDecimal amount, String currency, LocalDate asOfDate, String baseCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        LocalDate conversionDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        if (currency == null || currency.equalsIgnoreCase(baseCurrency)) {
            return amount;
        }

        if (baseCurrency == null || baseCurrency.equalsIgnoreCase("USD")) {
            BigDecimal rateToUsd = exchangeRateService.getExchangeRate(currency, conversionDate);
            return amount.multiply(rateToUsd).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rateToUsd = exchangeRateService.getExchangeRate(currency, conversionDate);
        BigDecimal amountInUsd = amount.multiply(rateToUsd);

        BigDecimal baseRateToUsd = exchangeRateService.getExchangeRate(baseCurrency, conversionDate);
        return amountInUsd.divide(baseRateToUsd, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal convertToUSD(BigDecimal amount, String currency, LocalDate asOfDate) {
        return convertToBaseCurrency(amount, currency, asOfDate, "USD");
    }

    private String getCurrencyName(String currencyCode) {
        Map<String, String> currencyNames = Map.ofEntries(
            entry("USD", "美元"),
            entry("CNY", "人民币"),
            entry("EUR", "欧元"),
            entry("GBP", "英镑"),
            entry("JPY", "日元"),
            entry("HKD", "港币"),
            entry("AUD", "澳元"),
            entry("CAD", "加元"),
            entry("SGD", "新加坡元"),
            entry("CHF", "瑞士法郎"),
            entry("NZD", "新西兰元"),
            entry("KRW", "韩元"),
            entry("TWD", "新台币")
        );
        return currencyNames.getOrDefault(currencyCode, currencyCode);
    }

    // 获取财务指标
    public FinancialMetricsDTO getFinancialMetrics(Long userId, Long familyId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();
        Integer currentYear = targetDate.getYear();

        FinancialMetricsDTO metrics = new FinancialMetricsDTO();
        metrics.setAsOfDate(targetDate);
        metrics.setYear(currentYear);

        // 1. 获取当前净资产（当前日期的资产负债情况）
        AssetSummaryDTO currentSummary = assetAnalysisService.getAssetSummary(userId, familyId, targetDate);
        currentSummary = addLiabilityDataToSummary(currentSummary, userId, familyId, targetDate, "All");
        metrics.setCurrentNetWorth(currentSummary.getNetWorth());

        // 2. 获取去年净资产（去年12月31日的资产负债情况）
        LocalDate lastYearEndDate = LocalDate.of(currentYear - 1, 12, 31);
        AssetSummaryDTO lastYearSummary = assetAnalysisService.getAssetSummary(userId, familyId, lastYearEndDate);
        lastYearSummary = addLiabilityDataToSummary(lastYearSummary, userId, familyId, lastYearEndDate, "All");
        metrics.setLastYearNetWorth(lastYearSummary.getNetWorth());

        // 3. 获取本年度投资回报（从投资分析服务）
        try {
            // 调用投资分析服务获取所有大类的年度投资回报，然后求和
            List<com.finance.app.dto.InvestmentCategoryAnalysisDTO> categoryAnalysis =
                investmentAnalysisService.getAnnualByCategory(familyId, currentYear, "USD");

            if (categoryAnalysis != null && !categoryAnalysis.isEmpty()) {
                BigDecimal totalReturns = categoryAnalysis.stream()
                    .map(com.finance.app.dto.InvestmentCategoryAnalysisDTO::getReturns)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                metrics.setAnnualInvestmentReturn(totalReturns);
            } else {
                metrics.setAnnualInvestmentReturn(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            // 如果获取投资回报失败，设置为0
            metrics.setAnnualInvestmentReturn(BigDecimal.ZERO);
        }

        // 4. 获取本年度实际支出（从支出分析服务的年度汇总）
        try {
            // 调用支出分析服务获取年度支出汇总（包含调整后的实际支出）
            List<com.finance.app.dto.expense.AnnualExpenseSummaryDTO> expenseSummary =
                expenseAnalysisService.getAnnualExpenseSummaryWithAdjustments(familyId, currentYear, "USD", true);

            // 查找总计行（majorCategoryId == 0）
            com.finance.app.dto.expense.AnnualExpenseSummaryDTO totalRow = expenseSummary.stream()
                .filter(item -> item.getMajorCategoryId() != null && item.getMajorCategoryId() == 0L)
                .findFirst()
                .orElse(null);

            if (totalRow != null && totalRow.getActualExpenseAmount() != null) {
                metrics.setAnnualExpense(totalRow.getActualExpenseAmount());
            } else {
                metrics.setAnnualExpense(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            // 如果获取支出失败，设置为0
            metrics.setAnnualExpense(BigDecimal.ZERO);
        }

        // 5. 计算工作收入：工作收入 = (当前净资产 - 去年净资产) - 投资回报 + 实际支出
        BigDecimal netWorthChange = metrics.getCurrentNetWorth().subtract(metrics.getLastYearNetWorth());
        BigDecimal annualWorkIncome = netWorthChange
            .subtract(metrics.getAnnualInvestmentReturn())
            .add(metrics.getAnnualExpense());
        metrics.setAnnualWorkIncome(annualWorkIncome);

        // 6. 填充已废弃字段（保持向后兼容）
        metrics.setTotalAssets(currentSummary.getTotalAssets());
        metrics.setTotalLiabilities(currentSummary.getTotalLiabilities());
        metrics.setNetWorth(currentSummary.getNetWorth());

        // 计算资产负债率
        if (currentSummary.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = currentSummary.getTotalLiabilities()
                .divide(currentSummary.getTotalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setDebtToAssetRatio(debtRatio);
        } else {
            metrics.setDebtToAssetRatio(BigDecimal.ZERO);
        }

        // 计算流动性比率
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

        // 月度变化
        LocalDate previousMonth = targetDate.minusMonths(1);
        metrics.setPreviousMonthDate(previousMonth);
        AssetSummaryDTO previousMonthSummary = assetAnalysisService.getAssetSummary(userId, familyId, previousMonth);
        previousMonthSummary = addLiabilityDataToSummary(previousMonthSummary, userId, familyId, previousMonth, "All");
        metrics.setPreviousMonthNetWorth(previousMonthSummary.getNetWorth());
        BigDecimal monthlyChange = currentSummary.getNetWorth().subtract(previousMonthSummary.getNetWorth());
        metrics.setMonthlyChange(monthlyChange);

        // 计算月度变化率
        if (previousMonthSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal monthlyChangeRate = monthlyChange
                .divide(previousMonthSummary.getNetWorth(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setMonthlyChangeRate(monthlyChangeRate);
        } else {
            metrics.setMonthlyChangeRate(BigDecimal.ZERO);
        }

        // 年度变化
        metrics.setPreviousYearDate(lastYearEndDate);
        metrics.setPreviousYearNetWorth(lastYearSummary.getNetWorth());
        BigDecimal yearlyChange = currentSummary.getNetWorth().subtract(lastYearSummary.getNetWorth());
        metrics.setYearlyChange(yearlyChange);

        // 计算年度变化率
        if (lastYearSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal yearlyChangeRate = yearlyChange
                .divide(lastYearSummary.getNetWorth(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setYearlyChangeRate(yearlyChangeRate);
        } else {
            metrics.setYearlyChangeRate(BigDecimal.ZERO);
        }

        return metrics;
    }

    // 获取风险评估
    public RiskAssessmentDTO getRiskAssessment(Long userId, Long familyId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        RiskAssessmentDTO assessment = new RiskAssessmentDTO();
        assessment.setAsOfDate(targetDate);

        // 1. 获取基础财务数据
        AssetSummaryDTO summary = assetAnalysisService.getAssetSummary(userId, familyId, targetDate);
        summary = addLiabilityDataToSummary(summary, userId, familyId, targetDate, "All");
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
    public OptimizationRecommendationDTO getOptimizationRecommendations(Long userId, Long familyId, LocalDate asOfDate) {
        // 如果没有指定日期,使用当前日期
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        // 获取基础数据
        AssetSummaryDTO summary = assetAnalysisService.getAssetSummary(userId, familyId, targetDate);
        summary = addLiabilityDataToSummary(summary, userId, familyId, targetDate, "All");
        RiskAssessmentDTO riskAssessment = getRiskAssessment(userId, familyId, targetDate);
        FinancialMetricsDTO metrics = getFinancialMetrics(userId, familyId, targetDate);

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
            generateTaxOptimization(userId, familyId, targetDate)
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
    private OptimizationRecommendationDTO.TaxOptimization generateTaxOptimization(Long userId, Long familyId, LocalDate targetDate) {
        OptimizationRecommendationDTO.TaxOptimization optimization =
            new OptimizationRecommendationDTO.TaxOptimization();

        // 获取按税收状态的净资产配置
        Map<String, Object> netWorthByTax = getNetWorthByTaxStatus(userId, familyId, targetDate);
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

    // ==================== 增强的财务指标计算方法 ====================

    /**
     * 获取增强的财务指标
     * 整合资产、负债、收入、支出、投资等全维度数据
     */
    public EnhancedFinancialMetricsDTO getEnhancedFinancialMetrics(Long userId, Long familyId, LocalDate asOfDate) {
        LocalDate targetDate = (asOfDate != null) ? asOfDate : LocalDate.now();
        Integer currentYear = targetDate.getYear();

        EnhancedFinancialMetricsDTO metrics = new EnhancedFinancialMetricsDTO();
        metrics.setAsOfDate(targetDate);
        metrics.setYear(currentYear);

        // 1. 获取基础资产负债数据(复用现有方法)
        AssetSummaryDTO currentSummary = assetAnalysisService.getAssetSummary(userId, familyId, targetDate);
        currentSummary = addLiabilityDataToSummary(currentSummary, userId, familyId, targetDate, "All");
        metrics.setTotalAssets(currentSummary.getTotalAssets());
        metrics.setTotalLiabilities(currentSummary.getTotalLiabilities());
        metrics.setNetWorth(currentSummary.getNetWorth());

        // 2. 计算资产负债率和流动性比率
        if (currentSummary.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = currentSummary.getTotalLiabilities()
                .divide(currentSummary.getTotalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setDebtToAssetRatio(debtRatio);
        } else {
            metrics.setDebtToAssetRatio(BigDecimal.ZERO);
        }

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

        // 3. 计算月度和年度变化
        calculateAssetChanges(metrics, userId, familyId, targetDate);

        // 4. 计算现金流指标
        calculateCashFlowMetrics(metrics, familyId, currentYear);

        // 5. 计算投资收益指标
        calculateInvestmentMetrics(metrics, familyId, currentYear);

        // 6. 计算财务健康评分
        EnhancedFinancialMetricsDTO.HealthScoreDTO healthScore = calculateHealthScore(metrics);
        metrics.setHealthScore(healthScore);

        return metrics;
    }

    /**
     * 计算资产变化(月度和年度)
     */
    private void calculateAssetChanges(EnhancedFinancialMetricsDTO metrics, Long userId, Long familyId, LocalDate targetDate) {
        // 月度变化
        LocalDate previousMonth = targetDate.minusMonths(1);
        metrics.setPreviousMonthDate(previousMonth);
        AssetSummaryDTO previousMonthSummary = assetAnalysisService.getAssetSummary(userId, familyId, previousMonth);
        previousMonthSummary = addLiabilityDataToSummary(previousMonthSummary, userId, familyId, previousMonth, "All");
        metrics.setPreviousMonthNetWorth(previousMonthSummary.getNetWorth());
        BigDecimal monthlyChange = metrics.getNetWorth().subtract(previousMonthSummary.getNetWorth());
        metrics.setMonthlyChange(monthlyChange);

        if (previousMonthSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal monthlyChangeRate = monthlyChange
                .divide(previousMonthSummary.getNetWorth(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setMonthlyChangeRate(monthlyChangeRate);
        } else {
            metrics.setMonthlyChangeRate(BigDecimal.ZERO);
        }

        // 年度变化
        LocalDate previousYear = LocalDate.of(targetDate.getYear() - 1, 12, 31);
        metrics.setPreviousYearDate(previousYear);
        AssetSummaryDTO previousYearSummary = assetAnalysisService.getAssetSummary(userId, familyId, previousYear);
        previousYearSummary = addLiabilityDataToSummary(previousYearSummary, userId, familyId, previousYear, "All");
        metrics.setPreviousYearNetWorth(previousYearSummary.getNetWorth());
        BigDecimal yearlyChange = metrics.getNetWorth().subtract(previousYearSummary.getNetWorth());
        metrics.setYearlyChange(yearlyChange);

        if (previousYearSummary.getNetWorth().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal yearlyChangeRate = yearlyChange
                .divide(previousYearSummary.getNetWorth(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setYearlyChangeRate(yearlyChangeRate);
        } else {
            metrics.setYearlyChangeRate(BigDecimal.ZERO);
        }
    }

    /**
     * 计算现金流指标
     */
    private void calculateCashFlowMetrics(EnhancedFinancialMetricsDTO metrics, Long familyId, Integer year) {
        try {
            // 1. 获取年度收入汇总
            List<com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO> incomeSummary =
                incomeAnalysisService.getAnnualMajorCategorySummary(familyId, year, "USD");

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal workIncome = BigDecimal.ZERO;
            BigDecimal investmentIncome = BigDecimal.ZERO;

            for (com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO category : incomeSummary) {
                totalIncome = totalIncome.add(category.getTotalAmount());

                // Salary大类 (ID=1)
                if ("Salary".equals(category.getMajorCategoryName())) {
                    workIncome = category.getTotalAmount();
                }
                // Investment大类 (ID=3)
                else if ("Investment".equals(category.getMajorCategoryName())) {
                    investmentIncome = category.getTotalAmount();
                }
            }

            metrics.setAnnualTotalIncome(totalIncome);
            metrics.setAnnualWorkIncome(workIncome);
            metrics.setAnnualInvestmentIncome(investmentIncome);
            metrics.setAnnualOtherIncome(totalIncome.subtract(workIncome).subtract(investmentIncome));

        } catch (Exception e) {
            metrics.setAnnualTotalIncome(BigDecimal.ZERO);
            metrics.setAnnualWorkIncome(BigDecimal.ZERO);
            metrics.setAnnualInvestmentIncome(BigDecimal.ZERO);
            metrics.setAnnualOtherIncome(BigDecimal.ZERO);
        }

        try {
            // 2. 获取年度支出汇总
            List<com.finance.app.dto.expense.AnnualExpenseSummaryDTO> expenseSummary =
                expenseAnalysisService.getAnnualExpenseSummaryWithAdjustments(familyId, year, "USD", true);

            // 查找总计行 (majorCategoryId == 0)
            com.finance.app.dto.expense.AnnualExpenseSummaryDTO totalRow = expenseSummary.stream()
                .filter(item -> item.getMajorCategoryId() != null && item.getMajorCategoryId() == 0L)
                .findFirst()
                .orElse(null);

            if (totalRow != null && totalRow.getActualExpenseAmount() != null) {
                metrics.setAnnualTotalExpense(totalRow.getActualExpenseAmount());
            } else {
                metrics.setAnnualTotalExpense(BigDecimal.ZERO);
            }

        } catch (Exception e) {
            metrics.setAnnualTotalExpense(BigDecimal.ZERO);
        }

        // 3. 计算净现金流和储蓄率
        BigDecimal netCashFlow = metrics.getAnnualTotalIncome().subtract(metrics.getAnnualTotalExpense());
        metrics.setNetCashFlow(netCashFlow);

        if (metrics.getAnnualTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingsRate = netCashFlow
                .divide(metrics.getAnnualTotalIncome(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setSavingsRate(savingsRate);

            BigDecimal expenseRatio = metrics.getAnnualTotalExpense()
                .divide(metrics.getAnnualTotalIncome(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            metrics.setExpenseRatio(expenseRatio);
        } else {
            metrics.setSavingsRate(BigDecimal.ZERO);
            metrics.setExpenseRatio(BigDecimal.ZERO);
        }

        // 4. 计算去年同期数据和增长率
        try {
            List<com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO> lastYearIncome =
                incomeAnalysisService.getAnnualMajorCategorySummary(familyId, year - 1, "USD");

            BigDecimal lastYearTotalIncome = lastYearIncome.stream()
                .map(com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            metrics.setLastYearTotalIncome(lastYearTotalIncome);

            if (lastYearTotalIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal incomeGrowth = metrics.getAnnualTotalIncome()
                    .subtract(lastYearTotalIncome)
                    .divide(lastYearTotalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                metrics.setIncomeGrowthRate(incomeGrowth);
            }

        } catch (Exception e) {
            metrics.setLastYearTotalIncome(BigDecimal.ZERO);
            metrics.setIncomeGrowthRate(BigDecimal.ZERO);
        }

        try {
            List<com.finance.app.dto.expense.AnnualExpenseSummaryDTO> lastYearExpense =
                expenseAnalysisService.getAnnualExpenseSummaryWithAdjustments(familyId, year - 1, "USD", true);

            com.finance.app.dto.expense.AnnualExpenseSummaryDTO lastYearTotal = lastYearExpense.stream()
                .filter(item -> item.getMajorCategoryId() != null && item.getMajorCategoryId() == 0L)
                .findFirst()
                .orElse(null);

            if (lastYearTotal != null && lastYearTotal.getActualExpenseAmount() != null) {
                metrics.setLastYearTotalExpense(lastYearTotal.getActualExpenseAmount());

                if (lastYearTotal.getActualExpenseAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal expenseGrowth = metrics.getAnnualTotalExpense()
                        .subtract(lastYearTotal.getActualExpenseAmount())
                        .divide(lastYearTotal.getActualExpenseAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    metrics.setExpenseGrowthRate(expenseGrowth);
                }
            }

        } catch (Exception e) {
            metrics.setLastYearTotalExpense(BigDecimal.ZERO);
            metrics.setExpenseGrowthRate(BigDecimal.ZERO);
        }
    }

    /**
     * 计算投资收益指标
     */
    private void calculateInvestmentMetrics(EnhancedFinancialMetricsDTO metrics, Long familyId, Integer year) {
        try {
            // 1. 获取年度投资大类分析
            List<com.finance.app.dto.InvestmentCategoryAnalysisDTO> categoryAnalysis =
                investmentAnalysisService.getAnnualByCategory(familyId, year, "USD");

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal currentValue = BigDecimal.ZERO;
            BigDecimal totalReturns = BigDecimal.ZERO;

            for (com.finance.app.dto.InvestmentCategoryAnalysisDTO category : categoryAnalysis) {
                // netDeposits = 累计投入, currentAssets = 当前市值, returns = 投资回报
                totalInvested = totalInvested.add(category.getNetDeposits() != null ? category.getNetDeposits() : BigDecimal.ZERO);
                currentValue = currentValue.add(category.getCurrentAssets() != null ? category.getCurrentAssets() : BigDecimal.ZERO);
                totalReturns = totalReturns.add(category.getReturns() != null ? category.getReturns() : BigDecimal.ZERO);
            }

            metrics.setTotalInvested(totalInvested);
            metrics.setCurrentInvestmentValue(currentValue);
            metrics.setTotalInvestmentReturn(totalReturns);

            // 计算收益率
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal returnRate = totalReturns
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                metrics.setInvestmentReturnRate(returnRate);
            } else {
                metrics.setInvestmentReturnRate(BigDecimal.ZERO);
            }

            // 2. 提取表现最好的前3个大类
            List<EnhancedFinancialMetricsDTO.TopInvestmentCategory> topCategories = categoryAnalysis.stream()
                .filter(cat -> cat.getReturnRate() != null)
                .sorted((a, b) -> b.getReturnRate().compareTo(a.getReturnRate()))
                .limit(3)
                .map(cat -> {
                    EnhancedFinancialMetricsDTO.TopInvestmentCategory top = new EnhancedFinancialMetricsDTO.TopInvestmentCategory();
                    top.setCategoryName(cat.getCategoryName());
                    top.setValue(cat.getCurrentAssets());
                    top.setReturnRate(cat.getReturnRate());
                    return top;
                })
                .collect(Collectors.toList());

            metrics.setTopCategories(topCategories);

        } catch (Exception e) {
            metrics.setTotalInvested(BigDecimal.ZERO);
            metrics.setCurrentInvestmentValue(BigDecimal.ZERO);
            metrics.setTotalInvestmentReturn(BigDecimal.ZERO);
            metrics.setInvestmentReturnRate(BigDecimal.ZERO);
            metrics.setTopCategories(new ArrayList<>());
        }
    }

    /**
     * 计算财务健康评分
     */
    private EnhancedFinancialMetricsDTO.HealthScoreDTO calculateHealthScore(EnhancedFinancialMetricsDTO metrics) {
        EnhancedFinancialMetricsDTO.HealthScoreDTO healthScore = new EnhancedFinancialMetricsDTO.HealthScoreDTO();
        EnhancedFinancialMetricsDTO.HealthScoreDTO.ScoreBreakdown scores = new EnhancedFinancialMetricsDTO.HealthScoreDTO.ScoreBreakdown();
        List<String> recommendations = new ArrayList<>();

        // 1. 资产负债管理 (0-25分)
        BigDecimal debtRatio = metrics.getDebtToAssetRatio();
        if (debtRatio.compareTo(new BigDecimal("30")) < 0) {
            scores.setDebtManagement(new BigDecimal("25"));
        } else if (debtRatio.compareTo(new BigDecimal("50")) < 0) {
            scores.setDebtManagement(new BigDecimal("20"));
        } else if (debtRatio.compareTo(new BigDecimal("70")) < 0) {
            scores.setDebtManagement(new BigDecimal("15"));
            recommendations.add("资产负债率偏高，建议加快债务偿还");
        } else {
            scores.setDebtManagement(new BigDecimal("10"));
            recommendations.add("资产负债率过高,需优先处理债务问题");
        }

        // 2. 流动性管理 (0-20分)
        BigDecimal liquidityRatio = metrics.getLiquidityRatio();
        if (liquidityRatio.compareTo(new BigDecimal("20")) >= 0) {
            scores.setLiquidity(new BigDecimal("20"));
        } else if (liquidityRatio.compareTo(new BigDecimal("15")) >= 0) {
            scores.setLiquidity(new BigDecimal("16"));
        } else if (liquidityRatio.compareTo(new BigDecimal("10")) >= 0) {
            scores.setLiquidity(new BigDecimal("12"));
            recommendations.add("流动性比率" + liquidityRatio.setScale(1, RoundingMode.HALF_UP) + "%，建议提升至20%以上");
        } else {
            scores.setLiquidity(new BigDecimal("8"));
            recommendations.add("流动性不足，建议增加应急资金储备");
        }

        // 3. 储蓄能力 (0-25分)
        BigDecimal savingsRate = metrics.getSavingsRate();
        if (savingsRate.compareTo(new BigDecimal("30")) > 0) {
            scores.setSavings(new BigDecimal("25"));
        } else if (savingsRate.compareTo(new BigDecimal("20")) >= 0) {
            scores.setSavings(new BigDecimal("20"));
        } else if (savingsRate.compareTo(new BigDecimal("10")) >= 0) {
            scores.setSavings(new BigDecimal("15"));
            recommendations.add("储蓄率偏低，建议提升至20%以上");
        } else {
            scores.setSavings(new BigDecimal("10"));
            recommendations.add("储蓄率过低，建议控制支出并增加储蓄");
        }

        // 4. 投资收益 (0-20分)
        BigDecimal investmentReturn = metrics.getInvestmentReturnRate();
        if (investmentReturn.compareTo(new BigDecimal("15")) > 0) {
            scores.setInvestment(new BigDecimal("20"));
        } else if (investmentReturn.compareTo(new BigDecimal("10")) >= 0) {
            scores.setInvestment(new BigDecimal("16"));
        } else if (investmentReturn.compareTo(new BigDecimal("5")) >= 0) {
            scores.setInvestment(new BigDecimal("12"));
            recommendations.add("投资收益率" + investmentReturn.setScale(1, RoundingMode.HALF_UP) + "%，考虑优化投资组合");
        } else {
            scores.setInvestment(new BigDecimal("8"));
            recommendations.add("投资收益率偏低，建议重新评估投资策略");
        }

        // 5. 资产增长 (0-10分)
        BigDecimal growthRate = metrics.getYearlyChangeRate();
        if (growthRate.compareTo(new BigDecimal("15")) > 0) {
            scores.setGrowth(new BigDecimal("10"));
        } else if (growthRate.compareTo(new BigDecimal("10")) >= 0) {
            scores.setGrowth(new BigDecimal("8"));
        } else if (growthRate.compareTo(new BigDecimal("5")) >= 0) {
            scores.setGrowth(new BigDecimal("6"));
        } else {
            scores.setGrowth(new BigDecimal("4"));
            recommendations.add("资产增长缓慢，建议优化资产配置");
        }

        // 计算总分
        BigDecimal totalScore = scores.getDebtManagement()
            .add(scores.getLiquidity())
            .add(scores.getSavings())
            .add(scores.getInvestment())
            .add(scores.getGrowth());

        healthScore.setTotalScore(totalScore);
        healthScore.setScores(scores);

        // 确定等级
        if (totalScore.compareTo(new BigDecimal("90")) >= 0) {
            healthScore.setGrade("A+");
        } else if (totalScore.compareTo(new BigDecimal("80")) >= 0) {
            healthScore.setGrade("A");
        } else if (totalScore.compareTo(new BigDecimal("70")) >= 0) {
            healthScore.setGrade("B");
        } else if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
            healthScore.setGrade("C");
        } else {
            healthScore.setGrade("D");
        }

        healthScore.setRecommendations(recommendations);

        return healthScore;
    }
}
