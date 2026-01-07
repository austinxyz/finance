package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO;
import com.finance.app.dto.income.IncomeAnnualMinorCategoryDTO;
import com.finance.app.dto.income.IncomeMonthlyTrendDTO;
import com.finance.app.service.income.IncomeAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incomes-analysis")
public class IncomeAnalysisController {

    @Autowired
    private IncomeAnalysisService incomeAnalysisService;

    /**
     * 获取年度大类汇总
     */
    @GetMapping("/annual/major-categories")
    public ApiResponse<List<IncomeAnnualMajorCategoryDTO>> getAnnualMajorCategories(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency) {
        List<IncomeAnnualMajorCategoryDTO> result = incomeAnalysisService.getAnnualMajorCategorySummary(
                familyId, year, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取年度小类汇总（某个大类下）
     */
    @GetMapping("/annual/minor-categories")
    public ApiResponse<List<IncomeAnnualMinorCategoryDTO>> getAnnualMinorCategories(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {
        List<IncomeAnnualMinorCategoryDTO> result = incomeAnalysisService.getAnnualMinorCategorySummary(
                familyId, year, majorCategoryId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取月度趋势（某个小类或无小类的大类数据）
     */
    @GetMapping("/annual/monthly-trend")
    public ApiResponse<List<IncomeMonthlyTrendDTO>> getAnnualMonthlyTrend(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(required = false) Long minorCategoryId,
            @RequestParam(defaultValue = "USD") String currency) {
        List<IncomeMonthlyTrendDTO> result = incomeAnalysisService.getAnnualMonthlyTrend(
                familyId, year, majorCategoryId, minorCategoryId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 刷新年度收入汇总表
     */
    @PostMapping("/annual/refresh")
    public ApiResponse<String> refreshAnnualIncomeSummary(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency) {
        incomeAnalysisService.refreshAnnualIncomeSummary(familyId, year, currency);
        return ApiResponse.success("年度收入汇总数据已刷新");
    }
}
