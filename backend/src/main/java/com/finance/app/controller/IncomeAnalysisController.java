package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.income.IncomeAnnualMajorCategoryDTO;
import com.finance.app.dto.income.IncomeAnnualMinorCategoryDTO;
import com.finance.app.dto.income.IncomeMonthlyTrendDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.income.IncomeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incomes-analysis")
@RequiredArgsConstructor
public class IncomeAnalysisController {

    private final IncomeAnalysisService incomeAnalysisService;
    private final AuthHelper authHelper;

    /**
     * 获取年度大类汇总
     */
    @GetMapping("/annual/major-categories")
    public ApiResponse<List<IncomeAnnualMajorCategoryDTO>> getAnnualMajorCategories(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authorized family (respects admin's familyId parameter)
        Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<IncomeAnnualMajorCategoryDTO> result = incomeAnalysisService.getAnnualMajorCategorySummary(
                authorizedFamilyId, year, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取年度小类汇总（某个大类下）
     */
    @GetMapping("/annual/minor-categories")
    public ApiResponse<List<IncomeAnnualMinorCategoryDTO>> getAnnualMinorCategories(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authorized family (respects admin's familyId parameter)
        Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<IncomeAnnualMinorCategoryDTO> result = incomeAnalysisService.getAnnualMinorCategorySummary(
                authorizedFamilyId, year, majorCategoryId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取月度趋势（某个小类或无小类的大类数据）
     */
    @GetMapping("/annual/monthly-trend")
    public ApiResponse<List<IncomeMonthlyTrendDTO>> getAnnualMonthlyTrend(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam Long majorCategoryId,
            @RequestParam(required = false) Long minorCategoryId,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authorized family (respects admin's familyId parameter)
        Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<IncomeMonthlyTrendDTO> result = incomeAnalysisService.getAnnualMonthlyTrend(
                authorizedFamilyId, year, majorCategoryId, minorCategoryId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 刷新年度收入汇总表
     */
    @PostMapping("/annual/refresh")
    public ApiResponse<String> refreshAnnualIncomeSummary(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authorized family (respects admin's familyId parameter)
        Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        incomeAnalysisService.refreshAnnualIncomeSummary(authorizedFamilyId, year, currency);
        return ApiResponse.success("年度收入汇总数据已刷新");
    }
}
