package com.finance.app.controller;

import com.finance.app.dto.AnnualFinancialSummaryDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.AnnualFinancialSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 年度财务摘要Controller（按家庭统计）
 */
@RestController
@RequestMapping("/annual-summary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnnualFinancialSummaryController {

    private final AnnualFinancialSummaryService summaryService;
    private final AuthHelper authHelper;

    /**
     * 获取家庭所有年度摘要
     */
    @GetMapping("/family/{familyId}")
    public ResponseEntity<Map<String, Object>> getAllSummaries(
            @PathVariable Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<AnnualFinancialSummaryDTO> summaries = summaryService.getAllSummaries(authorizedFamilyId);
            Map<String, Object> response = Map.of("success", true, "data", summaries);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取家庭年度摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 获取家庭指定年份的摘要
     */
    @GetMapping("/family/{familyId}/year/{year}")
    public ResponseEntity<AnnualFinancialSummaryDTO> getSummaryByYear(
            @PathVariable Long familyId,
            @PathVariable Integer year,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            return summaryService.getSummaryByYear(authorizedFamilyId, year)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("获取家庭指定年份摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取家庭指定年份范围的摘要
     */
    @GetMapping("/family/{familyId}/range")
    public ResponseEntity<Map<String, Object>> getSummariesByRange(
            @PathVariable Long familyId,
            @RequestParam Integer startYear,
            @RequestParam Integer endYear,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<AnnualFinancialSummaryDTO> summaries =
                    summaryService.getSummariesByYearRange(authorizedFamilyId, startYear, endYear);
            Map<String, Object> response = Map.of("success", true, "data", summaries);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取家庭年份范围摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 获取最近N年的摘要
     */
    @GetMapping("/family/{familyId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentYearsSummaries(
            @PathVariable Long familyId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<AnnualFinancialSummaryDTO> summaries =
                    summaryService.getRecentYearsSummaries(authorizedFamilyId, limit);
            Map<String, Object> response = Map.of("success", true, "data", summaries);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取家庭最近年份摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 计算或刷新指定年份的财务摘要
     */
    @PostMapping("/family/{familyId}/calculate/{year}")
    public ResponseEntity<AnnualFinancialSummaryDTO> calculateSummary(
            @PathVariable Long familyId,
            @PathVariable Integer year,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            AnnualFinancialSummaryDTO summary = summaryService.calculateAndRefreshSummary(authorizedFamilyId, year);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("计算年度摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 批量刷新多个年份的摘要
     */
    @PostMapping("/family/{familyId}/batch-calculate")
    public ResponseEntity<List<AnnualFinancialSummaryDTO>> batchCalculateSummaries(
            @PathVariable Long familyId,
            @RequestBody List<Integer> years,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<AnnualFinancialSummaryDTO> summaries =
                    summaryService.batchRefreshSummaries(authorizedFamilyId, years);
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            log.error("批量计算年度摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除指定年份的摘要
     */
    @DeleteMapping("/family/{familyId}/year/{year}")
    public ResponseEntity<Map<String, String>> deleteSummary(
            @PathVariable Long familyId,
            @PathVariable Integer year,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authorized family (respects admin's familyId parameter)
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            summaryService.deleteSummary(authorizedFamilyId, year);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            log.error("删除年度摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "删除失败: " + e.getMessage()));
        }
    }

    /**
     * 手动创建或更新摘要
     */
    @PostMapping("/family/{familyId}/save")
    public ResponseEntity<AnnualFinancialSummaryDTO> saveOrUpdateSummary(
            @PathVariable Long familyId,
            @RequestBody AnnualFinancialSummaryDTO summaryDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Set familyId from authenticated user
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);
            summaryDTO.setFamilyId(authorizedFamilyId);
            AnnualFinancialSummaryDTO saved = summaryService.saveOrUpdateSummary(summaryDTO);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("保存年度摘要失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
