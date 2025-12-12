package com.finance.app.repository;

import com.finance.app.model.AnnualExpenseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 年度支出汇总Repository
 */
@Repository
public interface AnnualExpenseSummaryRepository extends JpaRepository<AnnualExpenseSummary, Long> {

    /**
     * 查询大类级别的年度汇总 (minor_category_id IS NULL)
     * @param familyId 家庭ID
     * @param summaryYear 汇总年份
     * @param currency 货币
     * @return 大类级别的汇总列表
     */
    @Query("SELECT s FROM AnnualExpenseSummary s " +
           "WHERE s.familyId = :familyId " +
           "AND s.summaryYear = :summaryYear " +
           "AND s.currency = :currency " +
           "AND s.minorCategoryId IS NULL " +
           "AND s.majorCategoryId != 0 " +
           "ORDER BY s.actualExpenseAmount DESC")
    List<AnnualExpenseSummary> findMajorCategorySummary(
        @Param("familyId") Long familyId,
        @Param("summaryYear") Integer summaryYear,
        @Param("currency") String currency
    );

    /**
     * 查询小类级别的年度汇总 (minor_category_id IS NOT NULL)
     * @param familyId 家庭ID
     * @param summaryYear 汇总年份
     * @param currency 货币
     * @return 小类级别的汇总列表
     */
    @Query("SELECT s FROM AnnualExpenseSummary s " +
           "WHERE s.familyId = :familyId " +
           "AND s.summaryYear = :summaryYear " +
           "AND s.currency = :currency " +
           "AND s.minorCategoryId IS NOT NULL " +
           "ORDER BY s.majorCategoryId, s.actualExpenseAmount DESC")
    List<AnnualExpenseSummary> findMinorCategorySummary(
        @Param("familyId") Long familyId,
        @Param("summaryYear") Integer summaryYear,
        @Param("currency") String currency
    );

    /**
     * 查询年度总计 (major_category_id = 0)
     * @param familyId 家庭ID
     * @param summaryYear 汇总年份
     * @param currency 货币
     * @return 年度总计记录
     */
    @Query("SELECT s FROM AnnualExpenseSummary s " +
           "WHERE s.familyId = :familyId " +
           "AND s.summaryYear = :summaryYear " +
           "AND s.currency = :currency " +
           "AND s.majorCategoryId = 0")
    AnnualExpenseSummary findTotalSummary(
        @Param("familyId") Long familyId,
        @Param("summaryYear") Integer summaryYear,
        @Param("currency") String currency
    );

    /**
     * 查询某年度所有记录（包括大类、小类、总计）
     * @param familyId 家庭ID
     * @param summaryYear 汇总年份
     * @return 所有记录列表
     */
    List<AnnualExpenseSummary> findByFamilyIdAndSummaryYear(
        Long familyId,
        Integer summaryYear
    );

    /**
     * 删除某年度的所有汇总数据
     * @param familyId 家庭ID
     * @param summaryYear 汇总年份
     */
    void deleteByFamilyIdAndSummaryYear(Long familyId, Integer summaryYear);
}
