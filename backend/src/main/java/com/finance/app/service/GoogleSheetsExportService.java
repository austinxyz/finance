package com.finance.app.service;

import com.finance.app.model.*;
import com.finance.app.repository.*;
import com.google.api.services.sheets.v4.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Sheets年度报表导出服务
 * 复用ExcelExportService的数据逻辑，导出到Google Sheets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsExportService {

    private final GoogleSheetsService googleSheetsService;
    private final GoogleSheetsSyncRepository googleSheetsSyncRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final ExpenseBudgetRepository expenseBudgetRepository;
    private final ExpenseCategoryMajorRepository expenseCategoryMajorRepository;
    private final ExpenseCategoryMinorRepository expenseCategoryMinorRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final LiabilityTypeRepository liabilityTypeRepository;
    private final ExchangeRateService exchangeRateService;
    private final UserRepository userRepository;
    private final SseEmitterManager sseEmitterManager;
    private final ApplicationContext applicationContext;
    private final AnalysisService analysisService;

    private static final String RETIREMENT_FUND_TYPE = "RETIREMENT_FUND";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 汇率缓存 ThreadLocal（每个导出任务独立）
    private static final ThreadLocal<Map<String, BigDecimal>> EXCHANGE_RATE_CACHE = ThreadLocal.withInitial(HashMap::new);

    /**
     * 创建或更新年度财务报表Google Sheets（同步方法，立即启动异步任务）
     * @param familyId 家庭ID
     * @param year 年份
     * @param permissionRole 权限：reader或writer
     * @return Map包含任务ID和状态 {syncId, status}
     */
    public Map<String, Object> createOrUpdateAnnualReport(Long familyId, Integer year, String permissionRole) {
        log.info("开始创建Google Sheets年度报表任务: familyId={}, year={}", familyId, year);

        // 查询是否已存在同步记录
        Optional<GoogleSheetsSync> existingSync = googleSheetsSyncRepository.findByFamilyIdAndYear(familyId, year);

        GoogleSheetsSync sync;
        boolean isNew;

        if (existingSync.isPresent()) {
            sync = existingSync.get();
            isNew = false;

            // 检查是否有正在进行的任务
            if ("IN_PROGRESS".equals(sync.getStatus()) || "PENDING".equals(sync.getStatus())) {
                log.info("已有进行中的任务: syncId={}", sync.getId());
                Map<String, Object> result = new HashMap<>();
                result.put("syncId", sync.getId());
                result.put("status", sync.getStatus());
                result.put("progress", sync.getProgress());
                result.put("message", "已有正在进行的同步任务");
                return result;
            }

            // 重置状态以开始新的同步
            sync.setStatus("PENDING");
            sync.setProgress(0);
            sync.setErrorMessage(null);
            sync.setPermission(permissionRole);
            googleSheetsSyncRepository.save(sync);

            log.info("重新启动同步任务: syncId={}, spreadsheetId={}", sync.getId(), sync.getSpreadsheetId());

        } else {
            // 创建新的同步记录
            sync = new GoogleSheetsSync();
            sync.setFamilyId(familyId);
            sync.setYear(year);
            sync.setSpreadsheetId(""); // 稍后在异步任务中设置
            sync.setShareUrl(""); // 稍后在异步任务中设置
            sync.setPermission(permissionRole);
            sync.setStatus("PENDING");
            sync.setProgress(0);
            sync = googleSheetsSyncRepository.save(sync);
            isNew = true;

            log.info("创建新的同步任务记录: syncId={}", sync.getId());
        }

        // 异步执行导出任务（通过Spring代理调用以启用@Async）
        // 通过ApplicationContext获取代理对象来触发@Async
        applicationContext.getBean(GoogleSheetsExportService.class)
            .executeAsyncExport(sync.getId(), familyId, year, permissionRole, isNew);

        Map<String, Object> result = new HashMap<>();
        result.put("syncId", sync.getId());
        result.put("status", "PENDING");
        result.put("progress", 0);
        result.put("message", "报表生成任务已启动，请稍后查询状态");

        return result;
    }

    /**
     * 异步执行报表导出
     */
    @Async("googleSheetsExecutor")
    @Transactional
    public void executeAsyncExport(Long syncId, Long familyId, Integer year, String permissionRole, boolean isNew) {
        log.info("开始异步执行报表导出: syncId={}, familyId={}, year={}", syncId, familyId, year);

        // 等待500ms，确保客户端有时间建立SSE连接
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待SSE连接时被中断", e);
        }

        GoogleSheetsSync sync = googleSheetsSyncRepository.findById(syncId)
            .orElseThrow(() -> new RuntimeException("同步记录不存在: " + syncId));

        try {
            // 更新状态为进行中
            sync.setStatus("IN_PROGRESS");
            googleSheetsSyncRepository.save(sync);

            // 发送初始进度
            updateProgress(syncId, 5, "正在启动任务...");

            // 预加载汇率数据（优化性能）
            preloadExchangeRates(year);

            String spreadsheetId;
            String shareUrl;

            if (isNew) {
                // 创建新的电子表格
                String title = year + "年家庭财务报表";
                spreadsheetId = googleSheetsService.createSpreadsheet(title);
                sync.setSpreadsheetId(spreadsheetId);
                googleSheetsSyncRepository.save(sync);
                updateProgress(syncId, 10, "正在创建电子表格...");

                log.info("创建新的报表: {}", spreadsheetId);

                // 导出各个Sheet（每个Sheet更新进度）
                exportBalanceSheet(spreadsheetId, familyId, year);
                updateProgress(syncId, 25, "正在导出资产负债表...");

                exportBalanceSheetDetail(spreadsheetId, familyId, year);
                updateProgress(syncId, 35, "正在导出资产负债表明细...");

                exportExpenseSheet(spreadsheetId, familyId, year, "USD");
                updateProgress(syncId, 50, "正在导出USD开支表...");

                exportExpenseSheet(spreadsheetId, familyId, year, "CNY");
                updateProgress(syncId, 65, "正在导出CNY开支表...");

                exportInvestmentAccountSheet(spreadsheetId, familyId, year);
                updateProgress(syncId, 80, "正在导出投资账户明细...");

                exportRetirementAccountSheet(spreadsheetId, familyId, year);
                updateProgress(syncId, 90, "正在导出退休账户明细...");

                // 删除默认的"Sheet1"
                deleteDefaultSheet(spreadsheetId);

                // 设置权限
                shareUrl = googleSheetsService.setPermissions(spreadsheetId, permissionRole);
                sync.setShareUrl(shareUrl);

            } else {
                // 更新已存在的电子表格
                spreadsheetId = sync.getSpreadsheetId();
                log.info("更新已存在的报表: {}", spreadsheetId);

                // 清空并重新导出所有Sheet（带进度更新）
                clearAndExportAllSheets(spreadsheetId, familyId, year, syncId);

                // 更新权限（如果需要）
                if (!permissionRole.equals(sync.getPermission())) {
                    shareUrl = googleSheetsService.setPermissions(spreadsheetId, permissionRole);
                    sync.setPermission(permissionRole);
                } else {
                    shareUrl = sync.getShareUrl();
                }
                sync.setShareUrl(shareUrl);
            }

            // 标记为完成
            sync.setStatus("COMPLETED");
            sync.setProgress(100);
            sync.setErrorMessage(null);
            googleSheetsSyncRepository.save(sync);

            // 通过SSE推送完成消息
            sseEmitterManager.sendSuccess(syncId, shareUrl, spreadsheetId);

            log.info("年度报表生成完成: syncId={}, shareUrl={}", syncId, shareUrl);

        } catch (Exception e) {
            log.error("报表生成失败: syncId={}", syncId, e);

            // 标记为失败
            sync.setStatus("FAILED");
            sync.setErrorMessage(e.getMessage());
            googleSheetsSyncRepository.save(sync);

            // 通过SSE推送错误消息
            sseEmitterManager.sendError(syncId, e.getMessage());
        } finally {
            // 清理汇率缓存
            clearExchangeRateCache();
        }
    }

    /**
     * 更新任务进度（同时推送到SSE）
     */
    private void updateProgress(Long syncId, int progress) {
        updateProgress(syncId, progress, null);
    }

    /**
     * 更新任务进度并指定消息
     */
    private void updateProgress(Long syncId, int progress, String message) {
        googleSheetsSyncRepository.findById(syncId).ifPresent(sync -> {
            sync.setProgress(progress);
            googleSheetsSyncRepository.save(sync);

            // 通过SSE推送进度更新
            String statusMessage = message != null ? message : getProgressMessage(progress);
            sseEmitterManager.sendProgress(syncId, progress, sync.getStatus(), statusMessage);
        });
    }

    /**
     * 根据进度百分比获取状态消息
     */
    private String getProgressMessage(int progress) {
        if (progress <= 10) return "正在创建电子表格...";
        if (progress <= 25) return "正在导出资产负债表...";
        if (progress <= 35) return "正在导出资产负债表明细...";
        if (progress <= 50) return "正在导出USD开支表...";
        if (progress <= 65) return "正在导出CNY开支表...";
        if (progress <= 80) return "正在导出投资账户明细...";
        if (progress <= 90) return "正在导出退休账户明细...";
        return "正在完成...";
    }

    /**
     * 清空并重新导出所有工作表
     */
    private void clearAndExportAllSheets(String spreadsheetId, Long familyId, Integer year, Long syncId)
            throws IOException, GeneralSecurityException {
        log.info("清空并重新导出所有工作表");

        // 清空并更新资产负债表
        googleSheetsService.clearSheet(spreadsheetId, "资产负债表");
        exportBalanceSheet(spreadsheetId, familyId, year);
        updateProgress(syncId, 25, "正在导出资产负债表...");

        // 清空并更新资产负债表明细
        googleSheetsService.clearSheet(spreadsheetId, "资产负债表明细");
        exportBalanceSheetDetail(spreadsheetId, familyId, year);
        updateProgress(syncId, 35, "正在导出资产负债表明细...");

        // 清空并更新开支表-USD
        googleSheetsService.clearSheet(spreadsheetId, "开支表-USD");
        exportExpenseSheet(spreadsheetId, familyId, year, "USD");
        updateProgress(syncId, 50, "正在导出USD开支表...");

        // 清空并更新开支表-CNY
        googleSheetsService.clearSheet(spreadsheetId, "开支表-CNY");
        exportExpenseSheet(spreadsheetId, familyId, year, "CNY");
        updateProgress(syncId, 65, "正在导出CNY开支表...");

        // 清空并更新投资账户明细
        googleSheetsService.clearSheet(spreadsheetId, "投资账户明细");
        exportInvestmentAccountSheet(spreadsheetId, familyId, year);
        updateProgress(syncId, 80, "正在导出投资账户明细...");

        // 清空并更新退休账户明细
        googleSheetsService.clearSheet(spreadsheetId, "退休账户明细");
        exportRetirementAccountSheet(spreadsheetId, familyId, year);
        updateProgress(syncId, 90, "正在导出退休账户明细...");

        log.info("所有工作表更新完成");
    }

    /**
     * 导出资产负债表
     */
    private void exportBalanceSheet(String spreadsheetId, Long familyId, Integer year)
            throws IOException, GeneralSecurityException {
        log.info("导出资产负债表");

        Integer sheetId = googleSheetsService.addSheet(spreadsheetId, "资产负债表");
        LocalDate asOfDate = LocalDate.of(year, 12, 31);
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        List<List<Object>> rows = new ArrayList<>();

        // 标题行
        rows.add(Arrays.asList(year + "年资产负债表"));
        rows.add(Arrays.asList()); // 空行

        // 按货币分组显示
        List<String> currencies = Arrays.asList("USD", "CNY");

        for (String currency : currencies) {
            // 货币标题
            rows.add(Arrays.asList(currency + " 资产负债"));

            // 表头
            rows.add(Arrays.asList("资产类型", "当前年值", "去年年底", "同比%", "", "负债类型", "当前年值", "去年年底", "同比%"));

            // 获取资产和负债数据
            Map<String, BigDecimal> assetTypeCurrentAmounts = new HashMap<>();
            Map<String, BigDecimal> assetTypeLastYearAmounts = new HashMap<>();
            BigDecimal currencyTotalAssetsCurrent = BigDecimal.ZERO;
            BigDecimal currencyTotalAssetsLastYear = BigDecimal.ZERO;

            List<AssetRecord> currentAssetRecords = assetRecordRepository
                .findLatestRecordsByFamilyAndDate(familyId, asOfDate).stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (AssetRecord record : currentAssetRecords) {
                String typeName = record.getAccount().getAssetType().getChineseName();
                assetTypeCurrentAmounts.merge(typeName, record.getAmount(), BigDecimal::add);
                currencyTotalAssetsCurrent = currencyTotalAssetsCurrent.add(record.getAmount());
            }

            List<AssetRecord> lastYearAssetRecords = assetRecordRepository
                .findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate).stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (AssetRecord record : lastYearAssetRecords) {
                String typeName = record.getAccount().getAssetType().getChineseName();
                assetTypeLastYearAmounts.merge(typeName, record.getAmount(), BigDecimal::add);
                currencyTotalAssetsLastYear = currencyTotalAssetsLastYear.add(record.getAmount());
            }

            Map<String, BigDecimal> liabilityTypeCurrentAmounts = new HashMap<>();
            Map<String, BigDecimal> liabilityTypeLastYearAmounts = new HashMap<>();
            BigDecimal currencyTotalLiabilitiesCurrent = BigDecimal.ZERO;
            BigDecimal currencyTotalLiabilitiesLastYear = BigDecimal.ZERO;

            List<LiabilityRecord> currentLiabilityRecords = liabilityRecordRepository
                .findLatestRecordsByFamilyAndDate(familyId, asOfDate).stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (LiabilityRecord record : currentLiabilityRecords) {
                String typeName = record.getAccount().getLiabilityType().getChineseName();
                liabilityTypeCurrentAmounts.merge(typeName, record.getOutstandingBalance(), BigDecimal::add);
                currencyTotalLiabilitiesCurrent = currencyTotalLiabilitiesCurrent.add(record.getOutstandingBalance());
            }

            List<LiabilityRecord> lastYearLiabilityRecords = liabilityRecordRepository
                .findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate).stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (LiabilityRecord record : lastYearLiabilityRecords) {
                String typeName = record.getAccount().getLiabilityType().getChineseName();
                liabilityTypeLastYearAmounts.merge(typeName, record.getOutstandingBalance(), BigDecimal::add);
                currencyTotalLiabilitiesLastYear = currencyTotalLiabilitiesLastYear.add(record.getOutstandingBalance());
            }

            // 合并类型并排
            Set<String> allAssetTypes = new HashSet<>();
            allAssetTypes.addAll(assetTypeCurrentAmounts.keySet());
            allAssetTypes.addAll(assetTypeLastYearAmounts.keySet());
            List<String> assetTypesList = new ArrayList<>(allAssetTypes);

            Set<String> allLiabilityTypes = new HashSet<>();
            allLiabilityTypes.addAll(liabilityTypeCurrentAmounts.keySet());
            allLiabilityTypes.addAll(liabilityTypeLastYearAmounts.keySet());
            List<String> liabilityTypesList = new ArrayList<>(allLiabilityTypes);

            int maxRows = Math.max(assetTypesList.size(), liabilityTypesList.size());
            for (int i = 0; i < maxRows; i++) {
                List<Object> row = new ArrayList<>();

                // 资产列
                if (i < assetTypesList.size()) {
                    String typeName = assetTypesList.get(i);
                    BigDecimal current = assetTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    BigDecimal lastYear = assetTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                        ? current.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                        : (current.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

                    row.add(typeName);
                    row.add(current.doubleValue());
                    row.add(lastYear.doubleValue());
                    row.add(changePct / 100); // 格式化为百分比
                } else {
                    row.add("");
                    row.add("");
                    row.add("");
                    row.add("");
                }

                row.add(""); // 分隔列

                // 负债列
                if (i < liabilityTypesList.size()) {
                    String typeName = liabilityTypesList.get(i);
                    BigDecimal current = liabilityTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    BigDecimal lastYear = liabilityTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                        ? current.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                        : (current.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

                    row.add(typeName);
                    row.add(current.doubleValue());
                    row.add(lastYear.doubleValue());
                    row.add(changePct / 100); // 格式化为百分比
                } else {
                    row.add("");
                    row.add("");
                    row.add("");
                    row.add("");
                }

                rows.add(row);
            }

            // 计算小计同比变化
            double assetChangePct = currencyTotalAssetsLastYear.compareTo(BigDecimal.ZERO) != 0
                ? currencyTotalAssetsCurrent.subtract(currencyTotalAssetsLastYear)
                    .divide(currencyTotalAssetsLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (currencyTotalAssetsCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

            double liabilityChangePct = currencyTotalLiabilitiesLastYear.compareTo(BigDecimal.ZERO) != 0
                ? currencyTotalLiabilitiesCurrent.subtract(currencyTotalLiabilitiesLastYear)
                    .divide(currencyTotalLiabilitiesLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (currencyTotalLiabilitiesCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

            // 小计行
            rows.add(Arrays.asList(
                currency + " 资产小计",
                currencyTotalAssetsCurrent.doubleValue(),
                currencyTotalAssetsLastYear.doubleValue(),
                assetChangePct / 100,
                "",
                currency + " 负债小计",
                currencyTotalLiabilitiesCurrent.doubleValue(),
                currencyTotalLiabilitiesLastYear.doubleValue(),
                liabilityChangePct / 100
            ));

            // 计算净资产同比变化
            BigDecimal currentNetWorth = currencyTotalAssetsCurrent.subtract(currencyTotalLiabilitiesCurrent);
            BigDecimal lastYearNetWorth = currencyTotalAssetsLastYear.subtract(currencyTotalLiabilitiesLastYear);
            double netWorthChangePct = lastYearNetWorth.compareTo(BigDecimal.ZERO) != 0
                ? currentNetWorth.subtract(lastYearNetWorth)
                    .divide(lastYearNetWorth, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (currentNetWorth.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

            // 净资产行
            rows.add(Arrays.asList(
                currency + " 净资产",
                currentNetWorth.doubleValue(),
                lastYearNetWorth.doubleValue(),
                netWorthChangePct / 100
            ));

            rows.add(Arrays.asList()); // 空行
            rows.add(Arrays.asList()); // 空行
        }

        // 计算USD总计
        List<AssetRecord> allCurrentAssetRecords = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate);
        List<LiabilityRecord> allCurrentLiabilityRecords = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate);
        List<AssetRecord> allLastYearAssetRecords = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate);
        List<LiabilityRecord> allLastYearLiabilityRecords = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate);

        BigDecimal totalAssetsCurrent = allCurrentAssetRecords.stream()
            .map(r -> convertToUSD(r.getAmount(), r.getCurrency(), asOfDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAssetsLastYear = allLastYearAssetRecords.stream()
            .map(r -> convertToUSD(r.getAmount(), r.getCurrency(), lastYearEndDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilitiesCurrent = allCurrentLiabilityRecords.stream()
            .map(r -> convertToUSD(r.getOutstandingBalance(), r.getCurrency(), asOfDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilitiesLastYear = allLastYearLiabilityRecords.stream()
            .map(r -> convertToUSD(r.getOutstandingBalance(), r.getCurrency(), lastYearEndDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算USD总计同比变化
        double totalAssetChangePct = totalAssetsLastYear.compareTo(BigDecimal.ZERO) != 0
            ? totalAssetsCurrent.subtract(totalAssetsLastYear)
                .divide(totalAssetsLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
            : (totalAssetsCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

        double totalLiabilityChangePct = totalLiabilitiesLastYear.compareTo(BigDecimal.ZERO) != 0
            ? totalLiabilitiesCurrent.subtract(totalLiabilitiesLastYear)
                .divide(totalLiabilitiesLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
            : (totalLiabilitiesCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

        BigDecimal totalNetWorthCurrent = totalAssetsCurrent.subtract(totalLiabilitiesCurrent);
        BigDecimal totalNetWorthLastYear = totalAssetsLastYear.subtract(totalLiabilitiesLastYear);
        double totalNetWorthChangePct = totalNetWorthLastYear.compareTo(BigDecimal.ZERO) != 0
            ? totalNetWorthCurrent.subtract(totalNetWorthLastYear)
                .divide(totalNetWorthLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
            : (totalNetWorthCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

        rows.add(Arrays.asList("折算为USD总计"));
        rows.add(Arrays.asList("资产类型", "当前年值", "去年年底", "同比%", "", "负债类型", "当前年值", "去年年底", "同比%")); // 表头

        // 计算USD总计各类型金额
        Map<String, BigDecimal> totalAssetTypeCurrentAmounts = new HashMap<>();
        Map<String, BigDecimal> totalAssetTypeLastYearAmounts = new HashMap<>();
        Map<String, BigDecimal> totalLiabilityTypeCurrentAmounts = new HashMap<>();
        Map<String, BigDecimal> totalLiabilityTypeLastYearAmounts = new HashMap<>();

        for (AssetRecord record : allCurrentAssetRecords) {
            String typeName = record.getAccount().getAssetType().getChineseName();
            BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), asOfDate);
            totalAssetTypeCurrentAmounts.merge(typeName, amountUSD, BigDecimal::add);
        }

        for (AssetRecord record : allLastYearAssetRecords) {
            String typeName = record.getAccount().getAssetType().getChineseName();
            BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), lastYearEndDate);
            totalAssetTypeLastYearAmounts.merge(typeName, amountUSD, BigDecimal::add);
        }

        for (LiabilityRecord record : allCurrentLiabilityRecords) {
            String typeName = record.getAccount().getLiabilityType().getChineseName();
            BigDecimal amountUSD = convertToUSD(record.getOutstandingBalance(), record.getCurrency(), asOfDate);
            totalLiabilityTypeCurrentAmounts.merge(typeName, amountUSD, BigDecimal::add);
        }

        for (LiabilityRecord record : allLastYearLiabilityRecords) {
            String typeName = record.getAccount().getLiabilityType().getChineseName();
            BigDecimal amountUSD = convertToUSD(record.getOutstandingBalance(), record.getCurrency(), lastYearEndDate);
            totalLiabilityTypeLastYearAmounts.merge(typeName, amountUSD, BigDecimal::add);
        }

        // 合并类型列表
        Set<String> allTotalAssetTypes = new HashSet<>();
        allTotalAssetTypes.addAll(totalAssetTypeCurrentAmounts.keySet());
        allTotalAssetTypes.addAll(totalAssetTypeLastYearAmounts.keySet());
        List<String> totalAssetTypesList = new ArrayList<>(allTotalAssetTypes);

        Set<String> allTotalLiabilityTypes = new HashSet<>();
        allTotalLiabilityTypes.addAll(totalLiabilityTypeCurrentAmounts.keySet());
        allTotalLiabilityTypes.addAll(totalLiabilityTypeLastYearAmounts.keySet());
        List<String> totalLiabilityTypesList = new ArrayList<>(allTotalLiabilityTypes);

        int maxTotalRows = Math.max(totalAssetTypesList.size(), totalLiabilityTypesList.size());
        for (int i = 0; i < maxTotalRows; i++) {
            List<Object> row = new ArrayList<>();

            // 资产列
            if (i < totalAssetTypesList.size()) {
                String typeName = totalAssetTypesList.get(i);
                BigDecimal current = totalAssetTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                BigDecimal lastYear = totalAssetTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                    ? current.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                    : (current.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

                row.add(typeName);
                row.add(current.doubleValue());
                row.add(lastYear.doubleValue());
                row.add(changePct / 100);
            } else {
                row.add("");
                row.add("");
                row.add("");
                row.add("");
            }

            row.add(""); // 分隔列

            // 负债列
            if (i < totalLiabilityTypesList.size()) {
                String typeName = totalLiabilityTypesList.get(i);
                BigDecimal current = totalLiabilityTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                BigDecimal lastYear = totalLiabilityTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                    ? current.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                    : (current.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

                row.add(typeName);
                row.add(current.doubleValue());
                row.add(lastYear.doubleValue());
                row.add(changePct / 100);
            } else {
                row.add("");
                row.add("");
                row.add("");
                row.add("");
            }

            rows.add(row);
        }

        // 总计行
        rows.add(Arrays.asList(
            "资产总计 (USD)",
            totalAssetsCurrent.doubleValue(),
            totalAssetsLastYear.doubleValue(),
            totalAssetChangePct / 100,
            "",
            "负债总计 (USD)",
            totalLiabilitiesCurrent.doubleValue(),
            totalLiabilitiesLastYear.doubleValue(),
            totalLiabilityChangePct / 100
        ));

        rows.add(Arrays.asList()); // 空行

        // 净资产部分：使用AnalysisService的净资产分类逻辑
        rows.add(Arrays.asList("净资产类别", "当前年值", "去年年底", "同比%"));

        // 获取当前年的净资产分类
        Map<String, Object> netAllocationCurrent = analysisService.getNetAssetAllocation(null, familyId, asOfDate, "All");
        List<Map<String, Object>> netCategoriesCurrent = (List<Map<String, Object>>) netAllocationCurrent.get("data");

        // 获取去年年底的净资产分类
        Map<String, Object> netAllocationLastYear = analysisService.getNetAssetAllocation(null, familyId, lastYearEndDate, "All");
        List<Map<String, Object>> netCategoriesLastYear = (List<Map<String, Object>>) netAllocationLastYear.get("data");

        // 构建去年数据的Map，方便查找
        Map<String, BigDecimal> lastYearNetValueByCategory = new HashMap<>();
        for (Map<String, Object> cat : netCategoriesLastYear) {
            String catName = (String) cat.get("name");
            BigDecimal netValue = new BigDecimal(cat.get("netValue").toString());
            lastYearNetValueByCategory.put(catName, netValue);
        }

        // 显示各净资产类别
        for (Map<String, Object> cat : netCategoriesCurrent) {
            String catName = (String) cat.get("name");
            BigDecimal netCurrent = new BigDecimal(cat.get("netValue").toString());
            BigDecimal netLastYear = lastYearNetValueByCategory.getOrDefault(catName, BigDecimal.ZERO);

            double netChangePct = netLastYear.compareTo(BigDecimal.ZERO) != 0
                ? netCurrent.subtract(netLastYear).divide(netLastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (netCurrent.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);

            rows.add(Arrays.asList(
                catName,
                netCurrent.doubleValue(),
                netLastYear.doubleValue(),
                netChangePct / 100
            ));
        }

        rows.add(Arrays.asList(
            "净资产总计 (USD)",
            totalNetWorthCurrent.doubleValue(),
            totalNetWorthLastYear.doubleValue(),
            totalNetWorthChangePct / 100
        ));

        // 写入数据
        googleSheetsService.writeData(spreadsheetId, "资产负债表", rows);

        // 应用格式化
        List<Request> formatRequests = new ArrayList<>();

        // 1. 为所有单元格添加边框（新增了同比%列，共9列）
        formatRequests.add(googleSheetsService.createBordersForAll(sheetId, rows.size(), 9));

        // 2. 合并并居中主标题行
        formatRequests.addAll(googleSheetsService.createMergeAndCenterFormat(sheetId, 0, 1, 0, 9));

        // 动态查找各个section的行号
        int usdTitleRow = -1;
        int usdHeaderRow = -1;
        int usdSubtotalRow = -1;
        int usdNetAssetRow = -1;
        int cnyTitleRow = -1;
        int cnyHeaderRow = -1;
        int cnySubtotalRow = -1;
        int cnyNetAssetRow = -1;
        int totalTitleRow = -1;
        int totalHeaderRow = -1;
        int totalAssetRow = -1;
        int netWorthHeaderRow = -1;
        int totalNetAssetRow = -1;

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty()) continue;
            String firstCell = row.get(0).toString();

            if ("USD 资产负债".equals(firstCell)) {
                usdTitleRow = i;
            } else if (usdTitleRow != -1 && usdHeaderRow == -1 && "资产类型".equals(firstCell)) {
                usdHeaderRow = i;
            } else if (firstCell.startsWith("USD 资产小计")) {
                usdSubtotalRow = i;
            } else if (firstCell.startsWith("USD 净资产")) {
                usdNetAssetRow = i;
            } else if ("CNY 资产负债".equals(firstCell)) {
                cnyTitleRow = i;
            } else if (cnyTitleRow != -1 && cnyHeaderRow == -1 && "资产类型".equals(firstCell)) {
                cnyHeaderRow = i;
            } else if (firstCell.startsWith("CNY 资产小计")) {
                cnySubtotalRow = i;
            } else if (firstCell.startsWith("CNY 净资产")) {
                cnyNetAssetRow = i;
            } else if ("折算为USD总计".equals(firstCell)) {
                totalTitleRow = i;
            } else if (totalTitleRow != -1 && totalHeaderRow == -1 && "资产类型".equals(firstCell)) {
                totalHeaderRow = i;
            } else if (firstCell.startsWith("资产总计")) {
                totalAssetRow = i;
            } else if ("净资产类别".equals(firstCell)) {
                netWorthHeaderRow = i;
            } else if (firstCell.startsWith("净资产总计")) {
                totalNetAssetRow = i;
            }
        }

        // USD部分格式化
        if (usdTitleRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, usdTitleRow, usdTitleRow + 1, 0, 9)); // USD标题
        }
        if (usdHeaderRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, usdHeaderRow, usdHeaderRow + 1, 0, 9)); // USD表头

            // 格式化USD数据行（从表头下一行到小计行之前）
            if (usdSubtotalRow != -1) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, usdHeaderRow + 1, usdSubtotalRow, 1, 3, "USD"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, usdHeaderRow + 1, usdSubtotalRow, 3, 4));
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, usdHeaderRow + 1, usdSubtotalRow, 6, 8, "USD"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, usdHeaderRow + 1, usdSubtotalRow, 8, 9));
            }
        }
        if (usdSubtotalRow != -1) {
            // USD小计行 - 不需要header格式化，只需要货币格式
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, usdSubtotalRow, usdSubtotalRow + 1, 1, 3, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, usdSubtotalRow, usdSubtotalRow + 1, 3, 4));
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, usdSubtotalRow, usdSubtotalRow + 1, 6, 8, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, usdSubtotalRow, usdSubtotalRow + 1, 8, 9));
        }
        if (usdNetAssetRow != -1) {
            // USD净资产行 - 不需要header格式化，只需要货币格式
            // 先清除第0列的背景色（标签列）
            formatRequests.add(googleSheetsService.createPlainFormat(sheetId, usdNetAssetRow, usdNetAssetRow + 1, 0, 1));
            // 然后应用货币和百分比格式
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, usdNetAssetRow, usdNetAssetRow + 1, 1, 3, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, usdNetAssetRow, usdNetAssetRow + 1, 3, 4));
        }

        // CNY部分格式化
        if (cnyTitleRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, cnyTitleRow, cnyTitleRow + 1, 0, 9)); // CNY标题
        }
        if (cnyHeaderRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, cnyHeaderRow, cnyHeaderRow + 1, 0, 9)); // CNY表头

            // 格式化CNY数据行（从表头下一行到小计行之前）
            if (cnySubtotalRow != -1) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, cnyHeaderRow + 1, cnySubtotalRow, 1, 3, "CNY"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, cnyHeaderRow + 1, cnySubtotalRow, 3, 4));
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, cnyHeaderRow + 1, cnySubtotalRow, 6, 8, "CNY"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, cnyHeaderRow + 1, cnySubtotalRow, 8, 9));
            }
        }
        if (cnySubtotalRow != -1) {
            // CNY小计行 - 不需要header格式化，只需要货币格式
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, cnySubtotalRow, cnySubtotalRow + 1, 1, 3, "CNY"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, cnySubtotalRow, cnySubtotalRow + 1, 3, 4));
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, cnySubtotalRow, cnySubtotalRow + 1, 6, 8, "CNY"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, cnySubtotalRow, cnySubtotalRow + 1, 8, 9));
        }
        if (cnyNetAssetRow != -1) {
            // CNY净资产行 - 不需要header格式化，只需要货币格式
            // 先清除第0列的背景色（标签列）
            formatRequests.add(googleSheetsService.createPlainFormat(sheetId, cnyNetAssetRow, cnyNetAssetRow + 1, 0, 1));
            // 然后应用货币和百分比格式
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, cnyNetAssetRow, cnyNetAssetRow + 1, 1, 3, "CNY"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, cnyNetAssetRow, cnyNetAssetRow + 1, 3, 4));
        }

        // USD总计部分格式化
        if (totalTitleRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, totalTitleRow, totalTitleRow + 1, 0, 9)); // "折算为USD总计"标题
        }
        if (totalHeaderRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, totalHeaderRow, totalHeaderRow + 1, 0, 9)); // 表头

            // 格式化USD总计数据行（从表头下一行到总计行之前）
            if (totalAssetRow != -1) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, totalHeaderRow + 1, totalAssetRow, 1, 3, "USD"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, totalHeaderRow + 1, totalAssetRow, 3, 4));
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, totalHeaderRow + 1, totalAssetRow, 6, 8, "USD"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, totalHeaderRow + 1, totalAssetRow, 8, 9));
            }
        }
        if (totalAssetRow != -1) {
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, totalAssetRow, totalAssetRow + 1, 1, 3, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, totalAssetRow, totalAssetRow + 1, 3, 4));
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, totalAssetRow, totalAssetRow + 1, 6, 8, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, totalAssetRow, totalAssetRow + 1, 8, 9));
        }

        // 净资产类别表头
        if (netWorthHeaderRow != -1) {
            formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, netWorthHeaderRow, netWorthHeaderRow + 1, 0, 4));

            // 净资产类别数据行（从表头下一行到总计行之前）
            if (totalNetAssetRow != -1) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, netWorthHeaderRow + 1, totalNetAssetRow, 1, 3, "USD"));
                formatRequests.add(googleSheetsService.createPercentFormat(sheetId, netWorthHeaderRow + 1, totalNetAssetRow, 3, 4));
            }
        }

        if (totalNetAssetRow != -1) {
            formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, totalNetAssetRow, totalNetAssetRow + 1, 1, 3, "USD"));
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, totalNetAssetRow, totalNetAssetRow + 1, 3, 4));
        }

        googleSheetsService.formatCells(spreadsheetId, formatRequests);
    }

    /**
     * 导出开支表
     */
    private void exportExpenseSheet(String spreadsheetId, Long familyId, Integer year, String currency)
            throws IOException, GeneralSecurityException {
        log.info("导出开支表: {}", currency);

        String sheetName = "开支表-" + currency;
        Integer sheetId = googleSheetsService.addSheet(spreadsheetId, sheetName);

        List<List<Object>> rows = new ArrayList<>();

        // 标题行
        rows.add(Arrays.asList(year + "年度支出表 (" + currency + ")"));
        rows.add(Arrays.asList()); // 空行

        // 日常开支（上半年）
        addExpenseHalfYear(rows, familyId, year, currency, 1, 6, "日常开支 - 上半年 (1-6月)", false);
        rows.add(Arrays.asList()); // 空行
        rows.add(Arrays.asList()); // 空行

        // 大项开支（上半年）
        addExpenseHalfYear(rows, familyId, year, currency, 1, 6, "大项开支 - 上半年 (1-6月)", true);
        rows.add(Arrays.asList()); // 空行
        rows.add(Arrays.asList()); // 空行

        // 日常开支（下半年）
        addExpenseHalfYear(rows, familyId, year, currency, 7, 12, "日常开支 - 下半年 (7-12月)", false);
        rows.add(Arrays.asList()); // 空行
        rows.add(Arrays.asList()); // 空行

        // 大项开支（下半年）
        addExpenseHalfYear(rows, familyId, year, currency, 7, 12, "大项开支 - 下半年 (7-12月)", true);

        // 写入数据
        googleSheetsService.writeData(spreadsheetId, sheetName, rows);

        // 应用格式化
        List<Request> formatRequests = new ArrayList<>();

        // 1. 为所有单元格添加边框（大类+小类+预算+去年实际+6个月+总计+差异+剩余预算 = 14列）
        int maxCols = 14;
        formatRequests.add(googleSheetsService.createBordersForAll(sheetId, rows.size(), maxCols));

        // 2. 设置列宽以防止文字被遮挡
        formatRequests.add(googleSheetsService.createColumnWidthFormat(sheetId, 0, 1, 120)); // 大类列
        formatRequests.add(googleSheetsService.createColumnWidthFormat(sheetId, 1, 2, 180)); // 小类列
        formatRequests.add(googleSheetsService.createColumnWidthFormat(sheetId, 2, maxCols, 100)); // 其他列

        // 3. 合并并居中主标题
        formatRequests.addAll(googleSheetsService.createMergeAndCenterFormat(sheetId, 0, 1, 0, maxCols));

        // 3. 格式化所有小标题和表头行（每个section都有小标题+表头）
        int currentRow = 2; // 从第3行开始
        for (int section = 0; section < 4; section++) { // 4个部分：日常上半年、日常下半年、大项上半年、大项下半年
            // 小标题行
            if (currentRow < rows.size()) {
                formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, currentRow, currentRow + 1, 0, maxCols));
                currentRow++;
            }

            // 表头行
            if (currentRow < rows.size()) {
                formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, currentRow, currentRow + 1, 0, maxCols));
                currentRow++;

                // 数据行（假设每个部分最多30行数据）
                int sectionDataRows = 0;
                while (currentRow < rows.size() && sectionDataRows < 30) {
                    // 检查是否是下一个section的开始（空行后的小标题）
                    if (currentRow + 1 < rows.size() && rows.get(currentRow).isEmpty()) {
                        currentRow++; // 跳过空行
                        if (currentRow < rows.size() && rows.get(currentRow).isEmpty()) {
                            currentRow++; // 跳过第二个空行
                        }
                        if (currentRow < rows.size() && rows.get(currentRow).isEmpty()) {
                            currentRow++; // 跳过第三个空行
                        }
                        break;
                    }
                    sectionDataRows++;
                    currentRow++;
                }
            }
        }

        // 4. 格式化所有金额列为货币格式（跳过大类和小类列）
        // 预算列(col 2)、去年实际(col 3)、月度列(col 4-9)、总计(col 10)、差异(col 11)
        // 注意：跳过剩余预算列(col 12)，因为它需要带背景色的特殊格式
        formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, 3, rows.size(), 2, 12, currency));

        // 5. 添加剩余预算列的条件颜色格式（基于预算百分比）
        // 这个方法会为剩余预算列(col 12)同时设置货币格式和背景色
        addBudgetColorFormatting(formatRequests, sheetId, rows, currency, familyId, year);

        googleSheetsService.formatCells(spreadsheetId, formatRequests);
    }

    /**
     * 添加半年支出数据（优化版：消除重复查询）
     */
    private void addExpenseHalfYear(List<List<Object>> rows, Long familyId, Integer year, String currency,
                                   int startMonth, int endMonth, String title, boolean isMajorItems) {
        Set<String> majorItemCodes = new HashSet<>(Arrays.asList("HOUSING", "TRANSPORTATION", "BUSINESS"));

        // 小标题
        rows.add(Arrays.asList(title));

        // 表头
        List<Object> headerRow = new ArrayList<>();
        headerRow.add("大类");
        headerRow.add("小类");
        headerRow.add("预算");
        headerRow.add("去年实际");
        for (int month = startMonth; month <= endMonth; month++) {
            headerRow.add(month + "月");
        }
        headerRow.add("实际总计");
        headerRow.add("差异");
        headerRow.add("剩余预算");
        rows.add(headerRow);

        // 获取所有大类
        List<ExpenseCategoryMajor> majorCategories = expenseCategoryMajorRepository.findAll();

        // 🚀 优化：收集所有需要的小类ID，批量预加载数据
        List<Long> allMinorIds = new ArrayList<>();
        for (ExpenseCategoryMajor major : majorCategories) {
            if (major.getId() == 0) continue;
            boolean isMajor = majorItemCodes.contains(major.getCode());
            if (isMajorItems != isMajor) continue;

            List<ExpenseCategoryMinor> minorCategories = expenseCategoryMinorRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());
            minorCategories.forEach(minor -> allMinorIds.add(minor.getId()));
        }

        // 🚀 批量加载预算数据（一次查询）
        Map<Long, BigDecimal> budgetMap = new HashMap<>();
        if (!allMinorIds.isEmpty()) {
            List<ExpenseBudget> budgets = expenseBudgetRepository
                .findByFamilyIdAndBudgetYearAndCurrencyAndMinorCategoryIdIn(
                    familyId, year, currency, allMinorIds);
            budgets.forEach(b -> budgetMap.put(b.getMinorCategoryId(), b.getBudgetAmount()));
        }

        // 🚀 批量加载去年的开支记录（12-72次查询 -> 1次）
        Map<String, Map<Long, BigDecimal>> lastYearRecordsMap = new HashMap<>();
        for (int month = startMonth; month <= endMonth; month++) {
            String period = String.format("%d-%02d", year - 1, month);
            if (!allMinorIds.isEmpty()) {
                List<ExpenseRecord> records = expenseRecordRepository
                    .findByFamilyIdAndExpensePeriodAndCurrencyAndMinorCategoryIdIn(
                        familyId, period, currency, allMinorIds);
                Map<Long, BigDecimal> monthMap = new HashMap<>();
                records.forEach(r -> monthMap.put(r.getMinorCategoryId(), r.getAmount()));
                lastYearRecordsMap.put(period, monthMap);
            }
        }

        // 🚀 批量加载今年的开支记录（12-72次查询 -> 1次）
        Map<String, Map<Long, BigDecimal>> currentYearRecordsMap = new HashMap<>();
        for (int month = startMonth; month <= endMonth; month++) {
            String period = String.format("%d-%02d", year, month);
            if (!allMinorIds.isEmpty()) {
                List<ExpenseRecord> records = expenseRecordRepository
                    .findByFamilyIdAndExpensePeriodAndCurrencyAndMinorCategoryIdIn(
                        familyId, period, currency, allMinorIds);
                Map<Long, BigDecimal> monthMap = new HashMap<>();
                records.forEach(r -> monthMap.put(r.getMinorCategoryId(), r.getAmount()));
                currentYearRecordsMap.put(period, monthMap);
            }
        }

        // 🚀 批量加载上半年数据（如果是下半年）
        Map<String, Map<Long, BigDecimal>> firstHalfRecordsMap = new HashMap<>();
        if (startMonth >= 7 && !allMinorIds.isEmpty()) {
            for (int month = 1; month <= 6; month++) {
                String period = String.format("%d-%02d", year, month);
                List<ExpenseRecord> records = expenseRecordRepository
                    .findByFamilyIdAndExpensePeriodAndCurrencyAndMinorCategoryIdIn(
                        familyId, period, currency, allMinorIds);
                Map<Long, BigDecimal> monthMap = new HashMap<>();
                records.forEach(r -> monthMap.put(r.getMinorCategoryId(), r.getAmount()));
                firstHalfRecordsMap.put(period, monthMap);
            }
        }

        // 总计累加器
        BigDecimal grandTotalBudget = BigDecimal.ZERO;
        BigDecimal grandTotalLastYear = BigDecimal.ZERO;
        BigDecimal grandTotalActual = BigDecimal.ZERO;
        BigDecimal[] grandTotalMonthly = new BigDecimal[endMonth - startMonth + 1];
        for (int i = 0; i < grandTotalMonthly.length; i++) {
            grandTotalMonthly[i] = BigDecimal.ZERO;
        }

        // 现在使用预加载的数据，无需再查询数据库
        for (ExpenseCategoryMajor major : majorCategories) {
            if (major.getId() == 0) continue;

            boolean isMajor = majorItemCodes.contains(major.getCode());
            if (isMajorItems != isMajor) continue;

            List<ExpenseCategoryMinor> minorCategories = expenseCategoryMinorRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());

            if (minorCategories.isEmpty()) continue;

            BigDecimal majorBudgetTotal = BigDecimal.ZERO;
            BigDecimal majorActualTotal = BigDecimal.ZERO;
            BigDecimal majorLastYearTotal = BigDecimal.ZERO;

            // 小类明细行（只循环一次，使用预加载数据）
            for (ExpenseCategoryMinor minor : minorCategories) {
                BigDecimal budget = budgetMap.getOrDefault(minor.getId(), BigDecimal.ZERO);

                // 计算去年总计（使用预加载数据）
                BigDecimal lastYearTotal = BigDecimal.ZERO;
                for (int month = startMonth; month <= endMonth; month++) {
                    String lastYearPeriod = String.format("%d-%02d", year - 1, month);
                    Map<Long, BigDecimal> monthMap = lastYearRecordsMap.get(lastYearPeriod);
                    if (monthMap != null) {
                        lastYearTotal = lastYearTotal.add(monthMap.getOrDefault(minor.getId(), BigDecimal.ZERO));
                    }
                }

                // 计算今年月度实际（使用预加载数据）
                BigDecimal[] monthlyActuals = new BigDecimal[endMonth - startMonth + 1];
                BigDecimal actualTotal = BigDecimal.ZERO;
                for (int month = startMonth; month <= endMonth; month++) {
                    String period = String.format("%d-%02d", year, month);
                    Map<Long, BigDecimal> monthMap = currentYearRecordsMap.get(period);
                    BigDecimal monthlyAmount = BigDecimal.ZERO;
                    if (monthMap != null) {
                        monthlyAmount = monthMap.getOrDefault(minor.getId(), BigDecimal.ZERO);
                    }
                    monthlyActuals[month - startMonth] = monthlyAmount;
                    actualTotal = actualTotal.add(monthlyAmount);
                }

                // 计算剩余预算
                BigDecimal remainingBudget;
                if (startMonth >= 7) {
                    // 下半年：使用预加载的上半年数据
                    BigDecimal firstHalfActual = BigDecimal.ZERO;
                    for (int month = 1; month <= 6; month++) {
                        String period = String.format("%d-%02d", year, month);
                        Map<Long, BigDecimal> monthMap = firstHalfRecordsMap.get(period);
                        if (monthMap != null) {
                            firstHalfActual = firstHalfActual.add(monthMap.getOrDefault(minor.getId(), BigDecimal.ZERO));
                        }
                    }
                    remainingBudget = budget.subtract(firstHalfActual).subtract(actualTotal);
                } else {
                    // 上半年
                    remainingBudget = budget.subtract(actualTotal);
                }

                // 累加大类小计
                majorBudgetTotal = majorBudgetTotal.add(budget);
                majorActualTotal = majorActualTotal.add(actualTotal);
                majorLastYearTotal = majorLastYearTotal.add(lastYearTotal);

                // 生成行数据
                List<Object> row = new ArrayList<>();
                row.add(major.getName());
                row.add(minor.getName());
                row.add(budget.doubleValue());
                row.add(lastYearTotal.doubleValue());
                for (int j = 0; j < monthlyActuals.length; j++) {
                    BigDecimal monthly = monthlyActuals[j];
                    row.add(monthly.doubleValue());
                    grandTotalMonthly[j] = grandTotalMonthly[j].add(monthly);
                }
                row.add(actualTotal.doubleValue());
                row.add(actualTotal.subtract(lastYearTotal).doubleValue());
                row.add(remainingBudget.doubleValue());
                rows.add(row);
            }

            // 累加到总计
            grandTotalBudget = grandTotalBudget.add(majorBudgetTotal);
            grandTotalLastYear = grandTotalLastYear.add(majorLastYearTotal);
            grandTotalActual = grandTotalActual.add(majorActualTotal);
        }

        // 计算总剩余预算（使用已预加载的数据）
        BigDecimal grandRemainingBudget;

        if (startMonth >= 7) {
            // 下半年：使用预加载的上半年数据计算总实际支出
            BigDecimal grandFirstHalfActual = BigDecimal.ZERO;
            for (Map.Entry<String, Map<Long, BigDecimal>> entry : firstHalfRecordsMap.entrySet()) {
                Map<Long, BigDecimal> monthMap = entry.getValue();
                for (BigDecimal amount : monthMap.values()) {
                    grandFirstHalfActual = grandFirstHalfActual.add(amount);
                }
            }
            grandRemainingBudget = grandTotalBudget.subtract(grandFirstHalfActual).subtract(grandTotalActual);
        } else {
            grandRemainingBudget = grandTotalBudget.subtract(grandTotalActual);
        }

        // 添加总计行
        List<Object> totalRow = new ArrayList<>();
        totalRow.add("总计");
        totalRow.add("");
        totalRow.add(grandTotalBudget.doubleValue());
        totalRow.add(grandTotalLastYear.doubleValue());
        for (BigDecimal monthlyTotal : grandTotalMonthly) {
            totalRow.add(monthlyTotal.doubleValue());
        }
        totalRow.add(grandTotalActual.doubleValue());
        totalRow.add(grandTotalActual.subtract(grandTotalLastYear).doubleValue()); // 差异 = 今年实际 - 去年实际
        totalRow.add(grandRemainingBudget.doubleValue());
        rows.add(totalRow);
    }

    /**
     * 导出投资账户明细
     */
    private void exportInvestmentAccountSheet(String spreadsheetId, Long familyId, Integer year)
            throws IOException, GeneralSecurityException {
        log.info("导出投资账户明细");

        Integer sheetId = googleSheetsService.addSheet(spreadsheetId, "投资账户明细");

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList(year + "年投资账户明细"));
        rows.add(Arrays.asList()); // 空行

        // 获取投资账户（排除退休基金和房产类）
        List<AssetAccount> investmentAccounts = assetAccountRepository
            .findByFamilyIdAndIsActiveTrue(familyId).stream()
            .filter(account -> account.getAssetType() != null &&
                             Boolean.TRUE.equals(account.getAssetType().getIsInvestment()) &&
                             !RETIREMENT_FUND_TYPE.equals(account.getAssetType().getType()) &&
                             !"REAL_ESTATE".equals(account.getAssetType().getType())) // 排除房产
            .collect(Collectors.toList());

        if (investmentAccounts.isEmpty()) {
            rows.add(Arrays.asList("暂无投资账户数据"));
            googleSheetsService.writeData(spreadsheetId, "投资账户明细", rows);
            return;
        }

        // 表头
        List<Object> headerRow = new ArrayList<>();
        headerRow.add("日期");
        for (AssetAccount account : investmentAccounts) {
            headerRow.add(account.getAccountName());
        }
        headerRow.add("总计");
        rows.add(headerRow);

        // 去年年底数据
        LocalDate lastYearEnd = LocalDate.of(year - 1, 12, 31);
        List<Object> lastYearRow = new ArrayList<>();
        lastYearRow.add(lastYearEnd.format(DATE_FORMATTER) + " (去年年底)");

        BigDecimal lastYearTotal = BigDecimal.ZERO;
        List<BigDecimal> lastYearAccountValues = new ArrayList<>();
        for (AssetAccount account : investmentAccounts) {
            Optional<AssetRecord> recordOpt = assetRecordRepository
                .findLatestByAccountAndDate(account.getId(), lastYearEnd);
            BigDecimal amountUSD = BigDecimal.ZERO;
            if (recordOpt.isPresent()) {
                AssetRecord record = recordOpt.get();
                amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), lastYearEnd);
                lastYearRow.add(amountUSD.doubleValue());
                lastYearTotal = lastYearTotal.add(amountUSD);
            } else {
                lastYearRow.add(0.0);
            }
            lastYearAccountValues.add(amountUSD);
        }
        lastYearRow.add(lastYearTotal.doubleValue());
        rows.add(lastYearRow);

        // 本年度数据
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Set<LocalDate> allDates = new TreeSet<>();
        for (AssetAccount account : investmentAccounts) {
            List<AssetRecord> records = assetRecordRepository
                .findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(account.getId(), startDate, endDate);
            records.forEach(r -> allDates.add(r.getRecordDate()));
        }

        // 存储最新的一行数据用于计算同比
        List<BigDecimal> latestAccountValues = null;
        BigDecimal latestTotal = null;

        for (LocalDate date : allDates) {
            List<Object> row = new ArrayList<>();
            row.add(date.format(DATE_FORMATTER));

            BigDecimal dateTotal = BigDecimal.ZERO;
            List<BigDecimal> currentValues = new ArrayList<>();
            for (AssetAccount account : investmentAccounts) {
                Optional<AssetRecord> recordOpt = assetRecordRepository
                    .findLatestByAccountAndDate(account.getId(), date);
                if (recordOpt.isPresent()) {
                    AssetRecord record = recordOpt.get();
                    BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), date);
                    row.add(amountUSD.doubleValue());
                    dateTotal = dateTotal.add(amountUSD);
                    currentValues.add(amountUSD);
                } else {
                    row.add("");
                    currentValues.add(BigDecimal.ZERO);
                }
            }
            row.add(dateTotal.doubleValue());
            rows.add(row);

            // 保存最新一行数据
            latestAccountValues = currentValues;
            latestTotal = dateTotal;
        }

        // 添加同比百分比行（如果有最新数据）
        if (latestAccountValues != null && latestTotal != null) {
            rows.add(Arrays.asList()); // 空行

            List<Object> changeRow = new ArrayList<>();
            changeRow.add("同比去年底");
            for (int i = 0; i < investmentAccounts.size(); i++) {
                BigDecimal latest = latestAccountValues.get(i);
                BigDecimal lastYear = lastYearAccountValues.get(i);
                double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                    ? latest.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                    : (latest.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);
                changeRow.add(changePct / 100); // 格式化为百分比
            }

            double totalChangePct = lastYearTotal.compareTo(BigDecimal.ZERO) != 0
                ? latestTotal.subtract(lastYearTotal).divide(lastYearTotal, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (latestTotal.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);
            changeRow.add(totalChangePct / 100);

            rows.add(changeRow);
        }

        googleSheetsService.writeData(spreadsheetId, "投资账户明细", rows);

        // 应用格式化
        List<Request> formatRequests = new ArrayList<>();

        int colCount = investmentAccounts.size() + 2; // 日期列 + 账户列 + 总计列

        // 1. 为所有单元格添加边框
        formatRequests.add(googleSheetsService.createBordersForAll(sheetId, rows.size(), colCount));

        // 2. 合并并居中主标题
        formatRequests.addAll(googleSheetsService.createMergeAndCenterFormat(sheetId, 0, 1, 0, colCount));

        // 3. 格式化表头
        formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, 2, 3, 0, colCount)); // 表头

        // 3. 格式化金额列为USD货币格式（除了第一列日期和最后的同比百分比行）
        if (rows.size() > 3) {
            // 数据行（不包括最后的同比百分比行）
            int dataEndRow = rows.size() - 2; // 最后两行是空行和同比百分比行
            if (dataEndRow > 3) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, 3, dataEndRow, 1, colCount, "USD"));
            }

            // 同比百分比行（最后一行）
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, rows.size() - 1, rows.size(), 1, colCount));
        }

        // 4. 添加嵌入式折线图（如果有数据）
        if (rows.size() > 4) { // 至少有标题、空行、表头、去年数据、今年数据
            // 图表数据范围：从表头到数据结束（包括去年底和所有今年数据，不包括同比百分比行）
            int chartDataEndRow = rows.size() - 2; // 排除空行和同比行
            if (chartDataEndRow > 3) {
                // 图表使用嵌入模式（固定位置），放在数据下方
                formatRequests.add(googleSheetsService.createEmbeddedLineChart(
                    sheetId,
                    year + "年投资账户趋势",
                    2, // 表头行开始（0-based，包含去年底数据）
                    chartDataEndRow, // 数据结束行（不包含）
                    0, // 日期列
                    colCount, // 所有列（包括总计）
                    rows.size() + 2 // 图表锚点行（数据下方留2行空白）
                ));
            }
        }

        googleSheetsService.formatCells(spreadsheetId, formatRequests);
    }

    /**
     * 导出退休账户明细
     */
    private void exportRetirementAccountSheet(String spreadsheetId, Long familyId, Integer year)
            throws IOException, GeneralSecurityException {
        log.info("导出退休账户明细");

        Integer sheetId = googleSheetsService.addSheet(spreadsheetId, "退休账户明细");

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList(year + "年退休账户明细"));
        rows.add(Arrays.asList()); // 空行

        List<AssetAccount> retirementAccounts = assetAccountRepository
            .findByFamilyIdAndIsActiveTrue(familyId).stream()
            .filter(account -> account.getAssetType() != null &&
                             RETIREMENT_FUND_TYPE.equals(account.getAssetType().getType()))
            .collect(Collectors.toList());

        if (retirementAccounts.isEmpty()) {
            rows.add(Arrays.asList("暂无退休账户数据"));
            googleSheetsService.writeData(spreadsheetId, "退休账户明细", rows);
            return;
        }

        // 表头
        List<Object> headerRow = new ArrayList<>();
        headerRow.add("日期");
        for (AssetAccount account : retirementAccounts) {
            headerRow.add(account.getAccountName());
        }
        headerRow.add("总计");
        rows.add(headerRow);

        // 去年年底数据
        LocalDate lastYearEnd = LocalDate.of(year - 1, 12, 31);
        List<Object> lastYearRow = new ArrayList<>();
        lastYearRow.add(lastYearEnd.format(DATE_FORMATTER) + " (去年年底)");

        BigDecimal lastYearTotal = BigDecimal.ZERO;
        List<BigDecimal> lastYearAccountValues = new ArrayList<>();
        for (AssetAccount account : retirementAccounts) {
            Optional<AssetRecord> recordOpt = assetRecordRepository
                .findLatestByAccountAndDate(account.getId(), lastYearEnd);
            BigDecimal amountUSD = BigDecimal.ZERO;
            if (recordOpt.isPresent()) {
                AssetRecord record = recordOpt.get();
                amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), lastYearEnd);
                lastYearRow.add(amountUSD.doubleValue());
                lastYearTotal = lastYearTotal.add(amountUSD);
            } else {
                lastYearRow.add(0.0);
            }
            lastYearAccountValues.add(amountUSD);
        }
        lastYearRow.add(lastYearTotal.doubleValue());
        rows.add(lastYearRow);

        // 本年度数据
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Set<LocalDate> allDates = new TreeSet<>();
        for (AssetAccount account : retirementAccounts) {
            List<AssetRecord> records = assetRecordRepository
                .findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(account.getId(), startDate, endDate);
            records.forEach(r -> allDates.add(r.getRecordDate()));
        }

        // 存储最新的一行数据用于计算同比
        List<BigDecimal> latestAccountValues = null;
        BigDecimal latestTotal = null;

        for (LocalDate date : allDates) {
            List<Object> row = new ArrayList<>();
            row.add(date.format(DATE_FORMATTER));

            BigDecimal dateTotal = BigDecimal.ZERO;
            List<BigDecimal> currentValues = new ArrayList<>();
            for (AssetAccount account : retirementAccounts) {
                Optional<AssetRecord> recordOpt = assetRecordRepository
                    .findLatestByAccountAndDate(account.getId(), date);
                if (recordOpt.isPresent()) {
                    AssetRecord record = recordOpt.get();
                    BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), date);
                    row.add(amountUSD.doubleValue());
                    dateTotal = dateTotal.add(amountUSD);
                    currentValues.add(amountUSD);
                } else {
                    row.add("");
                    currentValues.add(BigDecimal.ZERO);
                }
            }
            row.add(dateTotal.doubleValue());
            rows.add(row);

            // 保存最新一行数据
            latestAccountValues = currentValues;
            latestTotal = dateTotal;
        }

        // 添加同比百分比行（如果有最新数据）
        if (latestAccountValues != null && latestTotal != null) {
            rows.add(Arrays.asList()); // 空行

            List<Object> changeRow = new ArrayList<>();
            changeRow.add("同比去年底");
            for (int i = 0; i < retirementAccounts.size(); i++) {
                BigDecimal latest = latestAccountValues.get(i);
                BigDecimal lastYear = lastYearAccountValues.get(i);
                double changePct = lastYear.compareTo(BigDecimal.ZERO) != 0
                    ? latest.subtract(lastYear).divide(lastYear, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                    : (latest.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);
                changeRow.add(changePct / 100); // 格式化为百分比
            }

            double totalChangePct = lastYearTotal.compareTo(BigDecimal.ZERO) != 0
                ? latestTotal.subtract(lastYearTotal).divide(lastYearTotal, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : (latestTotal.compareTo(BigDecimal.ZERO) != 0 ? 100.0 : 0.0);
            changeRow.add(totalChangePct / 100);

            rows.add(changeRow);
        }

        googleSheetsService.writeData(spreadsheetId, "退休账户明细", rows);

        // 应用格式化
        List<Request> formatRequests = new ArrayList<>();

        int colCount = retirementAccounts.size() + 2; // 日期列 + 账户列 + 总计列

        // 1. 为所有单元格添加边框
        formatRequests.add(googleSheetsService.createBordersForAll(sheetId, rows.size(), colCount));

        // 2. 合并并居中主标题
        formatRequests.addAll(googleSheetsService.createMergeAndCenterFormat(sheetId, 0, 1, 0, colCount));

        // 3. 格式化表头
        formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, 2, 3, 0, colCount)); // 表头

        // 3. 格式化金额列为USD货币格式（除了第一列日期和最后的同比百分比行）
        if (rows.size() > 3) {
            // 数据行（不包括最后的同比百分比行）
            int dataEndRow = rows.size() - 2; // 最后两行是空行和同比百分比行
            if (dataEndRow > 3) {
                formatRequests.add(googleSheetsService.createCurrencyFormat(sheetId, 3, dataEndRow, 1, colCount, "USD"));
            }

            // 同比百分比行（最后一行）
            formatRequests.add(googleSheetsService.createPercentFormat(sheetId, rows.size() - 1, rows.size(), 1, colCount));
        }

        // 4. 添加嵌入式折线图（如果有数据）
        if (rows.size() > 4) { // 至少有标题、空行、表头、去年数据、今年数据
            // 图表数据范围：从表头到数据结束（包括去年底和所有今年数据，不包括同比百分比行）
            int chartDataEndRow = rows.size() - 2; // 排除空行和同比行
            if (chartDataEndRow > 3) {
                // 图表使用嵌入模式（固定位置），放在数据下方
                formatRequests.add(googleSheetsService.createEmbeddedLineChart(
                    sheetId,
                    year + "年退休账户趋势",
                    2, // 表头行开始（0-based，包含去年底数据）
                    chartDataEndRow, // 数据结束行（不包含）
                    0, // 日期列
                    colCount, // 所有列（包括总计）
                    rows.size() + 2 // 图表锚点行（数据下方留2行空白）
                ));
            }
        }

        googleSheetsService.formatCells(spreadsheetId, formatRequests);
    }

    /**
     * 为剩余预算列添加条件颜色格式
     * 绿色: 剩余预算 > 20% of 预算
     * 黄色: 0% <= 剩余预算 <= 20% of 预算
     * 红色: 剩余预算 < 0 (负数)
     */
    private void addBudgetColorFormatting(List<Request> formatRequests, Integer sheetId,
                                         List<List<Object>> rows, String currency,
                                         Long familyId, Integer year) {
        Set<String> majorItemCodes = new HashSet<>(Arrays.asList("HOUSING", "TRANSPORTATION", "BUSINESS"));

        // 遍历所有数据行，为剩余预算列添加颜色
        int rowIndex = 0;

        for (List<Object> row : rows) {
            if (row.isEmpty() || row.size() < 13) { // 需要至少13列才能访问索引12
                rowIndex++;
                continue;
            }

            String firstCell = row.get(0).toString();

            // 跳过标题行、表头行和总计行
            if (firstCell.contains("年度支出表") || firstCell.equals("大类") ||
                firstCell.equals("总计") || firstCell.contains("半年") ||
                firstCell.contains("大项开支") || firstCell.contains("日常开支")) {
                rowIndex++;
                continue;
            }

            // 获取预算和剩余预算
            Object budgetObj = row.get(2); // 预算列
            Object remainingObj = row.get(12); // 剩余预算列（索引12，第13列）

            // 支持Number类型（包括Double、BigDecimal等）
            if (budgetObj instanceof Number && remainingObj instanceof Number) {
                double budget = ((Number) budgetObj).doubleValue();
                double remaining = ((Number) remainingObj).doubleValue();

                if (budget > 0) {
                    double remainingPct = remaining / budget;

                    // 根据百分比设置颜色（带边框和货币格式）
                    Color backgroundColor;
                    if (remaining < 0) {
                        // 红色 (负数 - 超支)
                        backgroundColor = new Color().setRed(1.0f).setGreen(0.8f).setBlue(0.8f);
                    } else if (remainingPct <= 0.2) {
                        // 黄色 (0-20% - 预算紧张)
                        backgroundColor = new Color().setRed(1.0f).setGreen(1.0f).setBlue(0.8f);
                    } else {
                        // 绿色 (>20% - 预算充足)
                        backgroundColor = new Color().setRed(0.8f).setGreen(1.0f).setBlue(0.8f);
                    }

                    // 创建带背景色和货币格式的单元格格式
                    String pattern = "USD".equals(currency) ? "$#,##0.00" : "¥#,##0.00";
                    formatRequests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(rowIndex)
                            .setEndRowIndex(rowIndex + 1)
                            .setStartColumnIndex(12)
                            .setEndColumnIndex(13))
                        .setCell(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                .setBackgroundColor(backgroundColor)
                                .setNumberFormat(new NumberFormat()
                                    .setType("CURRENCY")
                                    .setPattern(pattern))
                                .setBorders(new Borders()
                                    .setTop(new Border().setStyle("SOLID"))
                                    .setBottom(new Border().setStyle("SOLID"))
                                    .setLeft(new Border().setStyle("SOLID"))
                                    .setRight(new Border().setStyle("SOLID")))))
                        .setFields("userEnteredFormat(backgroundColor,numberFormat,borders)")));
                }
            }

            rowIndex++;
        }
    }

    /**
     * 导出资产负债表明细Sheet
     * 显示所有资产、负债账户的最新值，横坐标为用户名，按货币分开显示
     */
    private void exportBalanceSheetDetail(String spreadsheetId, Long familyId, Integer year)
            throws IOException, GeneralSecurityException {
        log.info("导出资产负债表明细");

        Integer sheetId = googleSheetsService.addSheet(spreadsheetId, "资产负债表明细");
        LocalDate asOfDate = LocalDate.of(year, 12, 31);

        List<List<Object>> rows = new ArrayList<>();

        // 标题行
        rows.add(Arrays.asList(year + "年资产负债表明细"));
        rows.add(Arrays.asList()); // 空行

        // 按货币分组
        List<String> currencies = Arrays.asList("USD", "CNY");

        for (String currency : currencies) {
            // 货币标题
            rows.add(Arrays.asList(currency + " 账户明细"));
            rows.add(Arrays.asList()); // 空行

            // 资产部分
            rows.add(Arrays.asList("资产账户"));

            // 一次性查询所有资产账户
            List<AssetAccount> allAssetAccounts = assetAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);

            // 预加载所有资产记录（减少数据库查询）
            Map<Long, AssetRecord> assetRecordMap = new HashMap<>();
            for (AssetAccount account : allAssetAccounts) {
                Optional<AssetRecord> recordOpt = assetRecordRepository
                    .findLatestByAccountAndDate(account.getId(), asOfDate);
                recordOpt.ifPresent(record -> assetRecordMap.put(account.getId(), record));
            }

            // 过滤出该货币的资产账户
            List<AssetAccount> assetAccounts = allAssetAccounts.stream()
                .filter(account -> {
                    AssetRecord record = assetRecordMap.get(account.getId());
                    return record != null && currency.equals(record.getCurrency());
                })
                .collect(Collectors.toList());

            if (!assetAccounts.isEmpty()) {
                // 🚀 优化：批量加载所有用户（避免N+1查询）
                Set<Long> userIds = assetAccounts.stream()
                    .map(AssetAccount::getUserId)
                    .collect(Collectors.toSet());
                List<User> users = userRepository.findAllById(userIds);
                Map<Long, String> userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

                // 按用户分组
                Map<String, List<AssetAccount>> accountsByUser = assetAccounts.stream()
                    .collect(Collectors.groupingBy(acc -> userIdToName.get(acc.getUserId())));

                // 表头：账户类型 + 各用户名 + 总计
                List<Object> headerRow = new ArrayList<>();
                headerRow.add("账户类型");
                headerRow.add("账户名称");
                List<String> userNames = new ArrayList<>(accountsByUser.keySet());
                headerRow.addAll(userNames);
                headerRow.add("总计");
                rows.add(headerRow);

                // 按资产类型分组
                Map<String, List<AssetAccount>> accountsByType = assetAccounts.stream()
                    .collect(Collectors.groupingBy(acc -> acc.getAssetType().getChineseName()));

                // 用户总计累加器
                Map<String, BigDecimal> userAssetTotals = new HashMap<>();
                for (String userName : userNames) {
                    userAssetTotals.put(userName, BigDecimal.ZERO);
                }
                BigDecimal assetGrandTotal = BigDecimal.ZERO;

                for (Map.Entry<String, List<AssetAccount>> typeEntry : accountsByType.entrySet()) {
                    String typeName = typeEntry.getKey();
                    List<AssetAccount> typeAccounts = typeEntry.getValue();

                    for (AssetAccount account : typeAccounts) {
                        List<Object> row = new ArrayList<>();
                        row.add(typeName);
                        row.add(account.getAccountName());

                        BigDecimal rowTotal = BigDecimal.ZERO;

                        // 为每个用户填充数据（使用预加载的记录）
                        String accountUserName = userIdToName.get(account.getUserId());
                        for (String userName : userNames) {
                            if (userName.equals(accountUserName)) {
                                AssetRecord record = assetRecordMap.get(account.getId());
                                BigDecimal amount = record != null ? record.getAmount() : BigDecimal.ZERO;
                                row.add(amount.doubleValue());
                                rowTotal = rowTotal.add(amount);
                                userAssetTotals.put(userName, userAssetTotals.get(userName).add(amount));
                            } else {
                                row.add(0.0);
                            }
                        }

                        row.add(rowTotal.doubleValue());
                        assetGrandTotal = assetGrandTotal.add(rowTotal);
                        rows.add(row);
                    }
                }

                // 添加资产总计行
                List<Object> assetTotalRow = new ArrayList<>();
                assetTotalRow.add("资产小计");
                assetTotalRow.add("");
                for (String userName : userNames) {
                    assetTotalRow.add(userAssetTotals.get(userName).doubleValue());
                }
                assetTotalRow.add(assetGrandTotal.doubleValue());
                rows.add(assetTotalRow);
            } else {
                rows.add(Arrays.asList("无" + currency + "资产账户"));
            }

            rows.add(Arrays.asList()); // 空行

            // 负债部分
            rows.add(Arrays.asList("负债账户"));

            // 一次性查询所有负债账户
            List<LiabilityAccount> allLiabilityAccounts = liabilityAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);

            // 预加载所有负债记录（减少数据库查询）
            Map<Long, LiabilityRecord> liabilityRecordMap = new HashMap<>();
            for (LiabilityAccount account : allLiabilityAccounts) {
                Optional<LiabilityRecord> recordOpt = liabilityRecordRepository
                    .findLatestByAccountIdBeforeOrOnDate(account.getId(), asOfDate);
                recordOpt.ifPresent(record -> liabilityRecordMap.put(account.getId(), record));
            }

            // 过滤出该货币的负债账户
            List<LiabilityAccount> liabilityAccounts = allLiabilityAccounts.stream()
                .filter(account -> {
                    LiabilityRecord record = liabilityRecordMap.get(account.getId());
                    return record != null && currency.equals(record.getCurrency());
                })
                .collect(Collectors.toList());

            if (!liabilityAccounts.isEmpty()) {
                // 🚀 优化：批量加载所有用户（避免N+1查询）
                Set<Long> liabUserIds = liabilityAccounts.stream()
                    .map(LiabilityAccount::getUserId)
                    .collect(Collectors.toSet());
                List<User> liabUsers = userRepository.findAllById(liabUserIds);
                Map<Long, String> liabUserIdToName = liabUsers.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

                // 按用户分组
                Map<String, List<LiabilityAccount>> liabAccountsByUser = liabilityAccounts.stream()
                    .collect(Collectors.groupingBy(acc -> liabUserIdToName.get(acc.getUserId())));

                // 表头
                List<Object> liabHeaderRow = new ArrayList<>();
                liabHeaderRow.add("账户类型");
                liabHeaderRow.add("账户名称");
                List<String> liabUserNames = new ArrayList<>(liabAccountsByUser.keySet());
                liabHeaderRow.addAll(liabUserNames);
                liabHeaderRow.add("总计");
                rows.add(liabHeaderRow);

                // 按负债类型分组
                Map<String, List<LiabilityAccount>> liabAccountsByType = liabilityAccounts.stream()
                    .collect(Collectors.groupingBy(acc -> acc.getLiabilityType().getChineseName()));

                // 用户总计累加器
                Map<String, BigDecimal> userLiabilityTotals = new HashMap<>();
                for (String userName : liabUserNames) {
                    userLiabilityTotals.put(userName, BigDecimal.ZERO);
                }
                BigDecimal liabilityGrandTotal = BigDecimal.ZERO;

                for (Map.Entry<String, List<LiabilityAccount>> typeEntry : liabAccountsByType.entrySet()) {
                    String typeName = typeEntry.getKey();
                    List<LiabilityAccount> typeAccounts = typeEntry.getValue();

                    for (LiabilityAccount account : typeAccounts) {
                        List<Object> row = new ArrayList<>();
                        row.add(typeName);
                        row.add(account.getAccountName());

                        BigDecimal rowTotal = BigDecimal.ZERO;

                        // 为每个用户填充数据（使用预加载的记录）
                        String accountUserName = liabUserIdToName.get(account.getUserId());
                        for (String userName : liabUserNames) {
                            if (userName.equals(accountUserName)) {
                                LiabilityRecord record = liabilityRecordMap.get(account.getId());
                                BigDecimal amount = record != null ? record.getOutstandingBalance() : BigDecimal.ZERO;
                                row.add(amount.doubleValue());
                                rowTotal = rowTotal.add(amount);
                                userLiabilityTotals.put(userName, userLiabilityTotals.get(userName).add(amount));
                            } else {
                                row.add(0.0);
                            }
                        }

                        row.add(rowTotal.doubleValue());
                        liabilityGrandTotal = liabilityGrandTotal.add(rowTotal);
                        rows.add(row);
                    }
                }

                // 添加负债总计行
                List<Object> liabilityTotalRow = new ArrayList<>();
                liabilityTotalRow.add("负债小计");
                liabilityTotalRow.add("");
                for (String userName : liabUserNames) {
                    liabilityTotalRow.add(userLiabilityTotals.get(userName).doubleValue());
                }
                liabilityTotalRow.add(liabilityGrandTotal.doubleValue());
                rows.add(liabilityTotalRow);
            } else {
                rows.add(Arrays.asList("无" + currency + "负债账户"));
            }

            rows.add(Arrays.asList()); // 空行
            rows.add(Arrays.asList()); // 空行
        }

        // 写入数据
        googleSheetsService.writeData(spreadsheetId, "资产负债表明细", rows);

        // 应用格式化
        List<Request> formatRequests = new ArrayList<>();

        // 确定最大列数（账户类型 + 账户名称 + 可能的多个用户 + 总计）
        int maxCols = 10; // 预估最大列数，后续可以根据实际调整

        // 1. 为所有单元格添加边框
        formatRequests.add(googleSheetsService.createBordersForAll(sheetId, rows.size(), maxCols));

        // 2. 合并并居中主标题
        formatRequests.addAll(googleSheetsService.createMergeAndCenterFormat(sheetId, 0, 1, 0, maxCols));

        // 3. 格式化所有货币标题和表头
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty()) continue;
            String firstCell = row.get(0).toString();

            if (firstCell.contains("账户明细") || firstCell.equals("资产账户") ||
                firstCell.equals("负债账户") || firstCell.equals("账户类型")) {
                formatRequests.add(googleSheetsService.createHeaderFormat(sheetId, i, i + 1, 0, maxCols));
            }
        }

        // 4. 格式化金额列（除了前两列：账户类型和账户名称）
        // 查找数据行并应用货币格式
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty() || row.size() <= 2) continue;

            String firstCell = row.get(0).toString();

            // 跳过标题、空行、表头行
            if (firstCell.contains("明细") || firstCell.contains("账户") ||
                firstCell.equals("账户类型") || firstCell.contains("无")) {
                continue;
            }

            // 确定该行的货币
            String rowCurrency = "USD";
            for (int j = i - 1; j >= 0; j--) {
                if (rows.get(j).isEmpty()) continue;
                String cellValue = rows.get(j).get(0).toString();
                if (cellValue.contains("CNY 账户明细")) {
                    rowCurrency = "CNY";
                    break;
                } else if (cellValue.contains("USD 账户明细")) {
                    rowCurrency = "USD";
                    break;
                }
            }

            // 格式化该行的金额列
            formatRequests.add(googleSheetsService.createCurrencyFormat(
                sheetId, i, i + 1, 2, row.size(), rowCurrency));
        }

        googleSheetsService.formatCells(spreadsheetId, formatRequests);
    }

    /**
     * 删除默认的Sheet1
     */
    private void deleteDefaultSheet(String spreadsheetId) throws IOException, GeneralSecurityException {
        log.info("删除默认Sheet1");
        googleSheetsService.deleteSheetByTitle(spreadsheetId, "工作表1");
    }

    /**
     * 预加载指定年份所需的汇率到缓存
     */
    private void preloadExchangeRates(Integer year) {
        log.info("预加载{}年汇率数据", year);
        Map<String, BigDecimal> cache = EXCHANGE_RATE_CACHE.get();
        cache.clear(); // 清空之前的缓存

        // 需要加载的日期：年初、年底、去年年底，以及每月月底（用于月度趋势）
        List<LocalDate> datesToLoad = new ArrayList<>();
        datesToLoad.add(LocalDate.of(year - 1, 12, 31));  // 去年年底
        datesToLoad.add(LocalDate.of(year, 12, 31));       // 今年年底

        // 添加每月月底
        for (int month = 1; month <= 12; month++) {
            datesToLoad.add(LocalDate.of(year, month, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()));
        }

        // 支持的货币（除USD外）
        List<String> currencies = Arrays.asList("CNY", "EUR", "GBP", "JPY");

        // 批量加载汇率
        for (LocalDate date : datesToLoad) {
            for (String currency : currencies) {
                String cacheKey = currency + "_" + date.toString();
                try {
                    BigDecimal rate = exchangeRateService.getExchangeRate(currency, date);
                    cache.put(cacheKey, rate);
                } catch (Exception e) {
                    log.warn("加载汇率失败: {} on {}, 使用默认值1.0", currency, date, e);
                    cache.put(cacheKey, BigDecimal.ONE);
                }
            }
        }

        log.info("汇率缓存加载完成，共{}条", cache.size());
    }

    /**
     * 清理汇率缓存
     */
    private void clearExchangeRateCache() {
        EXCHANGE_RATE_CACHE.remove();
    }

    /**
     * 货币转换为USD（使用缓存）
     */
    private BigDecimal convertToUSD(BigDecimal amount, String currency, LocalDate date) {
        if ("USD".equals(currency)) {
            return amount;
        }

        // 尝试从缓存获取
        String cacheKey = currency + "_" + date.toString();
        Map<String, BigDecimal> cache = EXCHANGE_RATE_CACHE.get();
        BigDecimal rate = cache.get(cacheKey);

        // 如果缓存中没有，从服务获取（fallback）
        if (rate == null) {
            log.debug("汇率缓存未命中: {}, 从服务获取", cacheKey);
            rate = exchangeRateService.getExchangeRate(currency, date);
            cache.put(cacheKey, rate);
        }

        return amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 测试Google Sheets API连接
     * @return 测试表格的ID
     */
    public String testConnection() throws IOException, GeneralSecurityException {
        log.info("执行真实的Google Sheets API连接测试");

        // 创建一个简单的测试表格
        String spreadsheetId = googleSheetsService.createSpreadsheet("API连接测试 - " + new Date());

        log.info("测试成功，创建了测试表格: {}", spreadsheetId);
        return spreadsheetId;
    }
}
