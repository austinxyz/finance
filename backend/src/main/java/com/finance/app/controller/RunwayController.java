package com.finance.app.controller;

import com.finance.app.dto.RunwayAnalysisDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.RunwayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/runway")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RunwayController {

    private final RunwayService runwayService;
    private final AuthHelper authHelper;

    /**
     * 计算家庭资金跑道
     *
     * @param familyId      家庭ID
     * @param months        支出回溯月数（默认6个月）
     * @param includedTypes 纳入计算的资产类型代码列表（默认包含现金、股票、经纪账户、加密货币）
     * @param authHeader    JWT认证头
     */
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> getRunwayAnalysis(
            @RequestParam Long familyId,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) List<String> includedTypes,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authHelper.requireFamilyAccess(authHeader, familyId);

            if (months < 1 || months > 36) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "months 参数范围为 1-36"));
            }

            RunwayAnalysisDTO result = runwayService.calculateRunway(familyId, includedTypes, months);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("计算家庭 {} 的资金跑道失败", familyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
