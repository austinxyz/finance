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
     * Sheet 1: 资产负债表 - 资产和负债并排显示
     */
    private void createBalanceSheet(Workbook workbook, Map<String, CellStyle> styles, Long familyId, Integer year) {
        Sheet sheet = workbook.createSheet("资产负债表");
        LocalDate asOfDate = LocalDate.of(year, 12, 31);

        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(year + "年资产负债表 (截至 " + asOfDate.format(DATE_FORMATTER) + ")");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // 空行

        // 获取数据
        List<AssetType> assetTypes = assetTypeRepository.findAll();
        List<LiabilityType> liabilityTypes = liabilityTypeRepository.findAll();

        // 计算资产数据
        BigDecimal totalAssets = BigDecimal.ZERO;
        Map<Long, BigDecimal> assetTypeAmounts = new HashMap<>();
        for (AssetType assetType : assetTypes) {
            List<AssetRecord> records = assetRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate)
                .stream()
                .filter(r -> r.getAccount().getAssetType().getId().equals(assetType.getId()))
                .collect(Collectors.toList());

            BigDecimal typeTotal = records.stream()
                .map(r -> convertToUSD(r.getAmount(), r.getCurrency(), asOfDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (typeTotal.compareTo(BigDecimal.ZERO) > 0) {
                assetTypeAmounts.put(assetType.getId(), typeTotal);
                totalAssets = totalAssets.add(typeTotal);
            }
        }

        // 计算负债数据
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        Map<Long, BigDecimal> liabilityTypeAmounts = new HashMap<>();
        for (LiabilityType liabilityType : liabilityTypes) {
            List<LiabilityRecord> records = liabilityRecordRepository.findLatestRecordsByFamilyAndDate(familyId, asOfDate)
                .stream()
                .filter(r -> r.getAccount().getLiabilityType().getId().equals(liabilityType.getId()))
                .collect(Collectors.toList());

            BigDecimal typeTotal = records.stream()
                .map(r -> convertToUSD(r.getOutstandingBalance(), r.getCurrency(), asOfDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (typeTotal.compareTo(BigDecimal.ZERO) > 0) {
                liabilityTypeAmounts.put(liabilityType.getId(), typeTotal);
                totalLiabilities = totalLiabilities.add(typeTotal);
            }
        }

        // 表头：资产 | 金额 | 占比 || 负债 | 金额 | 占比
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "资产类型", styles.get("header"));
        createCell(headerRow, 1, "金额 (USD)", styles.get("header"));
        createCell(headerRow, 2, "占比", styles.get("header"));
        createCell(headerRow, 3, "", styles.get("header")); // 分隔列
        createCell(headerRow, 4, "负债类型", styles.get("header"));
        createCell(headerRow, 5, "金额 (USD)", styles.get("header"));
        createCell(headerRow, 6, "占比", styles.get("header"));

        // 并排写入资产和负债
        int maxRows = Math.max(assetTypes.size(), liabilityTypes.size());
        List<AssetType> assetList = assetTypes.stream()
            .filter(at -> assetTypeAmounts.containsKey(at.getId()))
            .collect(Collectors.toList());
        List<LiabilityType> liabilityList = liabilityTypes.stream()
            .filter(lt -> liabilityTypeAmounts.containsKey(lt.getId()))
            .collect(Collectors.toList());

        int rows = Math.max(assetList.size(), liabilityList.size());
        for (int i = 0; i < rows; i++) {
            Row row = sheet.createRow(rowNum++);

            // 资产列
            if (i < assetList.size()) {
                AssetType assetType = assetList.get(i);
                BigDecimal amount = assetTypeAmounts.get(assetType.getId());
                createCell(row, 0, assetType.getChineseName(), styles.get("normal"));
                createNumericCell(row, 1, amount.doubleValue(), styles.get("amount"));
                if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
                    double percentage = amount.divide(totalAssets, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100")).doubleValue();
                    createCell(row, 2, String.format("%.2f%%", percentage), styles.get("normal"));
                }
            } else {
                createCell(row, 0, "", styles.get("normal"));
                createCell(row, 1, "", styles.get("amount"));
                createCell(row, 2, "", styles.get("normal"));
            }

            createCell(row, 3, "", styles.get("normal")); // 分隔列

            // 负债列
            if (i < liabilityList.size()) {
                LiabilityType liabilityType = liabilityList.get(i);
                BigDecimal amount = liabilityTypeAmounts.get(liabilityType.getId());
                createCell(row, 4, liabilityType.getChineseName(), styles.get("normal"));
                createNumericCell(row, 5, amount.doubleValue(), styles.get("amount"));
                if (totalLiabilities.compareTo(BigDecimal.ZERO) > 0) {
                    double percentage = amount.divide(totalLiabilities, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100")).doubleValue();
                    createCell(row, 6, String.format("%.2f%%", percentage), styles.get("normal"));
                }
            } else {
                createCell(row, 4, "", styles.get("normal"));
                createCell(row, 5, "", styles.get("amount"));
                createCell(row, 6, "", styles.get("normal"));
            }
        }

        // 总计行
        Row totalRow = sheet.createRow(rowNum++);
        createCell(totalRow, 0, "资产总计", styles.get("total"));
        createNumericCell(totalRow, 1, totalAssets.doubleValue(), styles.get("total"));
        createCell(totalRow, 2, "100.00%", styles.get("total"));
        createCell(totalRow, 3, "", styles.get("total"));
        createCell(totalRow, 4, "负债总计", styles.get("total"));
        createNumericCell(totalRow, 5, totalLiabilities.doubleValue(), styles.get("total"));
        createCell(totalRow, 6, "100.00%", styles.get("total"));

        rowNum++; // 空行

        // 净资产
        Row netWorthRow = sheet.createRow(rowNum++);
        createCell(netWorthRow, 0, "净资产", styles.get("total"));
        createNumericCell(netWorthRow, 1, totalAssets.subtract(totalLiabilities).doubleValue(), styles.get("total"));

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

        // 自动调整列宽
        for (int i = 0; i < 9; i++) {
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

        int colIdx = 3;
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

            // 先收集所有小类数据
            List<Map<String, Object>> minorDataList = new ArrayList<>();
            for (ExpenseCategoryMinor minor : minorCategories) {
                // 获取预算
                Optional<ExpenseBudget> budgetOpt = expenseBudgetRepository
                    .findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(familyId, year, minor.getId(), currency);
                BigDecimal budget = budgetOpt.map(ExpenseBudget::getBudgetAmount).orElse(BigDecimal.ZERO);

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

                Map<String, Object> minorData = new HashMap<>();
                minorData.put("minor", minor);
                minorData.put("budget", budget);
                minorData.put("monthlyActuals", monthlyActuals);
                minorData.put("actualTotal", actualTotal);
                minorDataList.add(minorData);
            }

            // 写入大类汇总行
            Row majorRow = sheet.createRow(rowNum++);
            createCell(majorRow, 0, major.getName(), styles.get("total"));
            createCell(majorRow, 1, "小计", styles.get("total"));
            createNumericCell(majorRow, 2, majorBudgetTotal.doubleValue(), styles.get("total"));

            colIdx = 3;
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
                BigDecimal[] monthlyActuals = (BigDecimal[]) minorData.get("monthlyActuals");
                BigDecimal actualTotal = (BigDecimal) minorData.get("actualTotal");

                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "", styles.get("normal"));
                createCell(row, 1, minor.getName(), styles.get("normal"));
                createNumericCell(row, 2, budget.doubleValue(), styles.get("amount"));

                colIdx = 3;
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
     * Sheet 4: 投资账户明细 - 只包含真正的投资账户(股票投资、数字货币等)
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

        // 获取投资类账户：排除现金、房产、退休账户
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
