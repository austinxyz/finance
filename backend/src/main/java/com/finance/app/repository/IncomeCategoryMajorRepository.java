package com.finance.app.repository;

import com.finance.app.model.IncomeCategoryMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeCategoryMajorRepository extends JpaRepository<IncomeCategoryMajor, Long> {

    /**
     * 查找所有启用的大类，按显示顺序排序
     */
    List<IncomeCategoryMajor> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * 按显示顺序查找所有大类
     */
    List<IncomeCategoryMajor> findAllByOrderByDisplayOrderAsc();
}
