package com.finance.app.controller;

import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.OverallTrendDataPointDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin
public class AnalysisController {

    private final AnalysisService analysisService;

    // 获取资产总览
    @GetMapping("/summary")
    public ApiResponse<AssetSummaryDTO> getAssetSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        AssetSummaryDTO summary = analysisService.getAssetSummary(userId, asOfDate);
        return ApiResponse.success(summary);
    }

    // 获取总资产趋势数据
    @GetMapping("/trends/total")
    public ApiResponse<List<TrendDataDTO>> getTotalAssetTrend(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<TrendDataDTO> trends = analysisService.getTotalAssetTrend(userId, startDate, endDate);
        return ApiResponse.success(trends);
    }

    // 获取单个账户趋势数据
    @GetMapping("/trends/account/{accountId}")
    public ApiResponse<List<TrendDataDTO>> getAccountTrend(@PathVariable Long accountId) {
        List<TrendDataDTO> trends = analysisService.getAccountTrend(accountId);
        return ApiResponse.success(trends);
    }

    // 获取按分类汇总的资产数据
    @GetMapping("/allocation/category")
    public ApiResponse<Map<String, Object>> getAssetAllocationByCategory(@RequestParam(required = false) Long userId) {
        Map<String, Object> allocation = analysisService.getAssetAllocationByCategory(userId);
        return ApiResponse.success(allocation);
    }

    // 获取按类型汇总的资产数据
    @GetMapping("/allocation/type")
    public ApiResponse<Map<String, Object>> getAssetAllocationByType(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Map<String, Object> allocation = analysisService.getAssetAllocationByType(userId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取净资产配置（资产减去对应负债）
    @GetMapping("/allocation/net")
    public ApiResponse<Map<String, Object>> getNetAssetAllocation(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Map<String, Object> allocation = analysisService.getNetAssetAllocation(userId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取按类型的负债配置
    @GetMapping("/allocation/liability")
    public ApiResponse<Map<String, Object>> getLiabilityAllocationByType(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Map<String, Object> allocation = analysisService.getLiabilityAllocationByType(userId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取综合趋势数据（净资产、总资产、总负债）
    @GetMapping("/trends/overall")
    public ApiResponse<List<OverallTrendDataPointDTO>> getOverallTrend(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        List<OverallTrendDataPointDTO> trends = analysisService.getOverallTrend(startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取资产分类趋势数据
    @GetMapping("/trends/asset-category/{categoryType}")
    public ApiResponse<List<TrendDataPointDTO>> getAssetCategoryTrend(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        List<TrendDataPointDTO> trends = analysisService.getAssetCategoryTrend(categoryType, startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取负债分类趋势数据
    @GetMapping("/trends/liability-category/{categoryType}")
    public ApiResponse<List<TrendDataPointDTO>> getLiabilityCategoryTrend(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        List<TrendDataPointDTO> trends = analysisService.getLiabilityCategoryTrend(categoryType, startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取净资产分类趋势数据
    @GetMapping("/trends/net-asset-category/{categoryCode}")
    public ApiResponse<List<TrendDataPointDTO>> getNetAssetCategoryTrend(
            @PathVariable String categoryCode,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        List<TrendDataPointDTO> trends = analysisService.getNetAssetCategoryTrend(categoryCode, startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取指定类型和日期的资产账户及其余额
    @GetMapping("/allocation/asset-accounts/{categoryType}")
    public ApiResponse<List<Map<String, Object>>> getAssetAccountsWithBalances(
            @PathVariable String categoryType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        List<Map<String, Object>> accounts = analysisService.getAssetAccountsWithBalancesByType(categoryType, userId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取指定类型和日期的负债账户及其余额
    @GetMapping("/allocation/liability-accounts/{categoryType}")
    public ApiResponse<List<Map<String, Object>>> getLiabilityAccountsWithBalances(
            @PathVariable String categoryType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        List<Map<String, Object>> accounts = analysisService.getLiabilityAccountsWithBalancesByType(categoryType, userId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取资产分类下所有账户的趋势数据
    @GetMapping("/trends/asset-accounts/{categoryType}")
    public ApiResponse<Map<String, List<AccountTrendDataPointDTO>>> getAssetAccountsTrendByCategory(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        Map<String, List<AccountTrendDataPointDTO>> trends = analysisService.getAssetAccountsTrendByCategory(
            categoryType, startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取负债分类下所有账户的趋势数据
    @GetMapping("/trends/liability-accounts/{categoryType}")
    public ApiResponse<Map<String, List<AccountTrendDataPointDTO>>> getLiabilityAccountsTrendByCategory(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId) {
        Map<String, List<AccountTrendDataPointDTO>> trends = analysisService.getLiabilityAccountsTrendByCategory(
            categoryType, startDate, endDate, userId);
        return ApiResponse.success(trends);
    }

    // 获取净资产类别下的所有账户详情（包含资产账户和负债账户）
    @GetMapping("/allocation/net-asset-accounts/{categoryCode}")
    public ApiResponse<Map<String, Object>> getNetAssetCategoryAccounts(
            @PathVariable String categoryCode,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Map<String, Object> accounts = analysisService.getNetAssetCategoryAccounts(categoryCode, userId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取按税收状态的净资产配置
    @GetMapping("/allocation/net-worth-by-tax-status")
    public ApiResponse<Map<String, Object>> getNetWorthByTaxStatus(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Map<String, Object> allocation = analysisService.getNetWorthByTaxStatus(userId, asOfDate);
        return ApiResponse.success(allocation);
    }
}
