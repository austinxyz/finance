package com.finance.app.repository;

import com.finance.app.model.ExpenseCategoryMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支出大类Repository
 */
@Repository
public interface ExpenseCategoryMajorRepository extends JpaRepository<ExpenseCategoryMajor, Long> {

    /**
     * 根据编码查找大类
     */
    Optional<ExpenseCategoryMajor> findByCode(String code);

    /**
     * 查找所有启用的大类（按排序顺序）
     */
    List<ExpenseCategoryMajor> findByIsActiveTrueOrderBySortOrder();

    /**
     * 查找所有大类（按排序顺序）
     */
    List<ExpenseCategoryMajor> findAllByOrderBySortOrder();
}
