package com.finance.app.repository;

import com.finance.app.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // 查找所有启用的汇率
    List<ExchangeRate> findByIsActiveTrueOrderByEffectiveDateDesc();

    // 查找特定货币的所有汇率
    List<ExchangeRate> findByCurrencyOrderByEffectiveDateDesc(String currency);

    // 查找特定货币在特定日期或之前的最新汇率
    @Query("SELECT e FROM ExchangeRate e WHERE e.currency = :currency " +
           "AND e.effectiveDate <= :date AND e.isActive = true " +
           "ORDER BY e.effectiveDate DESC")
    List<ExchangeRate> findLatestRateByCurrencyAndDate(@Param("currency") String currency,
                                                        @Param("date") LocalDate date);

    // 查找特定日期的所有汇率
    List<ExchangeRate> findByEffectiveDateOrderByCurrency(LocalDate effectiveDate);

    // 查找特定货币和日期的汇率
    Optional<ExchangeRate> findByCurrencyAndEffectiveDate(String currency, LocalDate effectiveDate);
}
