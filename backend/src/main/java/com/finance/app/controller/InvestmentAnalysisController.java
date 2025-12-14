package com.finance.app.controller;

import com.finance.app.dto.AccountMonthlyTrendResponseDTO;
import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.InvestmentAccountAnalysisDTO;
import com.finance.app.dto.InvestmentCategoryAnalysisDTO;
import com.finance.app.service.InvestmentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 投资分析控制器
 */
@RestController
@RequestMapping("/investments/analysis")
@RequiredArgsConstructor
public class InvestmentAnalysisController {

    private final InvestmentAnalysisService analysisService;

    /**
     * 获取年度大类投资分析
     */
    @GetMapping("/annual/by-category")
    public ApiResponse<List<InvestmentCategoryAnalysisDTO>> getAnnualByCategory(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "All") String currency
    ) {
        List<InvestmentCategoryAnalysisDTO> result = analysisService.getAnnualByCategory(familyId, year, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取年度账户投资分析（可按大类筛选）
     */
    @GetMapping("/annual/by-account")
    public ApiResponse<List<InvestmentAccountAnalysisDTO>> getAnnualByAccount(
            @RequestParam Long familyId,
            @RequestParam Integer year,
            @RequestParam(required = false) Long assetTypeId,
            @RequestParam(defaultValue = "All") String currency
    ) {
        List<InvestmentAccountAnalysisDTO> result = analysisService.getAnnualByAccount(familyId, year, assetTypeId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取账户月度投资趋势
     */
    @GetMapping("/annual/monthly-trend")
    public ApiResponse<AccountMonthlyTrendResponseDTO> getAccountMonthlyTrend(
            @RequestParam Long accountId,
            @RequestParam Integer year
    ) {
        AccountMonthlyTrendResponseDTO result = analysisService.getAccountMonthlyTrend(accountId, year);
        return ApiResponse.success(result);
    }
}
