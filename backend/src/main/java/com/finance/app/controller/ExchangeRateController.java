package com.finance.app.controller;

import com.finance.app.model.ExchangeRate;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final AuthHelper authHelper;

    /**
     * 获取所有启用的汇率 - Public endpoint
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActiveRates() {
        List<ExchangeRate> rates = exchangeRateService.getAllActiveRates();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rates);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有汇率（包括停用的） - Admin only
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllRates(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        List<ExchangeRate> rates = exchangeRateService.getAllRates();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rates);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取特定货币的所有汇率历史 - Public endpoint
     */
    @GetMapping("/currency/{currency}")
    public ResponseEntity<Map<String, Object>> getRatesByCurrency(@PathVariable String currency) {
        List<ExchangeRate> rates = exchangeRateService.getRatesByCurrency(currency);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rates);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取特定货币在指定日期的汇率 - Public endpoint
     */
    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam String currency,
            @RequestParam(required = false) String date) {

        LocalDate effectiveDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        BigDecimal rate = exchangeRateService.getExchangeRate(currency, effectiveDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("currency", currency);
        data.put("date", effectiveDate);
        data.put("rateToUsd", rate);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有货币的当前最新汇率 - Public endpoint
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestRates() {
        List<ExchangeRate> rates = exchangeRateService.getRatesByDate(LocalDate.now());

        // 如果今天没有汇率，获取所有启用的汇率并按货币分组取最新
        if (rates.isEmpty()) {
            rates = exchangeRateService.getAllActiveRates();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rates);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取特定日期的所有汇率 - Public endpoint
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<Map<String, Object>> getRatesByDate(@PathVariable String date) {
        LocalDate effectiveDate = LocalDate.parse(date);
        List<ExchangeRate> rates = exchangeRateService.getRatesByDate(effectiveDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rates);

        return ResponseEntity.ok(response);
    }

    /**
     * 创建新的汇率记录 - Admin only
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createExchangeRate(
            @RequestBody ExchangeRate exchangeRate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        try {
            ExchangeRate created = exchangeRateService.createExchangeRate(exchangeRate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "汇率创建成功");
            response.put("data", created);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新汇率记录 - Admin only
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateExchangeRate(
            @PathVariable Long id,
            @RequestBody ExchangeRate exchangeRate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        try {
            ExchangeRate updated = exchangeRateService.updateExchangeRate(id, exchangeRate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "汇率更新成功");
            response.put("data", updated);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除汇率记录 - Admin only
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteExchangeRate(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        try {
            exchangeRateService.deleteExchangeRate(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "汇率删除成功");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 停用汇率记录 - Admin only
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateExchangeRate(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        try {
            ExchangeRate deactivated = exchangeRateService.deactivateExchangeRate(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "汇率已停用");
            response.put("data", deactivated);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 初始化默认汇率 - Admin only
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeDefaultRates(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireAdmin(authHeader);

        exchangeRateService.initializeDefaultRates();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "默认汇率初始化成功");

        return ResponseEntity.ok(response);
    }
}
