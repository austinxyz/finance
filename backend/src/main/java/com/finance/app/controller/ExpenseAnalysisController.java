package com.finance.app.controller;

import com.finance.app.dto.expense.AnnualExpenseSummaryDTO;
import com.finance.app.dto.expense.BudgetExecutionDTO;
import com.finance.app.dto.expense.ExpenseAnnualMajorCategoryDTO;
import com.finance.app.dto.expense.ExpenseAnnualMinorCategoryDTO;
import com.finance.app.dto.expense.ExpenseMonthlyTrendDTO;
import com.finance.app.service.expense.ExpenseAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses/analysis")
public class ExpenseAnalysisController {

    @Autowired
    private ExpenseAnalysisService expenseAnalysisService;

    /**
     * 获取年度大类汇总
     * GET /expenses/analysis/annual/major-categories?familyId=1&year=2025&currency=USD
     */
    @GetMapping("/annual/major-categories")
    public ResponseEntity<Map<String, Object>> getAnnualMajorCategorySummary(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency) {
        try {
            List<ExpenseAnnualMajorCategoryDTO> result = expenseAnalysisService
                    .getAnnualMajorCategorySummary(familyId, year, currency);

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
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {
        try {
            List<ExpenseAnnualMinorCategoryDTO> result = expenseAnalysisService
                    .getAnnualMinorCategorySummary(familyId, year, majorCategoryId, currency);

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
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long minorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {
        try {
            List<ExpenseMonthlyTrendDTO> result = expenseAnalysisService
                    .getAnnualMonthlyTrend(familyId, year, minorCategoryId, currency);

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
            @RequestParam Long familyId,
            @RequestParam Integer budgetYear,
            @RequestParam(defaultValue = "USD") String currency) {
        try {
            List<BudgetExecutionDTO> result = expenseAnalysisService
                    .getBudgetExecution(familyId, budgetYear, currency);

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
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "CNY") String currency,
            @RequestParam(defaultValue = "true") boolean includeTotals) {
        try {
            List<AnnualExpenseSummaryDTO> result = expenseAnalysisService
                    .getAnnualExpenseSummaryWithAdjustments(familyId, year, currency, includeTotals);

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
}
