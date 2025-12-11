package com.finance.app.repository;

import com.finance.app.model.ExpenseCategoryMinor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支出子分类Repository
 */
@Repository
public interface ExpenseCategoryMinorRepository extends JpaRepository<ExpenseCategoryMinor, Long> {

    /**
     * 根据大类ID查找所有子分类（按排序顺序）
     */
    List<ExpenseCategoryMinor> findByMajorCategoryIdOrderBySortOrder(Long majorCategoryId);

    /**
     * 根据大类ID查找启用的子分类（按排序顺序）
     */
    List<ExpenseCategoryMinor> findByMajorCategoryIdAndIsActiveTrueOrderBySortOrder(Long majorCategoryId);

    /**
     * 根据大类ID和名称查找子分类（用于唯一性校验）
     */
    Optional<ExpenseCategoryMinor> findByMajorCategoryIdAndName(Long majorCategoryId, String name);

    /**
     * 检查子分类是否有关联的支出记录
     */
    @Query("SELECT COUNT(r) > 0 FROM ExpenseRecord r WHERE r.minorCategoryId = :minorCategoryId")
    boolean hasExpenseRecords(@Param("minorCategoryId") Long minorCategoryId);

    /**
     * 统计子分类关联的记录数量
     */
    @Query("SELECT COUNT(r) FROM ExpenseRecord r WHERE r.minorCategoryId = :minorCategoryId")
    long countExpenseRecords(@Param("minorCategoryId") Long minorCategoryId);
}
