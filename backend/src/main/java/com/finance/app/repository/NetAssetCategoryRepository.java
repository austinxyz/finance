package com.finance.app.repository;

import com.finance.app.model.NetAssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetAssetCategoryRepository extends JpaRepository<NetAssetCategory, Long> {

    // 根据代码查找
    Optional<NetAssetCategory> findByCode(String code);

    // 获取所有净资产类别，按显示顺序排序
    List<NetAssetCategory> findAllByOrderByDisplayOrderAsc();
}
