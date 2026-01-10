package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.NetAssetCategory;
import com.finance.app.model.NetAssetCategoryAssetTypeMapping;
import com.finance.app.model.NetAssetCategoryLiabilityTypeMapping;
import com.finance.app.repository.LiabilityRecordRepository;
import com.finance.app.repository.NetAssetCategoryRepository;
import com.finance.app.repository.NetAssetCategoryAssetTypeMappingRepository;
import com.finance.app.repository.NetAssetCategoryLiabilityTypeMappingRepository;
import com.finance.app.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
@CrossOrigin
public class DataMigrationController {

    private final LiabilityRecordRepository liabilityRecordRepository;
    private final NetAssetCategoryRepository netAssetCategoryRepository;
    private final NetAssetCategoryAssetTypeMappingRepository assetTypeMappingRepository;
    private final NetAssetCategoryLiabilityTypeMappingRepository liabilityTypeMappingRepository;
    private final AuthHelper authHelper;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Exchange rates to USD (base currency)
    private static final Map<String, BigDecimal> EXCHANGE_RATES = new HashMap<>();
    static {
        EXCHANGE_RATES.put("USD", BigDecimal.valueOf(1.0));
        EXCHANGE_RATES.put("CNY", BigDecimal.valueOf(0.14));  // 1 CNY = 0.14 USD
        EXCHANGE_RATES.put("EUR", BigDecimal.valueOf(1.08));  // 1 EUR = 1.08 USD
        EXCHANGE_RATES.put("GBP", BigDecimal.valueOf(1.27));  // 1 GBP = 1.27 USD
        EXCHANGE_RATES.put("JPY", BigDecimal.valueOf(0.0067));  // 1 JPY = 0.0067 USD
        EXCHANGE_RATES.put("HKD", BigDecimal.valueOf(0.13));  // 1 HKD = 0.13 USD
        EXCHANGE_RATES.put("AUD", BigDecimal.valueOf(0.65));  // 1 AUD = 0.65 USD
        EXCHANGE_RATES.put("CAD", BigDecimal.valueOf(0.72));  // 1 CAD = 0.72 USD
        EXCHANGE_RATES.put("SGD", BigDecimal.valueOf(0.74));  // 1 SGD = 0.74 USD
        EXCHANGE_RATES.put("KRW", BigDecimal.valueOf(0.00075));  // 1 KRW = 0.00075 USD
    }

    @PostMapping("/fix-liability-exchange-rates")
    public ApiResponse<Map<String, Object>> fixLiabilityExchangeRates(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Verify admin role
        authHelper.requireAdmin(authHeader);

        // This endpoint is deprecated - exchange_rate and balance_in_base_currency fields have been removed
        // Currency conversion is now done dynamically using the exchange_rates table
        Map<String, Object> result = new HashMap<>();
        result.put("message", "This endpoint is deprecated. Exchange rate conversion is now done dynamically.");
        result.put("totalRecords", 0);
        result.put("updated", 0);
        result.put("skipped", 0);

        return ApiResponse.success("Exchange rates fixed successfully", result);
    }

    @PostMapping("/init-net-asset-categories")
    @Transactional
    public ApiResponse<Map<String, Object>> initNetAssetCategories(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Verify admin role
        authHelper.requireAdmin(authHeader);

        // 清空现有数据
        liabilityTypeMappingRepository.deleteAll();
        assetTypeMappingRepository.deleteAll();
        netAssetCategoryRepository.deleteAll();

        // 创建净资产类别
        // 1. 房地产净值
        NetAssetCategory realEstateNet = new NetAssetCategory();
        realEstateNet.setName("房地产净值");
        realEstateNet.setCode("REAL_ESTATE_NET");
        realEstateNet.setDescription("房地产总值减去房贷");
        realEstateNet.setDisplayOrder(1);
        realEstateNet.setColor("#10b981");
        realEstateNet = netAssetCategoryRepository.save(realEstateNet);

        // 2. 流动资产净值
        NetAssetCategory liquidNet = new NetAssetCategory();
        liquidNet.setName("流动资产净值");
        liquidNet.setCode("LIQUID_NET");
        liquidNet.setDescription("现金类资产减去信用卡和其他短期负债");
        liquidNet.setDisplayOrder(2);
        liquidNet.setColor("#3b82f6");
        liquidNet = netAssetCategoryRepository.save(liquidNet);

        // 3. 投资净值
        NetAssetCategory investmentNet = new NetAssetCategory();
        investmentNet.setName("投资净值");
        investmentNet.setCode("INVESTMENT_NET");
        investmentNet.setDescription("股票和退休基金等投资");
        investmentNet.setDisplayOrder(3);
        investmentNet.setColor("#8b5cf6");
        investmentNet = netAssetCategoryRepository.save(investmentNet);

        // 4. 其他净值
        NetAssetCategory otherNet = new NetAssetCategory();
        otherNet.setName("其他净值");
        otherNet.setCode("OTHER_NET");
        otherNet.setDescription("其他资产");
        otherNet.setDisplayOrder(4);
        otherNet.setColor("#f59e0b");
        otherNet = netAssetCategoryRepository.save(otherNet);

        // 创建映射关系
        int assetMappingCount = 0;
        int liabilityMappingCount = 0;

        // 房地产净值映射
        // 资产: REAL_ESTATE
        NetAssetCategoryAssetTypeMapping reAssetMapping = new NetAssetCategoryAssetTypeMapping();
        reAssetMapping.setNetAssetCategoryId(realEstateNet.getId());
        reAssetMapping.setAssetType("REAL_ESTATE");
        assetTypeMappingRepository.save(reAssetMapping);
        assetMappingCount++;

        // 负债: MORTGAGE, AUTO_LOAN
        NetAssetCategoryLiabilityTypeMapping reMortgageMapping = new NetAssetCategoryLiabilityTypeMapping();
        reMortgageMapping.setNetAssetCategoryId(realEstateNet.getId());
        reMortgageMapping.setLiabilityType("MORTGAGE");
        liabilityTypeMappingRepository.save(reMortgageMapping);
        liabilityMappingCount++;

        NetAssetCategoryLiabilityTypeMapping reAutoLoanMapping = new NetAssetCategoryLiabilityTypeMapping();
        reAutoLoanMapping.setNetAssetCategoryId(realEstateNet.getId());
        reAutoLoanMapping.setLiabilityType("AUTO_LOAN");
        liabilityTypeMappingRepository.save(reAutoLoanMapping);
        liabilityMappingCount++;

        // 流动资产净值映射
        // 资产: CASH
        NetAssetCategoryAssetTypeMapping cashAssetMapping = new NetAssetCategoryAssetTypeMapping();
        cashAssetMapping.setNetAssetCategoryId(liquidNet.getId());
        cashAssetMapping.setAssetType("CASH");
        assetTypeMappingRepository.save(cashAssetMapping);
        assetMappingCount++;

        // 负债: CREDIT_CARD, PERSONAL_LOAN, STUDENT_LOAN, BUSINESS_LOAN, OTHER
        String[] liquidLiabilities = {"CREDIT_CARD", "PERSONAL_LOAN", "STUDENT_LOAN", "BUSINESS_LOAN", "OTHER"};
        for (String liabilityType : liquidLiabilities) {
            NetAssetCategoryLiabilityTypeMapping mapping = new NetAssetCategoryLiabilityTypeMapping();
            mapping.setNetAssetCategoryId(liquidNet.getId());
            mapping.setLiabilityType(liabilityType);
            liabilityTypeMappingRepository.save(mapping);
            liabilityMappingCount++;
        }

        // 投资净值映射
        // 资产: STOCKS, RETIREMENT_FUND
        NetAssetCategoryAssetTypeMapping stocksAssetMapping = new NetAssetCategoryAssetTypeMapping();
        stocksAssetMapping.setNetAssetCategoryId(investmentNet.getId());
        stocksAssetMapping.setAssetType("STOCKS");
        assetTypeMappingRepository.save(stocksAssetMapping);
        assetMappingCount++;

        NetAssetCategoryAssetTypeMapping retirementAssetMapping = new NetAssetCategoryAssetTypeMapping();
        retirementAssetMapping.setNetAssetCategoryId(investmentNet.getId());
        retirementAssetMapping.setAssetType("RETIREMENT_FUND");
        assetTypeMappingRepository.save(retirementAssetMapping);
        assetMappingCount++;

        // 其他净值映射
        // 资产: INSURANCE, CRYPTOCURRENCY, PRECIOUS_METALS, OTHER
        String[] otherAssets = {"INSURANCE", "CRYPTOCURRENCY", "PRECIOUS_METALS", "OTHER"};
        for (String assetType : otherAssets) {
            NetAssetCategoryAssetTypeMapping mapping = new NetAssetCategoryAssetTypeMapping();
            mapping.setNetAssetCategoryId(otherNet.getId());
            mapping.setAssetType(assetType);
            assetTypeMappingRepository.save(mapping);
            assetMappingCount++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("netAssetCategoriesCreated", 4);
        result.put("assetMappingsCreated", assetMappingCount);
        result.put("liabilityMappingsCreated", liabilityMappingCount);

        return ApiResponse.success("Net asset categories initialized successfully", result);
    }

    @PostMapping("/reset-exchange-rates-table")
    public ApiResponse<Map<String, Object>> resetExchangeRatesTable(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Verify admin role
        authHelper.requireAdmin(authHeader);

        try {
            // Drop the existing table
            jdbcTemplate.execute("DROP TABLE IF EXISTS exchange_rates");

            // Create the table with correct structure
            String createTableSQL = """
                CREATE TABLE exchange_rates (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    currency VARCHAR(10) NOT NULL,
                    rate_to_usd DECIMAL(18,8) NOT NULL,
                    effective_date DATE NOT NULL,
                    source VARCHAR(100),
                    notes VARCHAR(500),
                    is_active BOOLEAN DEFAULT true,
                    created_at DATETIME,
                    updated_at DATETIME,
                    UNIQUE KEY unique_currency_date (currency, effective_date)
                )
            """;
            jdbcTemplate.execute(createTableSQL);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Exchange rates table has been reset successfully");

            return ApiResponse.success("Table reset successfully", result);
        } catch (Exception e) {
            return ApiResponse.error("Failed to reset table: " + e.getMessage());
        }
    }
}
