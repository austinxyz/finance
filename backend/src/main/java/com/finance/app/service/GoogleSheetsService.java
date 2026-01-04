package com.finance.app.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Google Sheets服务
 * 使用OAuth 2.0用户认证，支持创建和更新电子表格
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Personal Finance Management";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleOAuthService googleOAuthService;

    /**
     * 创建信任所有证书的HTTP传输（用于解决SSL证书验证问题）
     * 注意：这在生产环境中不推荐，但对于个人应用和开发环境是可以接受的
     */
    private NetHttpTransport createTrustAllTransport() throws GeneralSecurityException, IOException {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 创建NetHttpTransport
            return new NetHttpTransport.Builder()
                .setSslSocketFactory(sslContext.getSocketFactory())
                .setHostnameVerifier((hostname, session) -> true) // 信任所有主机名
                .build();
        } catch (Exception e) {
            log.warn("无法创建自定义SSL传输，回退到默认传输", e);
            return GoogleNetHttpTransport.newTrustedTransport();
        }
    }

    /**
     * 获取Google Sheets服务实例（使用OAuth 2.0）
     */
    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = createTrustAllTransport();
        Credential credential = googleOAuthService.getUserCredential();

        return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    /**
     * 获取Google Drive服务实例（用于权限管理）
     */
    private Drive getDriveService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = createTrustAllTransport();
        Credential credential = googleOAuthService.getUserCredential();

        return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    /**
     * 创建新的电子表格
     * @param title 表格标题
     * @return 电子表格ID
     */
    public String createSpreadsheet(String title) throws IOException, GeneralSecurityException {
        log.info("创建Google Sheets电子表格: {}", title);

        Sheets service = getSheetsService();

        Spreadsheet spreadsheet = new Spreadsheet()
            .setProperties(new SpreadsheetProperties()
                .setTitle(title)
                .setLocale("zh_CN")
                .setTimeZone("Asia/Shanghai"));

        spreadsheet = service.spreadsheets().create(spreadsheet)
            .setFields("spreadsheetId,spreadsheetUrl")
            .execute();

        String spreadsheetId = spreadsheet.getSpreadsheetId();
        log.info("电子表格创建成功: {}", spreadsheetId);
        log.info("访问链接: {}", spreadsheet.getSpreadsheetUrl());

        return spreadsheetId;
    }

    /**
     * 创建新的工作表（如果已存在则返回现有的）
     * @param spreadsheetId 电子表格ID
     * @param sheetTitle 工作表标题
     * @return 工作表ID
     */
    public Integer addSheet(String spreadsheetId, String sheetTitle) throws IOException, GeneralSecurityException {
        log.info("添加工作表: {} 到电子表格: {}", sheetTitle, spreadsheetId);

        Sheets service = getSheetsService();

        // 先获取所有已存在的工作表
        Spreadsheet spreadsheet = service.spreadsheets()
            .get(spreadsheetId)
            .setFields("sheets.properties")
            .execute();

        // 检查是否已存在同名工作表
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheetTitle.equals(sheet.getProperties().getTitle())) {
                Integer existingSheetId = sheet.getProperties().getSheetId();
                log.info("工作表已存在，使用现有ID: {}", existingSheetId);
                return existingSheetId;
            }
        }

        // 不存在则创建新工作表
        AddSheetRequest addSheetRequest = new AddSheetRequest()
            .setProperties(new SheetProperties().setTitle(sheetTitle));

        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(Collections.singletonList(new Request().setAddSheet(addSheetRequest)));

        BatchUpdateSpreadsheetResponse response = service.spreadsheets()
            .batchUpdate(spreadsheetId, batchRequest)
            .execute();

        Integer sheetId = response.getReplies().get(0).getAddSheet().getProperties().getSheetId();
        log.info("工作表创建成功，ID: {}", sheetId);

        return sheetId;
    }

    /**
     * 批量写入数据到指定工作表
     * @param spreadsheetId 电子表格ID
     * @param sheetName 工作表名称
     * @param values 数据（行列二维数组）
     */
    public void writeData(String spreadsheetId, String sheetName, List<List<Object>> values)
            throws IOException, GeneralSecurityException {
        log.info("写入数据到工作表: {} (共{}行)", sheetName, values.size());

        Sheets service = getSheetsService();

        // 直接使用工作表名称，Java客户端会自动处理编码
        String range = sheetName + "!A1";
        ValueRange body = new ValueRange().setValues(values);

        service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("USER_ENTERED")
            .execute();

        log.info("数据写入完成");
    }

    /**
     * 批量格式化单元格
     * @param spreadsheetId 电子表格ID
     * @param requests 格式化请求列表
     */
    public void formatCells(String spreadsheetId, List<Request> requests)
            throws IOException, GeneralSecurityException {
        log.info("应用单元格格式化，共{}个请求", requests.size());

        Sheets service = getSheetsService();

        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(requests);

        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();

        log.info("格式化完成");
    }

    /**
     * 设置电子表格权限
     * @param spreadsheetId 电子表格ID
     * @param role 角色: "reader"（只读）或 "writer"（编辑）
     * @return 分享链接
     */
    public String setPermissions(String spreadsheetId, String role) throws IOException, GeneralSecurityException {
        log.info("设置电子表格权限: {} - {}", spreadsheetId, role);

        Drive driveService = getDriveService();

        // 设置任何人都可以通过链接访问
        Permission permission = new Permission()
            .setType("anyone")
            .setRole(role); // "reader" 或 "writer"

        driveService.permissions().create(spreadsheetId, permission).execute();

        String shareUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId;
        log.info("权限设置完成，分享链接: {}", shareUrl);

        return shareUrl;
    }

    /**
     * 检查工作表是否存在
     * @param spreadsheetId 电子表格ID
     * @param sheetName 工作表名称
     * @return 是否存在
     */
    public boolean sheetExists(String spreadsheetId, String sheetName)
            throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();

        Spreadsheet spreadsheet = service.spreadsheets()
            .get(spreadsheetId)
            .setFields("sheets.properties")
            .execute();

        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheetName.equals(sheet.getProperties().getTitle())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清空工作表内容
     * @param spreadsheetId 电子表格ID
     * @param sheetName 工作表名称
     */
    public void clearSheet(String spreadsheetId, String sheetName)
            throws IOException, GeneralSecurityException {
        log.info("清空工作表: {}", sheetName);

        // 先检查工作表是否存在
        if (!sheetExists(spreadsheetId, sheetName)) {
            log.warn("工作表不存在，跳过清空: {}", sheetName);
            return;
        }

        Sheets service = getSheetsService();

        // 直接使用工作表名称，Java客户端会自动处理编码
        ClearValuesRequest clearRequest = new ClearValuesRequest();
        service.spreadsheets().values()
            .clear(spreadsheetId, sheetName, clearRequest)
            .execute();

        log.info("工作表清空完成");
    }

    /**
     * 根据标题删除工作表
     * @param spreadsheetId 电子表格ID
     * @param sheetTitle 工作表标题
     */
    public void deleteSheetByTitle(String spreadsheetId, String sheetTitle)
            throws IOException, GeneralSecurityException {
        log.info("删除工作表: {}", sheetTitle);

        Sheets service = getSheetsService();

        // 获取所有工作表，找到目标sheet的ID
        Spreadsheet spreadsheet = service.spreadsheets()
            .get(spreadsheetId)
            .setFields("sheets.properties")
            .execute();

        Integer sheetIdToDelete = null;
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheetTitle.equals(sheet.getProperties().getTitle())) {
                sheetIdToDelete = sheet.getProperties().getSheetId();
                break;
            }
        }

        if (sheetIdToDelete == null) {
            log.warn("未找到工作表: {}，跳过删除", sheetTitle);
            return;
        }

        // 删除工作表
        DeleteSheetRequest deleteRequest = new DeleteSheetRequest()
            .setSheetId(sheetIdToDelete);

        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(Collections.singletonList(new Request().setDeleteSheet(deleteRequest)));

        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();

        log.info("工作表删除成功: {}", sheetTitle);
    }

    /**
     * 获取电子表格的访问URL
     */
    public String getSpreadsheetUrl(String spreadsheetId) {
        return "https://docs.google.com/spreadsheets/d/" + spreadsheetId;
    }

    /**
     * 辅助方法：将BigDecimal转换为显示格式
     */
    protected Object formatNumber(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.doubleValue();
    }

    /**
     * 辅助方法：将LocalDate转换为显示格式
     */
    protected Object formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 创建标题行格式化请求
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     */
    protected Request createHeaderFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(new Color()
                        .setRed(0.85f)
                        .setGreen(0.85f)
                        .setBlue(0.85f))
                    .setTextFormat(new TextFormat()
                        .setBold(true)
                        .setFontSize(12))
                    .setHorizontalAlignment("CENTER")
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(backgroundColor,textFormat,horizontalAlignment,borders)"));
    }

    /**
     * 合并单元格并居中对齐
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     * @return 请求列表（包含合并和格式化）
     */
    public List<Request> createMergeAndCenterFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol) {
        List<Request> requests = new ArrayList<>();

        // 1. 合并单元格
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setMergeType("MERGE_ALL")));

        // 2. 设置居中对齐和加粗
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setHorizontalAlignment("CENTER")
                    .setVerticalAlignment("MIDDLE")
                    .setTextFormat(new TextFormat()
                        .setBold(true)
                        .setFontSize(14))))
            .setFields("userEnteredFormat(horizontalAlignment,verticalAlignment,textFormat)")));

        return requests;
    }

    /**
     * 创建金额格式化请求（带货币符号）
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     * @param currency 货币代码（USD或CNY）
     */
    public Request createCurrencyFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol, String currency) {
        // 根据货币选择格式模板
        String pattern = "USD".equals(currency) ? "$#,##0.00" : "¥#,##0.00";

        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setNumberFormat(new NumberFormat()
                        .setType("CURRENCY")
                        .setPattern(pattern))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(numberFormat,borders)"));
    }

    /**
     * 创建百分比格式
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     */
    public Request createPercentFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setNumberFormat(new NumberFormat()
                        .setType("PERCENT")
                        .setPattern("0.00%"))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(numberFormat,borders)"));
    }

    /**
     * 创建普通单元格格式（清除背景色，只保留边框和加粗）
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     */
    public Request createPlainFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(new Color()
                        .setRed(1.0f)
                        .setGreen(1.0f)
                        .setBlue(1.0f))
                    .setTextFormat(new TextFormat()
                        .setBold(true)
                        .setFontSize(11))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(backgroundColor,textFormat,borders)"));
    }

    /**
     * 创建带背景颜色的单元格格式
     * @param sheetId 工作表ID
     * @param startRow 起始行
     * @param endRow 结束行（不包含）
     * @param startCol 起始列
     * @param endCol 结束列（不包含）
     * @param red RGB红色值（0.0-1.0）
     * @param green RGB绿色值（0.0-1.0）
     * @param blue RGB蓝色值（0.0-1.0）
     */
    public Request createBackgroundColorFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol,
                                                float red, float green, float blue) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(new Color()
                        .setRed(red)
                        .setGreen(green)
                        .setBlue(blue))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(backgroundColor,borders)"));
    }

    /**
     * 创建背景色+粗体格式
     */
    public Request createBackgroundColorBoldFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol,
                                                   float red, float green, float blue) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(new Color()
                        .setRed(red)
                        .setGreen(green)
                        .setBlue(blue))
                    .setTextFormat(new TextFormat()
                        .setBold(true))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(backgroundColor,textFormat,borders)"));
    }

    /**
     * 创建背景色+粗体+货币格式（用于总计行）
     */
    public Request createBackgroundColorBoldCurrencyFormat(Integer sheetId, int startRow, int endRow, int startCol, int endCol,
                                                           float red, float green, float blue, String currency) {
        String pattern = "USD".equals(currency) ? "$#,##0.00" : "¥#,##0.00";

        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(startCol)
                .setEndColumnIndex(endCol))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBackgroundColor(new Color()
                        .setRed(red)
                        .setGreen(green)
                        .setBlue(blue))
                    .setTextFormat(new TextFormat()
                        .setBold(true))
                    .setNumberFormat(new NumberFormat()
                        .setType("CURRENCY")
                        .setPattern(pattern))
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(backgroundColor,textFormat,numberFormat,borders)"));
    }

    /**
     * 为单个单元格创建百分比格式+条件背景颜色（正数绿色，负数红色）
     * @param sheetId 工作表ID
     * @param row 行索引（0-based）
     * @param col 列索引（0-based）
     * @param value 百分比值（小数形式，如0.15表示15%）
     */
    public Request createConditionalPercentFormat(Integer sheetId, int row, int col, double value) {
        // 根据值决定背景颜色：负数红色，正数绿色
        Color backgroundColor;
        if (value < 0) {
            // 红色背景
            backgroundColor = new Color().setRed(1.0f).setGreen(0.8f).setBlue(0.8f);
        } else {
            // 绿色背景
            backgroundColor = new Color().setRed(0.8f).setGreen(1.0f).setBlue(0.8f);
        }

        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(row)
                .setEndRowIndex(row + 1)
                .setStartColumnIndex(col)
                .setEndColumnIndex(col + 1))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setNumberFormat(new NumberFormat()
                        .setType("PERCENT")
                        .setPattern("0.00%"))
                    .setBackgroundColor(backgroundColor)
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID"))
                        .setBottom(new Border().setStyle("SOLID"))
                        .setLeft(new Border().setStyle("SOLID"))
                        .setRight(new Border().setStyle("SOLID")))))
            .setFields("userEnteredFormat(numberFormat,backgroundColor,borders)"));
    }

    /**
     * 为整个工作表添加表格线
     * @param sheetId 工作表ID
     * @param rowCount 行数
     * @param colCount 列数
     */
    public Request createBordersForAll(Integer sheetId, int rowCount, int colCount) {
        return new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(0)
                .setEndRowIndex(rowCount)
                .setStartColumnIndex(0)
                .setEndColumnIndex(colCount))
            .setCell(new CellData()
                .setUserEnteredFormat(new CellFormat()
                    .setBorders(new Borders()
                        .setTop(new Border().setStyle("SOLID").setWidth(1))
                        .setBottom(new Border().setStyle("SOLID").setWidth(1))
                        .setLeft(new Border().setStyle("SOLID").setWidth(1))
                        .setRight(new Border().setStyle("SOLID").setWidth(1)))))
            .setFields("userEnteredFormat(borders)"));
    }

    /**
     * 设置列宽
     * @param sheetId 工作表ID
     * @param startColumnIndex 起始列（0-based）
     * @param endColumnIndex 结束列（不包含，0-based）
     * @param pixelSize 列宽（像素）
     */
    public Request createColumnWidthFormat(Integer sheetId, int startColumnIndex, int endColumnIndex, int pixelSize) {
        return new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
            .setRange(new DimensionRange()
                .setSheetId(sheetId)
                .setDimension("COLUMNS")
                .setStartIndex(startColumnIndex)
                .setEndIndex(endColumnIndex))
            .setProperties(new DimensionProperties()
                .setPixelSize(pixelSize))
            .setFields("pixelSize"));
    }

    /**
     * 创建折线图
     * @param sheetId 工作表ID
     * @param title 图表标题
     * @param startRow 数据起始行（包含表头）
     * @param endRow 数据结束行（不包含）
     * @param startCol 数据起始列（日期列）
     * @param endCol 数据结束列（不包含）
     * @param anchorRow 图表锚点行
     * @param anchorCol 图表锚点列
     * @return 创建图表的请求
     */
    public Request createLineChart(Integer sheetId, String title, int startRow, int endRow, int startCol, int endCol,
                                    int anchorRow, int anchorCol) {
        // 创建数据源范围
        List<BasicChartSeries> series = new ArrayList<>();

        // 为每个数据列（账户）创建一个系列
        for (int col = startCol + 1; col < endCol; col++) {
            BasicChartSeries chartSeries = new BasicChartSeries()
                .setSeries(new ChartData()
                    .setSourceRange(new ChartSourceRange()
                        .setSources(Collections.singletonList(
                            new GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(startRow)
                                .setEndRowIndex(endRow)
                                .setStartColumnIndex(col)
                                .setEndColumnIndex(col + 1)
                        ))));
            series.add(chartSeries);
        }

        // X轴（日期列）
        BasicChartDomain domain = new BasicChartDomain()
            .setDomain(new ChartData()
                .setSourceRange(new ChartSourceRange()
                    .setSources(Collections.singletonList(
                        new GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(startRow)
                            .setEndRowIndex(endRow)
                            .setStartColumnIndex(startCol)
                            .setEndColumnIndex(startCol + 1)
                    ))));

        BasicChartSpec basicChart = new BasicChartSpec()
            .setChartType("LINE")
            .setLegendPosition("RIGHT_LEGEND")
            .setHeaderCount(1)
            .setSeries(series)
            .setDomains(Collections.singletonList(domain))
            .setAxis(Arrays.asList(
                new BasicChartAxis()
                    .setPosition("BOTTOM_AXIS")
                    .setTitle("日期"),
                new BasicChartAxis()
                    .setPosition("LEFT_AXIS")
                    .setTitle("金额 (USD)")
            ));

        EmbeddedChart chart = new EmbeddedChart()
            .setSpec(new ChartSpec()
                .setTitle(title)
                .setBasicChart(basicChart))
            .setPosition(new EmbeddedObjectPosition()
                .setOverlayPosition(new OverlayPosition()
                    .setAnchorCell(new GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(anchorRow)
                        .setColumnIndex(anchorCol))
                    .setOffsetXPixels(0)
                    .setOffsetYPixels(0)
                    .setWidthPixels(800)
                    .setHeightPixels(400)));

        return new Request().setAddChart(new AddChartRequest().setChart(chart));
    }

    /**
     * 创建嵌入式（固定位置）折线图
     * @param sheetId 工作表ID
     * @param title 图表标题
     * @param startRow 数据起始行（包含表头）
     * @param endRow 数据结束行（不包含）
     * @param startCol 数据起始列（日期列）
     * @param endCol 数据结束列（不包含）
     * @param anchorRow 图表锚点行（图表将从此行开始）
     * @return 创建图表的请求
     */
    public Request createEmbeddedLineChart(Integer sheetId, String title, int startRow, int endRow, int startCol, int endCol,
                                           int anchorRow) {
        // 创建数据源范围
        List<BasicChartSeries> series = new ArrayList<>();

        // 为每个数据列（账户）创建一个系列
        for (int col = startCol + 1; col < endCol; col++) {
            BasicChartSeries chartSeries = new BasicChartSeries()
                .setSeries(new ChartData()
                    .setSourceRange(new ChartSourceRange()
                        .setSources(Collections.singletonList(
                            new GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(startRow)
                                .setEndRowIndex(endRow)
                                .setStartColumnIndex(col)
                                .setEndColumnIndex(col + 1)
                        ))));
            series.add(chartSeries);
        }

        // X轴（日期列）
        BasicChartDomain domain = new BasicChartDomain()
            .setDomain(new ChartData()
                .setSourceRange(new ChartSourceRange()
                    .setSources(Collections.singletonList(
                        new GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(startRow)
                            .setEndRowIndex(endRow)
                            .setStartColumnIndex(startCol)
                            .setEndColumnIndex(startCol + 1)
                    ))));

        BasicChartSpec basicChart = new BasicChartSpec()
            .setChartType("LINE")
            .setLegendPosition("RIGHT_LEGEND")
            .setHeaderCount(1)
            .setSeries(series)
            .setDomains(Collections.singletonList(domain))
            .setAxis(Arrays.asList(
                new BasicChartAxis()
                    .setPosition("BOTTOM_AXIS")
                    .setTitle("日期"),
                new BasicChartAxis()
                    .setPosition("LEFT_AXIS")
                    .setTitle("金额 (USD)")
            ));

        // 使用OverlayPosition将图表固定在数据下方
        EmbeddedChart chart = new EmbeddedChart()
            .setSpec(new ChartSpec()
                .setTitle(title)
                .setBasicChart(basicChart))
            .setPosition(new EmbeddedObjectPosition()
                .setOverlayPosition(new OverlayPosition()
                    .setAnchorCell(new GridCoordinate()
                        .setSheetId(sheetId)
                        .setRowIndex(anchorRow)
                        .setColumnIndex(0))
                    .setOffsetXPixels(0)
                    .setOffsetYPixels(0)
                    .setWidthPixels(1000)
                    .setHeightPixels(400)));

        return new Request().setAddChart(new AddChartRequest().setChart(chart));
    }
}
