package com.finance.app.service;

import com.finance.app.dto.RunwayAnalysisDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.ExpenseCategoryMajor;
import com.finance.app.model.ExpenseRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.ExpenseRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunwayService {

    private static final List<String> DEFAULT_LIQUID_TYPES = List.of(
            "CASH", "STOCKS", "CRYPTOCURRENCY", "PRECIOUS_METALS"
    );
    private static final int STALE_THRESHOLD_DAYS = 30;
    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AssetAccountRepository assetAccountRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final ExpenseCategoryMajorRepository expenseCategoryMajorRepository;
    private final ExchangeRateService exchangeRateService;

    public RunwayAnalysisDTO calculateRunway(Long familyId, List<String> includedTypes, int months) {
        List<String> types = (includedTypes == null || includedTypes.isEmpty())
                ? DEFAULT_LIQUID_TYPES : includedTypes;

        LiquidAssetsResult liquidResult = getLiquidAssets(familyId, types);
        MonthlyBurnResult burnResult = getMonthlyBurn(familyId, months);
        Map<String, BigDecimal> expenseBreakdown = getExpenseBreakdown(familyId, months);

        Integer runwayMonths = null;
        String depletionDate = null;
        BigDecimal burn = burnResult.monthlyBurn();
        if (burn != null && burn.compareTo(BigDecimal.ZERO) > 0
                && liquidResult.liquidTotal().compareTo(BigDecimal.ZERO) > 0) {
            runwayMonths = liquidResult.liquidTotal()
                    .divide(burn, 0, RoundingMode.FLOOR).intValue();
            depletionDate = YearMonth.now().plusMonths(runwayMonths).format(PERIOD_FMT);
        }

        return new RunwayAnalysisDTO(
                liquidResult.liquidTotal(),
                burn != null ? burn : BigDecimal.ZERO,
                runwayMonths,
                depletionDate,
                burnResult.monthsUsed(),
                liquidResult.assetDataMissing(),
                burnResult.expenseDataWarning(),
                liquidResult.latestSnapshotDate(),
                liquidResult.accountBreakdown(),
                expenseBreakdown
        );
    }

    private LiquidAssetsResult getLiquidAssets(Long familyId, List<String> includedTypes) {
        List<AssetAccount> liquidAccounts =
                assetAccountRepository.findByFamilyIdAndAssetTypeCodeIn(familyId, includedTypes);

        if (liquidAccounts.isEmpty()) {
            log.warn("家庭 {} 没有符合条件的流动资产账户，类型: {}", familyId, includedTypes);
            return new LiquidAssetsResult(BigDecimal.ZERO, true, null, List.of());
        }

        List<Long> accountIds = liquidAccounts.stream()
                .map(AssetAccount::getId)
                .collect(Collectors.toList());

        List<AssetRecord> latestRecords = assetRecordRepository.findLatestByAccountIds(accountIds);

        if (latestRecords.isEmpty()) {
            return new LiquidAssetsResult(BigDecimal.ZERO, true, null, List.of());
        }

        Map<Long, AssetRecord> latestByAccount = latestRecords.stream()
                .collect(Collectors.toMap(AssetRecord::getAccountId, r -> r, (a, b) -> a));

        BigDecimal total = BigDecimal.ZERO;
        LocalDate latestDate = null;
        List<RunwayAnalysisDTO.AccountInfo> breakdown = new ArrayList<>();

        for (AssetAccount account : liquidAccounts) {
            AssetRecord record = latestByAccount.get(account.getId());
            if (record == null) continue;

            BigDecimal usdValue = toUsd(record.getAmount(), record.getCurrency(), record.getRecordDate());
            total = total.add(usdValue);

            if (latestDate == null || record.getRecordDate().isAfter(latestDate)) {
                latestDate = record.getRecordDate();
            }

            String typeCode = account.getAssetType() != null
                    ? account.getAssetType().getType() : "UNKNOWN";
            breakdown.add(new RunwayAnalysisDTO.AccountInfo(
                    account.getId(), account.getAccountName(), typeCode, usdValue
            ));
        }

        boolean stale = latestDate == null
                || latestDate.isBefore(LocalDate.now().minusDays(STALE_THRESHOLD_DAYS));

        return new LiquidAssetsResult(total, stale, latestDate, breakdown);
    }

    private MonthlyBurnResult getMonthlyBurn(Long familyId, int months) {
        YearMonth endMonth = YearMonth.now().minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(months - 1);

        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndPeriodRange(
                familyId, startMonth.format(PERIOD_FMT), endMonth.format(PERIOD_FMT));

        if (records.isEmpty()) {
            return new MonthlyBurnResult(null, 0, true);
        }

        Map<String, BigDecimal> byPeriod = new TreeMap<>();
        for (ExpenseRecord record : records) {
            LocalDate rateDate = periodToDate(record.getExpensePeriod());
            BigDecimal usdAmount = toUsd(record.getAmount(), record.getCurrency(), rateDate);
            byPeriod.merge(record.getExpensePeriod(), usdAmount, BigDecimal::add);
        }

        int monthsUsed = byPeriod.size();
        BigDecimal totalExpense = byPeriod.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyBurn = totalExpense.divide(
                BigDecimal.valueOf(monthsUsed), 2, RoundingMode.HALF_UP);

        return new MonthlyBurnResult(monthlyBurn, monthsUsed, monthsUsed < 3);
    }

    private Map<String, BigDecimal> getExpenseBreakdown(Long familyId, int months) {
        YearMonth endMonth = YearMonth.now().minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(months - 1);

        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndPeriodRange(
                familyId, startMonth.format(PERIOD_FMT), endMonth.format(PERIOD_FMT));

        if (records.isEmpty()) return Map.of();

        List<ExpenseCategoryMajor> categories = expenseCategoryMajorRepository.findAll();
        Map<Long, String> categoryCodeMap = categories.stream()
                .collect(Collectors.toMap(ExpenseCategoryMajor::getId, ExpenseCategoryMajor::getCode));

        long monthsUsed = records.stream()
                .map(ExpenseRecord::getExpensePeriod).distinct().count();
        if (monthsUsed == 0) return Map.of();

        Map<Long, BigDecimal> byCategory = new HashMap<>();
        for (ExpenseRecord record : records) {
            LocalDate rateDate = periodToDate(record.getExpensePeriod());
            BigDecimal usdAmount = toUsd(record.getAmount(), record.getCurrency(), rateDate);
            byCategory.merge(record.getMajorCategoryId(), usdAmount, BigDecimal::add);
        }

        Map<String, BigDecimal> result = new TreeMap<>();
        byCategory.forEach((catId, total) -> {
            String code = categoryCodeMap.getOrDefault(catId, "OTHER");
            BigDecimal avgMonthly = total.divide(
                    BigDecimal.valueOf(monthsUsed), 2, RoundingMode.HALF_UP);
            result.merge(code, avgMonthly, BigDecimal::add);
        });

        return result;
    }

    /** Convert "yyyy-MM" period string to last day of that month for rate lookup */
    private LocalDate periodToDate(String period) {
        try {
            return YearMonth.parse(period, PERIOD_FMT).atEndOfMonth();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private BigDecimal toUsd(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null) return BigDecimal.ZERO;
        if (currency == null || "USD".equalsIgnoreCase(currency)) return amount;
        BigDecimal rate = exchangeRateService.getExchangeRate(currency, date);
        return amount.multiply(rate);
    }

    private record LiquidAssetsResult(
            BigDecimal liquidTotal,
            boolean assetDataMissing,
            LocalDate latestSnapshotDate,
            List<RunwayAnalysisDTO.AccountInfo> accountBreakdown
    ) {}

    private record MonthlyBurnResult(
            BigDecimal monthlyBurn,
            int monthsUsed,
            boolean expenseDataWarning
    ) {}
}
