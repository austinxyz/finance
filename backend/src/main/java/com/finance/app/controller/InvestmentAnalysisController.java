package com.finance.app.controller;

import com.finance.app.dto.AccountMonthlyTrendResponseDTO;
import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.InvestmentAccountAnalysisDTO;
import com.finance.app.dto.InvestmentCategoryAnalysisDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.InvestmentAnalysisService;
import com.finance.app.service.asset.AssetService;
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
    private final AssetService assetService;
    private final AuthHelper authHelper;

    /**
     * 获取年度大类投资分析
     */
    @GetMapping("/annual/by-category")
    public ApiResponse<List<InvestmentCategoryAnalysisDTO>> getAnnualByCategory(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // Admin can view any family, regular users can only view their own
        Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<InvestmentCategoryAnalysisDTO> result = analysisService.getAnnualByCategory(targetFamilyId, year, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取年度账户投资分析（可按大类筛选）
     */
    @GetMapping("/annual/by-account")
    public ApiResponse<List<InvestmentAccountAnalysisDTO>> getAnnualByAccount(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer year,
            @RequestParam(required = false) Long assetTypeId,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // Admin can view any family, regular users can only view their own
        Long targetFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<InvestmentAccountAnalysisDTO> result = analysisService.getAnnualByAccount(targetFamilyId, year, assetTypeId, currency);
        return ApiResponse.success(result);
    }

    /**
     * 获取账户月度投资趋势
     */
    @GetMapping("/annual/monthly-trend")
    public ApiResponse<AccountMonthlyTrendResponseDTO> getAccountMonthlyTrend(
            @RequestParam Long accountId,
            @RequestParam Integer year,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // Verify account access (account belongs to user's family)
        AssetAccount account = assetService.getAccountById(accountId);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        AccountMonthlyTrendResponseDTO result = analysisService.getAccountMonthlyTrend(accountId, year);
        return ApiResponse.success(result);
    }
}
