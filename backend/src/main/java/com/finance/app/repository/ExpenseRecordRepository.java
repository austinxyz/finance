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
     * 查询某期间某大类的总支出
     */
    @Query("SELECT COALESCE(SUM(r.amountInBaseCurrency), 0) FROM ExpenseRecord r " +
           "WHERE r.familyId = :familyId " +
           "AND r.expensePeriod = :period " +
           "AND r.majorCategoryId = :majorCategoryId")
    java.math.BigDecimal sumByPeriodAndMajorCategory(
        @Param("familyId") Long familyId,
        @Param("period") String period,
        @Param("majorCategoryId") Long majorCategoryId
    );

    /**
     * 查询某期间总支出
     */
    @Query("SELECT COALESCE(SUM(r.amountInBaseCurrency), 0) FROM ExpenseRecord r " +
           "WHERE r.familyId = :familyId " +
           "AND r.expensePeriod = :period")
    java.math.BigDecimal sumByPeriod(
        @Param("familyId") Long familyId,
        @Param("period") String period
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
}
