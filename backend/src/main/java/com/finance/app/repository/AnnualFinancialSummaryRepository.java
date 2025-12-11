package com.finance.app.repository;

import com.finance.app.model.AnnualFinancialSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年度财务摘要Repository
 */
@Repository
public interface AnnualFinancialSummaryRepository extends JpaRepository<AnnualFinancialSummary, Long> {

    /**
     * 根据家庭ID查询所有年度摘要，按年份降序排列
     */
    List<AnnualFinancialSummary> findByFamilyIdOrderByYearDesc(Long familyId);

    /**
     * 根据家庭ID和年份查询摘要
     */
    Optional<AnnualFinancialSummary> findByFamilyIdAndYear(Long familyId, Integer year);

    /**
     * 根据家庭ID查询指定年份范围的摘要
     */
    @Query("SELECT a FROM AnnualFinancialSummary a WHERE a.familyId = :familyId " +
           "AND a.year BETWEEN :startYear AND :endYear ORDER BY a.year DESC")
    List<AnnualFinancialSummary> findByFamilyIdAndYearRange(
            @Param("familyId") Long familyId,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear);

    /**
     * 根据家庭ID查询最近N年的摘要（只返回有数据的年份，net_worth != 0）
     */
    @Query(value = "SELECT * FROM annual_financial_summary WHERE family_id = :familyId " +
                   "AND (total_assets > 0 OR total_liabilities > 0) " +
                   "ORDER BY year DESC LIMIT :limit", nativeQuery = true)
    List<AnnualFinancialSummary> findRecentYears(@Param("familyId") Long familyId, @Param("limit") int limit);

    /**
     * 删除家庭指定年份的摘要
     */
    void deleteByFamilyIdAndYear(Long familyId, Integer year);
}
