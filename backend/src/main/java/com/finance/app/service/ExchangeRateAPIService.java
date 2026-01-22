package com.finance.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 第三方汇率API服务
 * 使用 Frankfurter API (https://www.frankfurter.app/)
 * - 免费无需注册
 * - 数据来源：欧洲央行
 * - 支持历史汇率查询
 */
@Service
@Slf4j
public class ExchangeRateAPIService {

    private static final String API_BASE_URL = "https://api.frankfurter.app";
    private static final String[] SUPPORTED_CURRENCIES = {"CNY", "EUR", "GBP", "JPY", "AUD", "CAD"};

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从API获取指定日期的汇率
     * @param date 日期
     * @return Map<货币代码, 对美元汇率>
     */
    public Map<String, BigDecimal> fetchRatesForDate(LocalDate date) {
        Map<String, BigDecimal> rates = new HashMap<>();

        try {
            // Frankfurter API URL: https://api.frankfurter.app/{date}?from=USD&to=CNY,EUR,GBP,JPY,AUD,CAD
            String url = String.format("%s/%s?from=USD&to=%s",
                API_BASE_URL,
                date.toString(),
                String.join(",", SUPPORTED_CURRENCIES)
            );

            log.info("Fetching exchange rates from API: {}", url);

            // 调用API
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                log.error("API response is null");
                return rates;
            }

            // 解析JSON响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode ratesNode = root.get("rates");

            if (ratesNode == null) {
                log.error("No rates found in API response");
                return rates;
            }

            // Frankfurter API 返回的是 1 USD = X 货币
            // 我们需要的是 1 货币 = X USD (即汇率的倒数)
            for (String currency : SUPPORTED_CURRENCIES) {
                if (ratesNode.has(currency)) {
                    BigDecimal rateFromUsd = new BigDecimal(ratesNode.get(currency).asText());
                    // 计算对美元的汇率 (1 货币 = X USD)
                    BigDecimal rateToUsd = BigDecimal.ONE.divide(rateFromUsd, 8, RoundingMode.HALF_UP);
                    rates.put(currency, rateToUsd);
                    log.info("Fetched rate for {}: 1 {} = {} USD", currency, currency, rateToUsd);
                }
            }

            log.info("Successfully fetched {} exchange rates", rates.size());

        } catch (Exception e) {
            log.error("Error fetching exchange rates from API", e);
            throw new RuntimeException("获取汇率失败: " + e.getMessage(), e);
        }

        return rates;
    }

    /**
     * 获取当前最新汇率
     * @return Map<货币代码, 对美元汇率>
     */
    public Map<String, BigDecimal> fetchLatestRates() {
        return fetchRatesForDate(LocalDate.now());
    }

    /**
     * 检查API是否可用
     * @return true if API is available
     */
    public boolean isAPIAvailable() {
        try {
            String url = API_BASE_URL + "/latest";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.error("API health check failed", e);
            return false;
        }
    }
}
