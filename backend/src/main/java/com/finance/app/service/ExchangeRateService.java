package com.finance.app.service;

import com.finance.app.model.ExchangeRate;
import com.finance.app.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExchangeRateService {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * 获取所有启用的汇率，按生效日期降序排列
     */
    public List<ExchangeRate> getAllActiveRates() {
        return exchangeRateRepository.findByIsActiveTrueOrderByEffectiveDateDesc();
    }

    /**
     * 获取特定货币的所有汇率
     */
    public List<ExchangeRate> getRatesByCurrency(String currency) {
        return exchangeRateRepository.findByCurrencyOrderByEffectiveDateDesc(currency);
    }

    /**
     * 获取特定货币在指定日期或之前的最新汇率
     */
    public BigDecimal getExchangeRate(String currency, LocalDate date) {
        if (currency == null || currency.equalsIgnoreCase("USD")) {
            return BigDecimal.ONE;
        }

        List<ExchangeRate> rates = exchangeRateRepository.findLatestRateByCurrencyAndDate(
            currency.toUpperCase(),
            date
        );

        if (!rates.isEmpty()) {
            return rates.get(0).getRateToUsd();
        }

        // 如果没有找到指定日期的汇率，返回默认值1.0
        return BigDecimal.ONE;
    }

    /**
     * 获取特定货币的当前最新汇率
     */
    public BigDecimal getCurrentExchangeRate(String currency) {
        return getExchangeRate(currency, LocalDate.now());
    }

    /**
     * 获取特定日期的所有汇率
     */
    public List<ExchangeRate> getRatesByDate(LocalDate effectiveDate) {
        return exchangeRateRepository.findByEffectiveDateOrderByCurrency(effectiveDate);
    }

    /**
     * 创建新的汇率记录
     */
    @Transactional
    public ExchangeRate createExchangeRate(ExchangeRate exchangeRate) {
        // 检查是否已存在相同货币和日期的汇率
        Optional<ExchangeRate> existing = exchangeRateRepository.findByCurrencyAndEffectiveDate(
            exchangeRate.getCurrency(),
            exchangeRate.getEffectiveDate()
        );

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                "汇率已存在: " + exchangeRate.getCurrency() + " - " + exchangeRate.getEffectiveDate()
            );
        }

        return exchangeRateRepository.save(exchangeRate);
    }

    /**
     * 更新汇率记录
     */
    @Transactional
    public ExchangeRate updateExchangeRate(Long id, ExchangeRate updatedRate) {
        ExchangeRate existingRate = exchangeRateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("汇率记录不存在: " + id));

        // 如果货币或日期发生变化，检查是否会导致重复
        if (!existingRate.getCurrency().equals(updatedRate.getCurrency()) ||
            !existingRate.getEffectiveDate().equals(updatedRate.getEffectiveDate())) {

            Optional<ExchangeRate> duplicate = exchangeRateRepository.findByCurrencyAndEffectiveDate(
                updatedRate.getCurrency(),
                updatedRate.getEffectiveDate()
            );

            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                throw new IllegalArgumentException(
                    "汇率已存在: " + updatedRate.getCurrency() + " - " + updatedRate.getEffectiveDate()
                );
            }
        }

        existingRate.setCurrency(updatedRate.getCurrency());
        existingRate.setRateToUsd(updatedRate.getRateToUsd());
        existingRate.setEffectiveDate(updatedRate.getEffectiveDate());
        existingRate.setSource(updatedRate.getSource());
        existingRate.setNotes(updatedRate.getNotes());
        existingRate.setIsActive(updatedRate.getIsActive());

        return exchangeRateRepository.save(existingRate);
    }

    /**
     * 删除汇率记录
     */
    @Transactional
    public void deleteExchangeRate(Long id) {
        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("汇率记录不存在: " + id));

        exchangeRateRepository.delete(exchangeRate);
    }

    /**
     * 停用汇率记录（软删除）
     */
    @Transactional
    public ExchangeRate deactivateExchangeRate(Long id) {
        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("汇率记录不存在: " + id));

        exchangeRate.setIsActive(false);
        return exchangeRateRepository.save(exchangeRate);
    }

    /**
     * 初始化默认汇率
     */
    @Transactional
    public void initializeDefaultRates() {
        LocalDate today = LocalDate.now();

        // 检查是否已有今天的汇率
        List<ExchangeRate> existingRates = exchangeRateRepository.findByEffectiveDateOrderByCurrency(today);
        if (!existingRates.isEmpty()) {
            return; // 已有数据，不需要初始化
        }

        // 创建默认汇率
        createDefaultRate("CNY", new BigDecimal("0.14"), today, "系统初始化");
        createDefaultRate("EUR", new BigDecimal("1.08"), today, "系统初始化");
        createDefaultRate("GBP", new BigDecimal("1.27"), today, "系统初始化");
        createDefaultRate("JPY", new BigDecimal("0.0067"), today, "系统初始化");
        createDefaultRate("AUD", new BigDecimal("0.65"), today, "系统初始化");
        createDefaultRate("CAD", new BigDecimal("0.72"), today, "系统初始化");
    }

    private void createDefaultRate(String currency, BigDecimal rate, LocalDate date, String source) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCurrency(currency);
        exchangeRate.setRateToUsd(rate);
        exchangeRate.setEffectiveDate(date);
        exchangeRate.setSource(source);
        exchangeRate.setIsActive(true);
        exchangeRateRepository.save(exchangeRate);
    }
}
