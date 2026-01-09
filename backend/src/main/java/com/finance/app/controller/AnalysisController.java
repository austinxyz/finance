package com.finance.app.controller;

import com.finance.app.dto.AIAdviceRequestDTO;
import com.finance.app.dto.AIAdviceResponseDTO;
import com.finance.app.dto.AccountTrendDataPointDTO;
import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.EnhancedFinancialMetricsDTO;
import com.finance.app.dto.FinancialMetricsDTO;
import com.finance.app.dto.OptimizationRecommendationDTO;
import com.finance.app.dto.OverallTrendDataPointDTO;
import com.finance.app.dto.RiskAssessmentDTO;
import com.finance.app.dto.TrendDataDTO;
import com.finance.app.dto.TrendDataPointDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.AnalysisService;
import com.finance.app.service.ClaudeService;
import com.finance.app.service.asset.AssetAnalysisService;
import com.finance.app.service.liability.LiabilityAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AssetAnalysisService assetAnalysisService;
    private final LiabilityAnalysisService liabilityAnalysisService;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;
    private final AuthHelper authHelper;

    // 获取资产总览（包含负债和净资产）
    @GetMapping("/summary")
    public ApiResponse<AssetSummaryDTO> getAssetSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        // Get asset summary with authenticated family
        AssetSummaryDTO summary = assetAnalysisService.getAssetSummary(userId, authenticatedFamilyId, asOfDate, false, currency);

        // Add liability data using AnalysisService
        summary = analysisService.addLiabilityDataToSummary(summary, userId, authenticatedFamilyId, asOfDate, currency);

        return ApiResponse.success(summary);
    }

    // 获取总资产趋势数据
    @GetMapping("/trends/total")
    public ApiResponse<List<TrendDataDTO>> getTotalAssetTrend(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<TrendDataDTO> trends = assetAnalysisService.getTotalAssetTrend(userId, startDate, endDate);
        return ApiResponse.success(trends);
    }

    // 获取单个账户趋势数据
    @GetMapping("/trends/account/{accountId}")
    public ApiResponse<List<TrendDataDTO>> getAccountTrend(@PathVariable Long accountId) {
        List<TrendDataDTO> trends = assetAnalysisService.getAccountTrend(accountId);
        return ApiResponse.success(trends);
    }

    // 获取按分类汇总的资产数据
    @GetMapping("/allocation/category")
    public ApiResponse<Map<String, Object>> getAssetAllocationByCategory(@RequestParam(required = false) Long userId) {
        Map<String, Object> allocation = assetAnalysisService.getAssetAllocationByCategory(userId);
        return ApiResponse.success(allocation);
    }

    // 获取按类型汇总的资产数据
    @GetMapping("/allocation/type")
    public ApiResponse<Map<String, Object>> getAssetAllocationByType(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = assetAnalysisService.getAssetAllocationByType(userId, authenticatedFamilyId, asOfDate, currency);
        return ApiResponse.success(allocation);
    }

    // 获取净资产配置（资产减去对应负债）
    @GetMapping("/allocation/net")
    public ApiResponse<Map<String, Object>> getNetAssetAllocation(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = analysisService.getNetAssetAllocation(userId, authenticatedFamilyId, asOfDate, currency);
        return ApiResponse.success(allocation);
    }

    // 获取按类型的负债配置
    @GetMapping("/allocation/liability")
    public ApiResponse<Map<String, Object>> getLiabilityAllocationByType(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "All") String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = liabilityAnalysisService.getLiabilityAllocationByType(userId, authenticatedFamilyId, asOfDate, currency);
        return ApiResponse.success(allocation);
    }

    // 获取综合趋势数据（净资产、总资产、总负债）
    @GetMapping("/trends/overall")
    public ApiResponse<List<OverallTrendDataPointDTO>> getOverallTrend(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<OverallTrendDataPointDTO> trends = analysisService.getOverallTrend(startDate, endDate, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取资产分类趋势数据
    @GetMapping("/trends/asset-category/{categoryType}")
    public ApiResponse<List<TrendDataPointDTO>> getAssetCategoryTrend(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<TrendDataPointDTO> trends = assetAnalysisService.getAssetCategoryTrend(categoryType, startDate, endDate, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取负债分类趋势数据
    @GetMapping("/trends/liability-category/{categoryType}")
    public ApiResponse<List<TrendDataPointDTO>> getLiabilityCategoryTrend(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<TrendDataPointDTO> trends = liabilityAnalysisService.getLiabilityCategoryTrend(categoryType, startDate, endDate, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取净资产分类趋势数据
    @GetMapping("/trends/net-asset-category/{categoryCode}")
    public ApiResponse<List<TrendDataPointDTO>> getNetAssetCategoryTrend(
            @PathVariable String categoryCode,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<TrendDataPointDTO> trends = analysisService.getNetAssetCategoryTrend(categoryCode, startDate, endDate, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取指定类型和日期的资产账户及其余额
    @GetMapping("/allocation/asset-accounts/{categoryType}")
    public ApiResponse<List<Map<String, Object>>> getAssetAccountsWithBalances(
            @PathVariable String categoryType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<Map<String, Object>> accounts = assetAnalysisService.getAssetAccountsWithBalancesByType(categoryType, userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取指定类型和日期的负债账户及其余额
    @GetMapping("/allocation/liability-accounts/{categoryType}")
    public ApiResponse<List<Map<String, Object>>> getLiabilityAccountsWithBalances(
            @PathVariable String categoryType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        List<Map<String, Object>> accounts = liabilityAnalysisService.getLiabilityAccountsWithBalancesByType(categoryType, userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取资产分类下所有账户的趋势数据
    @GetMapping("/trends/asset-accounts/{categoryType}")
    public ApiResponse<Map<String, List<AccountTrendDataPointDTO>>> getAssetAccountsTrendByCategory(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, List<AccountTrendDataPointDTO>> trends = assetAnalysisService.getAssetAccountsTrendByCategory(
            categoryType, startDate, endDate, userId, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取负债分类下所有账户的趋势数据
    @GetMapping("/trends/liability-accounts/{categoryType}")
    public ApiResponse<Map<String, List<AccountTrendDataPointDTO>>> getLiabilityAccountsTrendByCategory(
            @PathVariable String categoryType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, List<AccountTrendDataPointDTO>> trends = liabilityAnalysisService.getLiabilityAccountsTrendByCategory(
            categoryType, startDate, endDate, userId, authenticatedFamilyId);
        return ApiResponse.success(trends);
    }

    // 获取净资产类别下的所有账户详情（包含资产账户和负债账户）
    @GetMapping("/allocation/net-asset-accounts/{categoryCode}")
    public ApiResponse<Map<String, Object>> getNetAssetCategoryAccounts(
            @PathVariable String categoryCode,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> accounts = analysisService.getNetAssetCategoryAccounts(categoryCode, userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(accounts);
    }

    // 获取按税收状态的净资产配置
    @GetMapping("/allocation/net-worth-by-tax-status")
    public ApiResponse<Map<String, Object>> getNetWorthByTaxStatus(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = analysisService.getNetWorthByTaxStatus(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取按家庭成员的净资产配置
    @GetMapping("/allocation/net-worth-by-member")
    public ApiResponse<Map<String, Object>> getNetWorthByMember(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = analysisService.getNetWorthByMember(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取按货币的净资产配置
    @GetMapping("/allocation/net-worth-by-currency")
    public ApiResponse<Map<String, Object>> getNetWorthByCurrency(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        Map<String, Object> allocation = analysisService.getNetWorthByCurrency(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(allocation);
    }

    // 获取财务指标
    @GetMapping("/financial-metrics")
    public ApiResponse<FinancialMetricsDTO> getFinancialMetrics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        FinancialMetricsDTO metrics = analysisService.getFinancialMetrics(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(metrics);
    }

    // 获取增强的财务指标（整合收入、支出、投资等全维度数据）
    @GetMapping("/financial-metrics/enhanced")
    public ApiResponse<EnhancedFinancialMetricsDTO> getEnhancedFinancialMetrics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        EnhancedFinancialMetricsDTO metrics = analysisService.getEnhancedFinancialMetrics(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(metrics);
    }

    // 获取风险评估
    @GetMapping("/risk-assessment")
    public ApiResponse<RiskAssessmentDTO> getRiskAssessment(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        RiskAssessmentDTO assessment = analysisService.getRiskAssessment(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(assessment);
    }

    // 获取优化建议
    @GetMapping("/optimization-recommendations")
    public ApiResponse<OptimizationRecommendationDTO> getOptimizationRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        OptimizationRecommendationDTO recommendations = analysisService.getOptimizationRecommendations(userId, authenticatedFamilyId, asOfDate);
        return ApiResponse.success(recommendations);
    }

    // 获取 AI 增强的个性化理财建议
    @PostMapping("/ai-advice")
    public ApiResponse<AIAdviceResponseDTO> getAIAdvice(
            @RequestBody AIAdviceRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Use authenticated user's family_id
            Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

            // Get current optimization recommendations with authenticated family
            OptimizationRecommendationDTO recommendations =
                analysisService.getOptimizationRecommendations(request.getUserId(), authenticatedFamilyId, LocalDate.now());

            // Convert to Map for Claude API
            Map<String, Object> financialData = objectMapper.convertValue(recommendations, Map.class);

            // Generate AI advice
            String advice = claudeService.generateEnhancedRecommendations(
                financialData,
                request.getUserContext()
            );

            boolean aiEnabled = !advice.contains("Claude API 未配置") && !advice.contains("调用 Claude API 时出错");

            return ApiResponse.success(AIAdviceResponseDTO.builder()
                .advice(advice)
                .aiEnabled(aiEnabled)
                .errorMessage(aiEnabled ? null : advice)
                .build());

        } catch (Exception e) {
            return ApiResponse.success(AIAdviceResponseDTO.builder()
                .advice("生成 AI 建议时出错: " + e.getMessage())
                .aiEnabled(false)
                .errorMessage(e.getMessage())
                .build());
        }
    }
}
