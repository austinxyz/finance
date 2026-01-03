package com.finance.app.controller;

import com.finance.app.model.GoogleSheetsSync;
import com.finance.app.repository.GoogleSheetsSyncRepository;
import com.finance.app.service.GoogleSheetsExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private final GoogleSheetsSyncRepository googleSheetsSyncRepository;

    /**
     * 同步年度财务报表到Google Sheets
     * @param familyId 家庭ID
     * @param year 年份
     * @param permission 权限设置：reader（只读）或writer（可编辑），默认reader
     * @return 分享链接和电子表格ID
     */
    @PostMapping("/sync-annual-report")
    @Operation(summary = "同步年度财务报表到Google Sheets（异步）",
               description = "将指定年份的财务报表导出到Google Sheets（异步任务）。立即返回任务ID，客户端需要轮询查询任务状态。")
    public ResponseEntity<Map<String, Object>> syncAnnualReport(
            @Parameter(description = "家庭ID", required = true)
            @RequestParam Long familyId,

            @Parameter(description = "年份", required = true, example = "2024")
            @RequestParam Integer year,

            @Parameter(description = "权限设置：reader（只读）或writer（可编辑）", example = "reader")
            @RequestParam(defaultValue = "reader") String permission) {

        try {
            log.info("开始创建Google Sheets同步任务: familyId={}, year={}, permission={}",
                familyId, year, permission);

            // 验证权限参数
            if (!permission.equals("reader") && !permission.equals("writer")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "无效的权限参数，只能是reader或writer");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 启动异步任务
            Map<String, Object> taskResult = googleSheetsExportService.createOrUpdateAnnualReport(familyId, year, permission);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", taskResult);

            log.info("同步任务已启动: syncId={}", taskResult.get("syncId"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建同步任务失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "创建任务失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 查询同步任务状态
     */
    @GetMapping("/sync-status/{syncId}")
    @Operation(summary = "查询同步任务状态",
               description = "根据任务ID查询Google Sheets同步任务的状态和进度")
    public ResponseEntity<Map<String, Object>> getSyncStatus(
            @Parameter(description = "同步任务ID", required = true)
            @PathVariable Long syncId) {

        Optional<GoogleSheetsSync> syncOpt = googleSheetsSyncRepository.findById(syncId);

        if (syncOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "找不到指定的同步任务");
            return ResponseEntity.status(404).body(errorResponse);
        }

        GoogleSheetsSync sync = syncOpt.get();

        Map<String, Object> data = new HashMap<>();
        data.put("syncId", sync.getId());
        data.put("status", sync.getStatus());
        data.put("progress", sync.getProgress());
        data.put("familyId", sync.getFamilyId());
        data.put("year", sync.getYear());

        if ("COMPLETED".equals(sync.getStatus())) {
            data.put("shareUrl", sync.getShareUrl());
            data.put("spreadsheetId", sync.getSpreadsheetId());
            data.put("permission", sync.getPermission());
        }

        if ("FAILED".equals(sync.getStatus())) {
            data.put("errorMessage", sync.getErrorMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
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
