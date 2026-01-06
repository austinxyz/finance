package com.finance.app.repository;

import com.finance.app.model.IncomeBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeBudgetRepository extends JpaRepository<IncomeBudget, Long> {

    /**
     * 查找指定家庭、年份的所有预算
     */
    List<IncomeBudget> findByFamilyIdAndYear(Long familyId, Integer year);

    /**
     * 查找指定家庭、用户、年份的所有预算
     */
    List<IncomeBudget> findByFamilyIdAndUserIdAndYear(Long familyId, Long userId, Integer year);

    /**
     * 查找指定预算
     */
    Optional<IncomeBudget> findByFamilyIdAndUserIdAndMajorCategoryIdAndMinorCategoryIdAndYearAndCurrency(
        Long familyId,
        Long userId,
        Long majorCategoryId,
        Long minorCategoryId,
        Integer year,
        String currency
    );

    /**
     * 删除指定家庭、年份的所有预算
     */
    void deleteByFamilyIdAndYear(Long familyId, Integer year);
}
