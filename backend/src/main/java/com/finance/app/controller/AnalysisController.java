package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.AssetSummaryDTO;
import com.finance.app.dto.TrendDataDTO;
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
    public ApiResponse<AssetSummaryDTO> getAssetSummary(@RequestParam(required = false) Long userId) {
        AssetSummaryDTO summary = analysisService.getAssetSummary(userId);
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
    public ApiResponse<Map<String, Object>> getAssetAllocationByType(@RequestParam(required = false) Long userId) {
        Map<String, Object> allocation = analysisService.getAssetAllocationByType(userId);
        return ApiResponse.success(allocation);
    }

    // 获取净资产配置（资产减去对应负债）
    @GetMapping("/allocation/net")
    public ApiResponse<Map<String, Object>> getNetAssetAllocation(@RequestParam(required = false) Long userId) {
        Map<String, Object> allocation = analysisService.getNetAssetAllocation(userId);
        return ApiResponse.success(allocation);
    }

    // 获取按类型的负债配置
    @GetMapping("/allocation/liability")
    public ApiResponse<Map<String, Object>> getLiabilityAllocationByType(@RequestParam(required = false) Long userId) {
        Map<String, Object> allocation = analysisService.getLiabilityAllocationByType(userId);
        return ApiResponse.success(allocation);
    }
}
