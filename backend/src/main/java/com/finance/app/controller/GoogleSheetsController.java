package com.finance.app.controller;

import com.finance.app.service.GoogleSheetsExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Sheets同步控制器
 * 提供财务报表同步到Google Sheets的API
 */
@RestController
@RequestMapping("/google-sheets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Google Sheets", description = "Google Sheets同步API")
public class GoogleSheetsController {

    private final GoogleSheetsExportService googleSheetsExportService;

    /**
     * 同步年度财务报表到Google Sheets
     * @param familyId 家庭ID
     * @param year 年份
     * @param permission 权限设置：reader（只读）或writer（可编辑），默认reader
     * @return 分享链接和电子表格ID
     */
    @PostMapping("/sync-annual-report")
    @Operation(summary = "同步年度财务报表到Google Sheets",
               description = "将指定年份的财务报表导出到Google Sheets，并返回分享链接。每次调用都会创建新的电子表格。")
    public ResponseEntity<Map<String, Object>> syncAnnualReport(
            @Parameter(description = "家庭ID", required = true)
            @RequestParam Long familyId,

            @Parameter(description = "年份", required = true, example = "2024")
            @RequestParam Integer year,

            @Parameter(description = "权限设置：reader（只读）或writer（可编辑）", example = "reader")
            @RequestParam(defaultValue = "reader") String permission) {

        try {
            log.info("开始同步年度报表到Google Sheets: familyId={}, year={}, permission={}",
                familyId, year, permission);

            // 验证权限参数
            if (!permission.equals("reader") && !permission.equals("writer")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "无效的权限参数，只能是reader或writer");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 执行同步
            Map<String, Object> syncResult = googleSheetsExportService.createOrUpdateAnnualReport(familyId, year, permission);

            String shareUrl = (String) syncResult.get("shareUrl");
            String spreadsheetId = (String) syncResult.get("spreadsheetId");
            boolean isNew = (Boolean) syncResult.get("isNew");

            Map<String, String> data = new HashMap<>();
            data.put("spreadsheetId", spreadsheetId);
            data.put("shareUrl", shareUrl);
            data.put("permission", permission);
            data.put("isNew", String.valueOf(isNew));
            data.put("message", isNew ? "报表已成功创建并同步到Google Sheets" : "报表已更新，最新数据已同步到Google Sheets");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            log.info("年度报表同步完成: {}", shareUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("IO错误：无法访问Google Sheets API", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "IO错误：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);

        } catch (GeneralSecurityException e) {
            log.error("安全错误：Google API认证失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "认证失败：" + e.getMessage());
            errorResponse.put("hint", "请检查Service Account凭证文件是否正确配置");
            return ResponseEntity.status(401).body(errorResponse);

        } catch (Exception e) {
            log.error("同步失败：未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "同步失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 从分享链接提取spreadsheetId
     */
    private String extractSpreadsheetId(String shareUrl) {
        // URL格式: https://docs.google.com/spreadsheets/d/{spreadsheetId}
        String[] parts = shareUrl.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("d") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "";
    }

    /**
     * 测试Google Sheets连接
     * @return 连接状态
     */
    @GetMapping("/test-connection")
    @Operation(summary = "测试Google Sheets连接",
               description = "测试Service Account凭证是否正确配置，能否成功连接到Google Sheets API")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            log.info("测试Google Sheets API连接 - 真实API调用");

            // 真实测试：创建一个测试电子表格
            String testSpreadsheetId = googleSheetsExportService.testConnection();

            Map<String, String> data = new HashMap<>();
            data.put("status", "success");
            data.put("message", "Google Sheets API连接正常，已成功创建测试表格");
            data.put("testSpreadsheetId", testSpreadsheetId);
            data.put("testUrl", "https://docs.google.com/spreadsheets/d/" + testSpreadsheetId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            log.info("连接测试成功，测试表格ID: {}", testSpreadsheetId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("连接测试失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "连接失败：" + e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            errorResponse.put("hint", "请检查：1) Google Sheets API已启用 2) 已关联计费账户 3) google-credentials.json正确配置");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
