package com.finance.app.service;

import com.finance.app.model.*;
import com.finance.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel导出服务
 * 生成年度财务报表Excel文件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

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
    private final TaxStatus RETIREMENT = TaxStatus.TAX_DEFERRED;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 生成年度Excel报表
     */
    public byte[] generateAnnualReport(Long familyId, Integer year) throws IOException {
        log.info("开始生成年度Excel报表: familyId={}, year={}", familyId, year);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建样式
            Map<String, CellStyle> styles = createStyles(workbook);

            // Sheet 1: 资产负债表
            createBalanceSheet(workbook, styles, familyId, year);

            // Sheet 2-3: 开支表（USD和CNY）
            createExpenseSheet(workbook, styles, familyId, year, "USD");
            createExpenseSheet(workbook, styles, familyId, year, "CNY");

            // Sheet 4: 投资账户明细
            createInvestmentAccountSheet(workbook, styles, familyId, year);

            // Sheet 5: 退休账户明细
            createRetirementAccountSheet(workbook, styles, familyId, year);

            workbook.write(outputStream);
            log.info("年度Excel报表生成完成");
            return outputStream.toByteArray();
        }
    }

    /**
     * 创建样式
     */
    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // 标题样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("header", headerStyle);

        // 金额样式
        CellStyle amountStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        amountStyle.setDataFormat(format.getFormat("#,##0.00"));
        amountStyle.setBorderBottom(BorderStyle.THIN);
        amountStyle.setBorderTop(BorderStyle.THIN);
        amountStyle.setBorderLeft(BorderStyle.THIN);
        amountStyle.setBorderRight(BorderStyle.THIN);
        styles.put("amount", amountStyle);

        // 日期样式
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd"));
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setBorderTop(BorderStyle.THIN);
        dateStyle.setBorderLeft(BorderStyle.THIN);
        dateStyle.setBorderRight(BorderStyle.THIN);
        styles.put("date", dateStyle);

        // 普通单元格样式
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        styles.put("normal", normalStyle);

        // 汇总行样式
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setDataFormat(format.getFormat("#,##0.00"));
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderBottom(BorderStyle.MEDIUM);
        totalStyle.setBorderTop(BorderStyle.MEDIUM);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        styles.put("total", totalStyle);

        return styles;
    }

    /**
     * Sheet 1: 资产负债表 - 按货币分组显示资产和负债，包含去年年底数据
     */
    private void createBalanceSheet(Workbook workbook, Map<String, CellStyle> styles, Long familyId, Integer year) {
        Sheet sheet = workbook.createSheet("资产负债表");
        LocalDate asOfDate = LocalDate.of(year, 12, 31);
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(year + "年资产负债表");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // 空行

        // 按货币分组资产和负债
        List<String> currencies = Arrays.asList("USD", "CNY");

        for (String currency : currencies) {
            // 货币标题
            Row currencyTitleRow = sheet.createRow(rowNum++);
            createCell(currencyTitleRow, 0, currency + " 资产负债", styles.get("total"));

            // 表头：资产 | 当前年值 | 去年年底 || 负债 | 当前年值 | 去年年底
            Row headerRow = sheet.createRow(rowNum++);
            createCell(headerRow, 0, "资产类型", styles.get("header"));
            createCell(headerRow, 1, "当前年值", styles.get("header"));
            createCell(headerRow, 2, "去年年底", styles.get("header"));
            createCell(headerRow, 3, "", styles.get("header")); // 分隔列
            createCell(headerRow, 4, "负债类型", styles.get("header"));
            createCell(headerRow, 5, "当前年值", styles.get("header"));
            createCell(headerRow, 6, "去年年底", styles.get("header"));

            // 获取该货币的资产数据（按类型汇总）
            Map<String, BigDecimal> assetTypeCurrentAmounts = new HashMap<>();
            Map<String, BigDecimal> assetTypeLastYearAmounts = new HashMap<>();
            BigDecimal currencyTotalAssetsCurrent = BigDecimal.ZERO;
            BigDecimal currencyTotalAssetsLastYear = BigDecimal.ZERO;

            List<AssetRecord> currentAssetRecords = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate)
                .stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (AssetRecord record : currentAssetRecords) {
                String typeName = record.getAccount().getAssetType().getChineseName();
                assetTypeCurrentAmounts.merge(typeName, record.getAmount(), BigDecimal::add);
                currencyTotalAssetsCurrent = currencyTotalAssetsCurrent.add(record.getAmount());
            }

            List<AssetRecord> lastYearAssetRecords = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate)
                .stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (AssetRecord record : lastYearAssetRecords) {
                String typeName = record.getAccount().getAssetType().getChineseName();
                assetTypeLastYearAmounts.merge(typeName, record.getAmount(), BigDecimal::add);
                currencyTotalAssetsLastYear = currencyTotalAssetsLastYear.add(record.getAmount());
            }

            // 获取该货币的负债数据（按类型汇总）
            Map<String, BigDecimal> liabilityTypeCurrentAmounts = new HashMap<>();
            Map<String, BigDecimal> liabilityTypeLastYearAmounts = new HashMap<>();
            BigDecimal currencyTotalLiabilitiesCurrent = BigDecimal.ZERO;
            BigDecimal currencyTotalLiabilitiesLastYear = BigDecimal.ZERO;

            List<LiabilityRecord> currentLiabilityRecords = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate)
                .stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (LiabilityRecord record : currentLiabilityRecords) {
                String typeName = record.getAccount().getLiabilityType().getChineseName();
                liabilityTypeCurrentAmounts.merge(typeName, record.getOutstandingBalance(), BigDecimal::add);
                currencyTotalLiabilitiesCurrent = currencyTotalLiabilitiesCurrent.add(record.getOutstandingBalance());
            }

            List<LiabilityRecord> lastYearLiabilityRecords = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, lastYearEndDate)
                .stream()
                .filter(r -> currency.equals(r.getCurrency()))
                .collect(Collectors.toList());

            for (LiabilityRecord record : lastYearLiabilityRecords) {
                String typeName = record.getAccount().getLiabilityType().getChineseName();
                liabilityTypeLastYearAmounts.merge(typeName, record.getOutstandingBalance(), BigDecimal::add);
                currencyTotalLiabilitiesLastYear = currencyTotalLiabilitiesLastYear.add(record.getOutstandingBalance());
            }

            // 合并所有资产和负债类型名称
            Set<String> allAssetTypes = new HashSet<>();
            allAssetTypes.addAll(assetTypeCurrentAmounts.keySet());
            allAssetTypes.addAll(assetTypeLastYearAmounts.keySet());
            List<String> assetTypesList = new ArrayList<>(allAssetTypes);

            Set<String> allLiabilityTypes = new HashSet<>();
            allLiabilityTypes.addAll(liabilityTypeCurrentAmounts.keySet());
            allLiabilityTypes.addAll(liabilityTypeLastYearAmounts.keySet());
            List<String> liabilityTypesList = new ArrayList<>(allLiabilityTypes);

            // 并排写入资产和负债
            int rows = Math.max(assetTypesList.size(), liabilityTypesList.size());
            for (int i = 0; i < rows; i++) {
                Row row = sheet.createRow(rowNum++);

                // 资产列
                if (i < assetTypesList.size()) {
                    String typeName = assetTypesList.get(i);
                    BigDecimal currentAmount = assetTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    BigDecimal lastYearAmount = assetTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    createCell(row, 0, typeName, styles.get("normal"));
                    createNumericCell(row, 1, currentAmount.doubleValue(), styles.get("amount"));
                    createNumericCell(row, 2, lastYearAmount.doubleValue(), styles.get("amount"));
                } else {
                    createCell(row, 0, "", styles.get("normal"));
                    createCell(row, 1, "", styles.get("amount"));
                    createCell(row, 2, "", styles.get("amount"));
                }

                createCell(row, 3, "", styles.get("normal")); // 分隔列

                // 负债列
                if (i < liabilityTypesList.size()) {
                    String typeName = liabilityTypesList.get(i);
                    BigDecimal currentAmount = liabilityTypeCurrentAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    BigDecimal lastYearAmount = liabilityTypeLastYearAmounts.getOrDefault(typeName, BigDecimal.ZERO);
                    createCell(row, 4, typeName, styles.get("normal"));
                    createNumericCell(row, 5, currentAmount.doubleValue(), styles.get("amount"));
                    createNumericCell(row, 6, lastYearAmount.doubleValue(), styles.get("amount"));
                } else {
                    createCell(row, 4, "", styles.get("normal"));
                    createCell(row, 5, "", styles.get("amount"));
                    createCell(row, 6, "", styles.get("amount"));
                }
            }

            // 该货币小计行
            Row subtotalRow = sheet.createRow(rowNum++);
            createCell(subtotalRow, 0, currency + " 资产小计", styles.get("total"));
            createNumericCell(subtotalRow, 1, currencyTotalAssetsCurrent.doubleValue(), styles.get("total"));
            createNumericCell(subtotalRow, 2, currencyTotalAssetsLastYear.doubleValue(), styles.get("total"));
            createCell(subtotalRow, 3, "", styles.get("total"));
            createCell(subtotalRow, 4, currency + " 负债小计", styles.get("total"));
            createNumericCell(subtotalRow, 5, currencyTotalLiabilitiesCurrent.doubleValue(), styles.get("total"));
            createNumericCell(subtotalRow, 6, currencyTotalLiabilitiesLastYear.doubleValue(), styles.get("total"));

            // 该货币净资产
            Row currencyNetWorthRow = sheet.createRow(rowNum++);
            createCell(currencyNetWorthRow, 0, currency + " 净资产", styles.get("total"));
            createNumericCell(currencyNetWorthRow, 1,
                currencyTotalAssetsCurrent.subtract(currencyTotalLiabilitiesCurrent).doubleValue(),
                styles.get("total"));
            createNumericCell(currencyNetWorthRow, 2,
                currencyTotalAssetsLastYear.subtract(currencyTotalLiabilitiesLastYear).doubleValue(),
                styles.get("total"));

            rowNum += 2; // 空行
        }

        // 计算USD总计（包含货币转换）
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

        // USD总计
        Row usdTotalTitleRow = sheet.createRow(rowNum++);
        createCell(usdTotalTitleRow, 0, "折算为USD总计", styles.get("total"));

        Row usdTotalRow = sheet.createRow(rowNum++);
        createCell(usdTotalRow, 0, "资产总计 (USD)", styles.get("total"));
        createNumericCell(usdTotalRow, 1, totalAssetsCurrent.doubleValue(), styles.get("total"));
        createNumericCell(usdTotalRow, 2, totalAssetsLastYear.doubleValue(), styles.get("total"));
        createCell(usdTotalRow, 3, "", styles.get("total"));
        createCell(usdTotalRow, 4, "负债总计 (USD)", styles.get("total"));
        createNumericCell(usdTotalRow, 5, totalLiabilitiesCurrent.doubleValue(), styles.get("total"));
        createNumericCell(usdTotalRow, 6, totalLiabilitiesLastYear.doubleValue(), styles.get("total"));

        rowNum++; // 空行

        // 净资产总计
        Row netWorthRow = sheet.createRow(rowNum++);
        createCell(netWorthRow, 0, "净资产总计 (USD)", styles.get("total"));
        createNumericCell(netWorthRow, 1, totalAssetsCurrent.subtract(totalLiabilitiesCurrent).doubleValue(), styles.get("total"));
        createNumericCell(netWorthRow, 2, totalAssetsLastYear.subtract(totalLiabilitiesLastYear).doubleValue(), styles.get("total"));

        rowNum += 2; // 空行

        // 明细表头：资产明细 || 负债明细 并排
        Row detailHeaderRow = sheet.createRow(rowNum++);
        createCell(detailHeaderRow, 0, "资产账户", styles.get("header"));
        createCell(detailHeaderRow, 1, "类型", styles.get("header"));
        createCell(detailHeaderRow, 2, "USD金额", styles.get("header"));
        createCell(detailHeaderRow, 3, "", styles.get("header")); // 分隔列
        createCell(detailHeaderRow, 4, "负债账户", styles.get("header"));
        createCell(detailHeaderRow, 5, "类型", styles.get("header"));
        createCell(detailHeaderRow, 6, "USD金额", styles.get("header"));

        // 获取明细
        List<AssetRecord> assetRecords = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate);
        List<LiabilityRecord> liabilityRecords = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate);

        int detailRows = Math.max(assetRecords.size(), liabilityRecords.size());
        for (int i = 0; i < detailRows; i++) {
            Row row = sheet.createRow(rowNum++);

            // 资产明细
            if (i < assetRecords.size()) {
                AssetRecord record = assetRecords.get(i);
                createCell(row, 0, record.getAccount().getAccountName(), styles.get("normal"));
                createCell(row, 1, record.getAccount().getAssetType().getChineseName(), styles.get("normal"));
                createNumericCell(row, 2, convertToUSD(record.getAmount(), record.getCurrency(), asOfDate).doubleValue(), styles.get("amount"));
            } else {
                createCell(row, 0, "", styles.get("normal"));
                createCell(row, 1, "", styles.get("normal"));
                createCell(row, 2, "", styles.get("amount"));
            }

            createCell(row, 3, "", styles.get("normal")); // 分隔列

            // 负债明细
            if (i < liabilityRecords.size()) {
                LiabilityRecord record = liabilityRecords.get(i);
                createCell(row, 4, record.getAccount().getAccountName(), styles.get("normal"));
                createCell(row, 5, record.getAccount().getLiabilityType().getChineseName(), styles.get("normal"));
                createNumericCell(row, 6, convertToUSD(record.getOutstandingBalance(), record.getCurrency(), asOfDate).doubleValue(), styles.get("amount"));
            } else {
                createCell(row, 4, "", styles.get("normal"));
                createCell(row, 5, "", styles.get("normal"));
                createCell(row, 6, "", styles.get("amount"));
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Sheet 2-3: 开支表 - 按日常和大项分类
     */
    private void createExpenseSheet(Workbook workbook, Map<String, CellStyle> styles, Long familyId, Integer year, String currency) {
        String sheetName = "开支表-" + currency;
        Sheet sheet = workbook.createSheet(sheetName);

        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(year + "年度支出表 (" + currency + ")");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // 空行

        // 日常开支部分 (上半年)
        rowNum = createExpenseHalfYear(sheet, styles, familyId, year, currency, 1, 6, rowNum, "日常开支 - 上半年 (1-6月)", false);

        rowNum += 2; // 空行

        // 日常开支部分 (下半年)
        rowNum = createExpenseHalfYear(sheet, styles, familyId, year, currency, 7, 12, rowNum, "日常开支 - 下半年 (7-12月)", false);

        rowNum += 3; // 空行

        // 大项开支部分 (上半年)
        rowNum = createExpenseHalfYear(sheet, styles, familyId, year, currency, 1, 6, rowNum, "大项开支 - 上半年 (1-6月)", true);

        rowNum += 2; // 空行

        // 大项开支部分 (下半年)
        rowNum = createExpenseHalfYear(sheet, styles, familyId, year, currency, 7, 12, rowNum, "大项开支 - 下半年 (7-12月)", true);

        // 自动调整列宽 (增加了"去年同期"列，共10列)
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建半年支出表
     * @param isMajorItems true=大项开支(住/行/经营), false=日常开支(其他)
     */
    private int createExpenseHalfYear(Sheet sheet, Map<String, CellStyle> styles, Long familyId,
                                     Integer year, String currency, int startMonth, int endMonth,
                                     int rowNum, String title, boolean isMajorItems) {
        // 定义大项支出的code
        Set<String> majorItemCodes = new HashSet<>(Arrays.asList("HOUSING", "TRANSPORTATION", "BUSINESS"));
        // 小标题
        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue(title);
        Font subtitleFont = sheet.getWorkbook().createFont();
        subtitleFont.setBold(true);
        subtitleFont.setFontHeightInPoints((short) 12);
        CellStyle subtitleStyle = sheet.getWorkbook().createCellStyle();
        subtitleStyle.setFont(subtitleFont);
        subtitleCell.setCellStyle(subtitleStyle);

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "大类", styles.get("header"));
        createCell(headerRow, 1, "小类", styles.get("header"));
        createCell(headerRow, 2, "预算", styles.get("header"));
        createCell(headerRow, 3, "去年同期", styles.get("header"));

        int colIdx = 4;
        for (int month = startMonth; month <= endMonth; month++) {
            createCell(headerRow, colIdx++, month + "月", styles.get("header"));
        }
        createCell(headerRow, colIdx++, "实际总计", styles.get("header"));
        createCell(headerRow, colIdx, "差异", styles.get("header"));

        // 获取所有大类
        List<ExpenseCategoryMajor> majorCategories = expenseCategoryMajorRepository.findAll();

        // 遍历每个大类
        for (ExpenseCategoryMajor major : majorCategories) {
            if (major.getId() == 0) continue; // 跳过ID=0的汇总行

            // 根据isMajorItems参数过滤大类
            boolean isMajor = majorItemCodes.contains(major.getCode());
            if (isMajorItems != isMajor) continue; // 大项和日常分开显示

            List<ExpenseCategoryMinor> minorCategories = expenseCategoryMinorRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());

            if (minorCategories.isEmpty()) continue;

            BigDecimal majorBudgetTotal = BigDecimal.ZERO;
            BigDecimal majorActualTotal = BigDecimal.ZERO;
            BigDecimal majorLastYearTotal = BigDecimal.ZERO;

            // 先收集所有小类数据
            List<Map<String, Object>> minorDataList = new ArrayList<>();
            for (ExpenseCategoryMinor minor : minorCategories) {
                // 获取预算
                Optional<ExpenseBudget> budgetOpt = expenseBudgetRepository
                    .findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(familyId, year, minor.getId(), currency);
                BigDecimal budget = budgetOpt.map(ExpenseBudget::getBudgetAmount).orElse(BigDecimal.ZERO);

                // 获取去年同期实际支出总额
                BigDecimal lastYearTotal = BigDecimal.ZERO;
                for (int month = startMonth; month <= endMonth; month++) {
                    String lastYearPeriod = String.format("%d-%02d", year - 1, month);
                    Optional<ExpenseRecord> lastYearRecordOpt = expenseRecordRepository
                        .findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(familyId, lastYearPeriod, minor.getId(), currency);
                    BigDecimal lastYearMonthlyAmount = lastYearRecordOpt.map(ExpenseRecord::getAmount).orElse(BigDecimal.ZERO);
                    lastYearTotal = lastYearTotal.add(lastYearMonthlyAmount);
                }

                // 获取每月实际支出
                BigDecimal[] monthlyActuals = new BigDecimal[endMonth - startMonth + 1];
                BigDecimal actualTotal = BigDecimal.ZERO;

                for (int month = startMonth; month <= endMonth; month++) {
                    String period = String.format("%d-%02d", year, month);
                    Optional<ExpenseRecord> recordOpt = expenseRecordRepository
                        .findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(familyId, period, minor.getId(), currency);
                    BigDecimal monthlyAmount = recordOpt.map(ExpenseRecord::getAmount).orElse(BigDecimal.ZERO);
                    monthlyActuals[month - startMonth] = monthlyAmount;
                    actualTotal = actualTotal.add(monthlyAmount);
                }

                majorBudgetTotal = majorBudgetTotal.add(budget);
                majorActualTotal = majorActualTotal.add(actualTotal);
                majorLastYearTotal = majorLastYearTotal.add(lastYearTotal);

                Map<String, Object> minorData = new HashMap<>();
                minorData.put("minor", minor);
                minorData.put("budget", budget);
                minorData.put("lastYearTotal", lastYearTotal);
                minorData.put("monthlyActuals", monthlyActuals);
                minorData.put("actualTotal", actualTotal);
                minorDataList.add(minorData);
            }

            // 写入大类汇总行
            Row majorRow = sheet.createRow(rowNum++);
            createCell(majorRow, 0, major.getName(), styles.get("total"));
            createCell(majorRow, 1, "小计", styles.get("total"));
            createNumericCell(majorRow, 2, majorBudgetTotal.doubleValue(), styles.get("total"));
            createNumericCell(majorRow, 3, majorLastYearTotal.doubleValue(), styles.get("total"));

            colIdx = 4;
            for (int month = startMonth; month <= endMonth; month++) {
                String period = String.format("%d-%02d", year, month);
                BigDecimal monthTotal = BigDecimal.ZERO;
                for (Map<String, Object> minorData : minorDataList) {
                    BigDecimal[] monthlyActuals = (BigDecimal[]) minorData.get("monthlyActuals");
                    monthTotal = monthTotal.add(monthlyActuals[month - startMonth]);
                }
                createNumericCell(majorRow, colIdx++, monthTotal.doubleValue(), styles.get("total"));
            }
            createNumericCell(majorRow, colIdx++, majorActualTotal.doubleValue(), styles.get("total"));
            createNumericCell(majorRow, colIdx, majorActualTotal.subtract(majorBudgetTotal).doubleValue(), styles.get("total"));

            // 写入小类明细行
            for (Map<String, Object> minorData : minorDataList) {
                ExpenseCategoryMinor minor = (ExpenseCategoryMinor) minorData.get("minor");
                BigDecimal budget = (BigDecimal) minorData.get("budget");
                BigDecimal lastYearTotal = (BigDecimal) minorData.get("lastYearTotal");
                BigDecimal[] monthlyActuals = (BigDecimal[]) minorData.get("monthlyActuals");
                BigDecimal actualTotal = (BigDecimal) minorData.get("actualTotal");

                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "", styles.get("normal"));
                createCell(row, 1, minor.getName(), styles.get("normal"));
                createNumericCell(row, 2, budget.doubleValue(), styles.get("amount"));
                createNumericCell(row, 3, lastYearTotal.doubleValue(), styles.get("amount"));

                colIdx = 4;
                for (BigDecimal monthlyActual : monthlyActuals) {
                    createNumericCell(row, colIdx++, monthlyActual.doubleValue(), styles.get("amount"));
                }
                createNumericCell(row, colIdx++, actualTotal.doubleValue(), styles.get("amount"));
                createNumericCell(row, colIdx, actualTotal.subtract(budget).doubleValue(), styles.get("amount"));
            }
        }

        return rowNum;
    }

    /**
     * Sheet 4: 投资账户明细 - 只包含真正的投资账户(股票投资、数字货币等)，排除401k和IRA
     */
    private void createInvestmentAccountSheet(Workbook workbook, Map<String, CellStyle> styles, Long familyId, Integer year) {
        Sheet sheet = workbook.createSheet("投资账户明细");

        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(year + "年投资账户明细");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // 空行

        // 获取投资类账户：排除现金、房产、退休账户(401k和IRA)
        // 根据 asset_type.is_investment = true 且 tax_status != TAX_DEFERRED
        List<AssetAccount> investmentAccounts = assetAccountRepository
            .findByFamilyIdAndIsActiveTrueAndTaxStatusNot(familyId, RETIREMENT)
            .stream()
            .filter(account -> account.getAssetType() != null &&
                             Boolean.TRUE.equals(account.getAssetType().getIsInvestment()))
            .collect(Collectors.toList());

        if (investmentAccounts.isEmpty()) {
            Row noDataRow = sheet.createRow(rowNum);
            createCell(noDataRow, 0, "暂无投资账户数据", styles.get("normal"));
            return;
        }

        // 表头：日期列 + 各账户列
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "日期", styles.get("header"));
        int colIdx = 1;
        for (AssetAccount account : investmentAccounts) {
            createCell(headerRow, colIdx++, account.getAccountName(), styles.get("header"));
        }
        createCell(headerRow, colIdx, "总计", styles.get("header"));

        // 先添加去年年底数据行
        LocalDate lastYearEnd = LocalDate.of(year - 1, 12, 31);
        Row lastYearRow = sheet.createRow(rowNum++);
        createCell(lastYearRow, 0, lastYearEnd.format(DATE_FORMATTER) + " (去年年底)", styles.get("total"));

        colIdx = 1;
        BigDecimal lastYearTotal = BigDecimal.ZERO;
        for (AssetAccount account : investmentAccounts) {
            Optional<AssetRecord> recordOpt = assetRecordRepository
                .findLatestByAccountAndDate(account.getId(), lastYearEnd);

            if (recordOpt.isPresent()) {
                AssetRecord record = recordOpt.get();
                BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), lastYearEnd);
                createNumericCell(lastYearRow, colIdx, amountUSD.doubleValue(), styles.get("total"));
                lastYearTotal = lastYearTotal.add(amountUSD);
            } else {
                createNumericCell(lastYearRow, colIdx, 0.0, styles.get("total"));
            }
            colIdx++;
        }
        createNumericCell(lastYearRow, colIdx, lastYearTotal.doubleValue(), styles.get("total"));

        // 获取该年度所有日期的记录
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 收集所有记录日期
        Set<LocalDate> allDates = new TreeSet<>();
        for (AssetAccount account : investmentAccounts) {
            List<AssetRecord> records = assetRecordRepository
                .findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(account.getId(), startDate, endDate);
            records.forEach(r -> allDates.add(r.getRecordDate()));
        }

        // 按日期写入数据
        for (LocalDate date : allDates) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, date.format(DATE_FORMATTER), styles.get("normal"));

            colIdx = 1;
            BigDecimal dateTotal = BigDecimal.ZERO;

            for (AssetAccount account : investmentAccounts) {
                Optional<AssetRecord> recordOpt = assetRecordRepository
                    .findLatestByAccountAndDate(account.getId(), date);

                if (recordOpt.isPresent()) {
                    AssetRecord record = recordOpt.get();
                    BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), date);
                    createNumericCell(row, colIdx, amountUSD.doubleValue(), styles.get("amount"));
                    dateTotal = dateTotal.add(amountUSD);
                } else {
                    createCell(row, colIdx, "", styles.get("amount"));
                }
                colIdx++;
            }

            createNumericCell(row, colIdx, dateTotal.doubleValue(), styles.get("total"));
        }

        // 自动调整列宽
        for (int i = 0; i <= investmentAccounts.size() + 1; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Sheet 5: 退休账户明细 - 包含所有税收递延账户(401k, Roth IRA等)
     */
    private void createRetirementAccountSheet(Workbook workbook, Map<String, CellStyle> styles, Long familyId, Integer year) {
        Sheet sheet = workbook.createSheet("退休账户明细");

        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(year + "年退休账户明细");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // 空行

        // 获取所有退休账户(TaxStatus = TAX_DEFERRED)
        // 包括401k、Roth IRA、Traditional IRA等所有税收递延账户
        List<AssetAccount> retirementAccounts = assetAccountRepository
            .findByFamilyIdAndIsActiveTrueAndTaxStatus(familyId, RETIREMENT);

        if (retirementAccounts.isEmpty()) {
            Row noDataRow = sheet.createRow(rowNum);
            createCell(noDataRow, 0, "暂无退休账户数据", styles.get("normal"));
            return;
        }

        // 表头：日期列 + 各账户列
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "日期", styles.get("header"));
        int colIdx = 1;
        for (AssetAccount account : retirementAccounts) {
            createCell(headerRow, colIdx++, account.getAccountName(), styles.get("header"));
        }
        createCell(headerRow, colIdx, "总计", styles.get("header"));

        // 先添加去年年底数据行
        LocalDate lastYearEnd = LocalDate.of(year - 1, 12, 31);
        Row lastYearRow = sheet.createRow(rowNum++);
        createCell(lastYearRow, 0, lastYearEnd.format(DATE_FORMATTER) + " (去年年底)", styles.get("total"));

        colIdx = 1;
        BigDecimal lastYearTotal = BigDecimal.ZERO;
        for (AssetAccount account : retirementAccounts) {
            Optional<AssetRecord> recordOpt = assetRecordRepository
                .findLatestByAccountAndDate(account.getId(), lastYearEnd);

            if (recordOpt.isPresent()) {
                AssetRecord record = recordOpt.get();
                BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), lastYearEnd);
                createNumericCell(lastYearRow, colIdx, amountUSD.doubleValue(), styles.get("total"));
                lastYearTotal = lastYearTotal.add(amountUSD);
            } else {
                createNumericCell(lastYearRow, colIdx, 0.0, styles.get("total"));
            }
            colIdx++;
        }
        createNumericCell(lastYearRow, colIdx, lastYearTotal.doubleValue(), styles.get("total"));

        // 获取该年度所有日期的记录
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 收集所有记录日期
        Set<LocalDate> allDates = new TreeSet<>();
        for (AssetAccount account : retirementAccounts) {
            List<AssetRecord> records = assetRecordRepository
                .findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(account.getId(), startDate, endDate);
            records.forEach(r -> allDates.add(r.getRecordDate()));
        }

        // 按日期写入数据
        for (LocalDate date : allDates) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, date.format(DATE_FORMATTER), styles.get("normal"));

            colIdx = 1;
            BigDecimal dateTotal = BigDecimal.ZERO;

            for (AssetAccount account : retirementAccounts) {
                Optional<AssetRecord> recordOpt = assetRecordRepository
                    .findLatestByAccountAndDate(account.getId(), date);

                if (recordOpt.isPresent()) {
                    AssetRecord record = recordOpt.get();
                    BigDecimal amountUSD = convertToUSD(record.getAmount(), record.getCurrency(), date);
                    createNumericCell(row, colIdx, amountUSD.doubleValue(), styles.get("amount"));
                    dateTotal = dateTotal.add(amountUSD);
                } else {
                    createCell(row, colIdx, "", styles.get("amount"));
                }
                colIdx++;
            }

            createNumericCell(row, colIdx, dateTotal.doubleValue(), styles.get("total"));
        }

        // 自动调整列宽
        for (int i = 0; i <= retirementAccounts.size() + 1; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 货币转换为USD
     */
    private BigDecimal convertToUSD(BigDecimal amount, String currency, LocalDate date) {
        if ("USD".equals(currency)) {
            return amount;
        }
        // getExchangeRate returns USD per 1 unit of currency
        BigDecimal rate = exchangeRateService.getExchangeRate(currency, date);
        return amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 创建文本单元格
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    /**
     * 创建数值单元格
     */
    private void createNumericCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
