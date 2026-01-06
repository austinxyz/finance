package com.finance.app.repository;

import com.finance.app.model.IncomeCategoryMinor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeCategoryMinorRepository extends JpaRepository<IncomeCategoryMinor, Long> {

    /**
     * 查找指定大类下的所有小类
     */
    List<IncomeCategoryMinor> findByMajorCategoryId(Long majorCategoryId);

    /**
     * 查找指定大类下的启用小类
     */
    List<IncomeCategoryMinor> findByMajorCategoryIdAndIsActiveTrue(Long majorCategoryId);

    /**
     * 查找所有启用的小类
     */
    List<IncomeCategoryMinor> findByIsActiveTrue();
}
