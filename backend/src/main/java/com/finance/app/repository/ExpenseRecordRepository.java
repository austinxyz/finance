package com.finance.app.repository;

import com.finance.app.model.ExpenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支出记录Repository
 */
@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Long> {

    /**
     * 根据家庭ID和期间查询支出记录
     */
    List<ExpenseRecord> findByFamilyIdAndExpensePeriod(Long familyId, String expensePeriod);

    /**
     * 根据家庭ID和期间范围查询支出记录
     */
    @Query("SELECT r FROM ExpenseRecord r " +
           "WHERE r.familyId = :familyId " +
           "AND r.expensePeriod >= :startPeriod " +
           "AND r.expensePeriod <= :endPeriod " +
           "ORDER BY r.expensePeriod DESC, r.majorCategoryId, r.minorCategoryId")
    List<ExpenseRecord> findByFamilyIdAndPeriodRange(
        @Param("familyId") Long familyId,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    /**
     * 根据家庭ID、期间和子分类ID查找单条记录（用于唯一性校验）
     * @deprecated 使用 findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency 替代
     */
    @Deprecated
    Optional<ExpenseRecord> findFirstByFamilyIdAndExpensePeriodAndMinorCategoryId(
        Long familyId,
        String expensePeriod,
        Long minorCategoryId
    );

    /**
     * 根据家庭ID、期间、子分类ID和货币查找记录（支持多货币）
     */
    Optional<ExpenseRecord> findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(
        Long familyId,
        String expensePeriod,
        Long minorCategoryId,
        String currency
    );

    /**
     * 根据家庭ID和大类ID查询历史记录
     */
    List<ExpenseRecord> findByFamilyIdAndMajorCategoryIdOrderByExpensePeriodDesc(
        Long familyId,
        Long majorCategoryId
    );

    /**
     * 根据家庭ID和子分类ID查询历史记录
     */
    List<ExpenseRecord> findByFamilyIdAndMinorCategoryIdOrderByExpensePeriodDesc(
        Long familyId,
        Long minorCategoryId
    );

    /**
     * 查询上个月的支出记录
     */
    @Query("SELECT r FROM ExpenseRecord r " +
           "WHERE r.familyId = :familyId " +
           "AND r.expensePeriod = :lastPeriod")
    List<ExpenseRecord> findLastMonthRecords(
        @Param("familyId") Long familyId,
        @Param("lastPeriod") String lastPeriod
    );

    /**
     * 根据家庭ID和期间范围查询（别名方法，用于ExpenseAnalysisService）
     */
    default List<ExpenseRecord> findByFamilyIdAndExpensePeriodBetween(Long familyId, String startPeriod, String endPeriod) {
        return findByFamilyIdAndPeriodRange(familyId, startPeriod, endPeriod);
    }

    /**
     * 根据家庭ID、期间和子分类ID查询所有记录（返回List而非Optional）
     */
    List<ExpenseRecord> findByFamilyIdAndExpensePeriodAndMinorCategoryId(
        Long familyId,
        String expensePeriod,
        Long minorCategoryId
    );

    /**
     * 查询指定家庭、大类、币种、期间的支出总额
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ExpenseRecord r " +
           "WHERE r.familyId = :familyId " +
           "AND r.majorCategoryId = :majorCategoryId " +
           "AND r.currency = :currency " +
           "AND r.expensePeriod = :period")
    Optional<java.math.BigDecimal> sumByFamilyAndCategoryAndPeriod(
        @Param("familyId") Long familyId,
        @Param("majorCategoryId") Long majorCategoryId,
        @Param("currency") String currency,
        @Param("period") String period
    );

    /**
     * 批量查询多个小类和期间的支出记录（优化性能）
     */
    List<ExpenseRecord> findByFamilyIdAndExpensePeriodAndCurrencyAndMinorCategoryIdIn(
        Long familyId, String expensePeriod, String currency, List<Long> minorCategoryIds);
}
