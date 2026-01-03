package com.finance.app.repository;

import com.finance.app.model.ExpenseBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支出预算Repository
 */
@Repository
public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, Long> {

    /**
     * 查询指定家庭、年份、货币的所有预算
     */
    List<ExpenseBudget> findByFamilyIdAndBudgetYearAndCurrency(
        Long familyId, Integer budgetYear, String currency);

    /**
     * 查询指定家庭、年份的所有预算（所有货币）
     */
    List<ExpenseBudget> findByFamilyIdAndBudgetYear(Long familyId, Integer budgetYear);

    /**
     * 查询特定预算记录
     */
    Optional<ExpenseBudget> findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(
        Long familyId, Integer budgetYear, Long minorCategoryId, String currency);

    /**
     * 删除指定家庭、年份、货币的所有预算
     */
    void deleteByFamilyIdAndBudgetYearAndCurrency(
        Long familyId, Integer budgetYear, String currency);

    /**
     * 查询指定家庭、年份、大类的所有预算
     */
    @Query("SELECT b FROM ExpenseBudget b " +
           "WHERE b.familyId = :familyId " +
           "AND b.budgetYear = :year " +
           "AND b.minorCategoryId IN (SELECT mc.id FROM ExpenseCategoryMinor mc WHERE mc.majorCategoryId = :majorCategoryId)")
    List<ExpenseBudget> findByFamilyAndYearAndCategory(
        @Param("familyId") Long familyId,
        @Param("year") Integer year,
        @Param("majorCategoryId") Long majorCategoryId
    );

    /**
     * 批量查询多个小类的预算（优化性能）
     */
    List<ExpenseBudget> findByFamilyIdAndBudgetYearAndCurrencyAndMinorCategoryIdIn(
        Long familyId, Integer budgetYear, String currency, List<Long> minorCategoryIds);
}
