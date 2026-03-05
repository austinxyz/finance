package com.finance.app.controller;

import com.finance.app.dto.RunwayReportDetailDTO;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.dto.SaveRunwayReportRequest;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.RunwayReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/runway/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RunwayReportController {

    private final RunwayReportService runwayReportService;
    private final AuthHelper authHelper;

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveReport(
            @RequestBody SaveRunwayReportRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authHelper.requireFamilyAccess(authHeader, request.getFamilyId());
            RunwayReportSummaryDTO result = runwayReportService.saveReport(request.getFamilyId(), request.getSnapshotJson());
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("保存资金跑道报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listReports(
            @RequestParam Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authHelper.requireFamilyAccess(authHeader, familyId);
            List<RunwayReportSummaryDTO> result = runwayReportService.listReports(familyId);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("获取资金跑道报告列表失败: familyId={}", familyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReport(
            @PathVariable Long id,
            @RequestParam Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authHelper.requireFamilyAccess(authHeader, familyId);
            RunwayReportDetailDTO result = runwayReportService.getReport(id, familyId);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("获取资金跑道报告失败: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long familyId = authHelper.getFamilyIdFromAuth(authHeader);
            runwayReportService.deleteReport(id, familyId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("删除资金跑道报告失败: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
