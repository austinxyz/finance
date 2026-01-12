package com.finance.app.controller;

import com.finance.app.dto.expense.AnnualExpenseSummaryDTO;
import com.finance.app.dto.expense.BudgetExecutionDTO;
import com.finance.app.dto.expense.ExpenseAnnualMajorCategoryDTO;
import com.finance.app.dto.expense.ExpenseAnnualMinorCategoryDTO;
import com.finance.app.dto.expense.ExpenseMonthlyTrendDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.expense.ExpenseAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses/analysis")
@RequiredArgsConstructor
public class ExpenseAnalysisController {

    private final ExpenseAnalysisService expenseAnalysisService;
    private final AuthHelper authHelper;

    /**
     * 获取年度大类汇总
     * GET /expenses/analysis/annual/major-categories?familyId=1&year=2025&currency=USD
     */
    @GetMapping("/annual/major-categories")
    public ResponseEntity<Map<String, Object>> getAnnualMajorCategorySummary(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<ExpenseAnnualMajorCategoryDTO> result = expenseAnalysisService
                    .getAnnualMajorCategorySummary(targetFamilyId, year, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询大类汇总失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取年度小类汇总（某个大类下）
     * GET /expenses/analysis/annual/minor-categories?familyId=1&year=2025&majorCategoryId=1&currency=USD
     */
    @GetMapping("/annual/minor-categories")
    public ResponseEntity<Map<String, Object>> getAnnualMinorCategorySummary(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<ExpenseAnnualMinorCategoryDTO> result = expenseAnalysisService
                    .getAnnualMinorCategorySummary(targetFamilyId, year, majorCategoryId, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询小类汇总失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取月度趋势（某个小类的12个月数据）
     * GET /expenses/analysis/annual/monthly-trend?familyId=1&year=2025&minorCategoryId=82&currency=USD
     */
    @GetMapping("/annual/monthly-trend")
    public ResponseEntity<Map<String, Object>> getAnnualMonthlyTrend(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam Long minorCategoryId,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<ExpenseMonthlyTrendDTO> result = expenseAnalysisService
                    .getAnnualMonthlyTrend(targetFamilyId, year, minorCategoryId, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询月度趋势失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取预算执行分析
     * GET /expenses/analysis/budget-execution?familyId=1&budgetYear=2025&currency=USD
     */
    @GetMapping("/budget-execution")
    public ResponseEntity<Map<String, Object>> getBudgetExecution(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer budgetYear,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<BudgetExecutionDTO> result = expenseAnalysisService
                    .getBudgetExecution(targetFamilyId, budgetYear, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询预算执行失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取年度支出汇总(包含资产/负债调整)
     * GET /expenses/analysis/annual/summary?familyId=1&year=2025&currency=CNY&includeTotals=true
     */
    @GetMapping("/annual/summary")
    public ResponseEntity<Map<String, Object>> getAnnualExpenseSummary(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "CNY") String currency,
            @RequestParam(defaultValue = "true") boolean includeTotals,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<AnnualExpenseSummaryDTO> result = expenseAnalysisService
                    .getAnnualExpenseSummaryWithAdjustments(targetFamilyId, year, currency, includeTotals);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询年度汇总失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 计算年度支出汇总（运行存储过程）
     * POST /expenses/analysis/annual/summary/calculate?familyId=1&year=2025
     */
    @PostMapping("/annual/summary/calculate")
    public ResponseEntity<Map<String, Object>> calculateAnnualExpenseSummary(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            expenseAnalysisService.calculateAnnualExpenseSummary(targetFamilyId, year);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "年度支出汇总计算完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "计算年度汇总失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取多年度支出趋势分析
     * GET /expenses/analysis/annual/trend?familyId=1&limit=5&currency=USD
     */
    @GetMapping("/annual/trend")
    public ResponseEntity<Map<String, Object>> getAnnualExpenseTrend(
            @RequestParam(required = false) Long familyId,
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<Map<String, Object>> result = expenseAnalysisService
                    .getAnnualExpenseTrend(targetFamilyId, limit, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询年度趋势失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取各大类的多年度基础支出趋势
     * GET /expenses/analysis/annual/category-trend?familyId=1&limit=5&currency=USD
     */
    @GetMapping("/annual/category-trend")
    public ResponseEntity<Map<String, Object>> getAnnualCategoryTrend(
            @RequestParam(required = false) Long familyId,
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<Map<String, Object>> result = expenseAnalysisService
                    .getAnnualCategoryTrend(targetFamilyId, limit, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询大类年度趋势失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取年度汇总表（返回USD基准货币数据，前端根据选中货币换算）
     * GET /expenses/analysis/annual/summary-table?familyId=1&limit=5
     */
    @GetMapping("/annual/summary-table")
    public ResponseEntity<Map<String, Object>> getAnnualSummaryTable(
            @RequestParam(required = false) Long familyId,
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Admin can view any family, regular users can only view their own
            Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            // 始终返回USD基准货币数据
            Map<String, Object> result = expenseAnalysisService
                    .getAnnualSummaryTable(targetFamilyId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询年度汇总表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
