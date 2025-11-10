package com.finance.app.repository;

import com.finance.app.model.NetAssetCategoryLiabilityTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetAssetCategoryLiabilityTypeMappingRepository extends JpaRepository<NetAssetCategoryLiabilityTypeMapping, Long> {

    // 根据净资产类别ID查找所有负债类型映射
    List<NetAssetCategoryLiabilityTypeMapping> findByNetAssetCategoryId(Long netAssetCategoryId);

    // 根据负债类型查找所有净资产类别映射
    List<NetAssetCategoryLiabilityTypeMapping> findByLiabilityType(String liabilityType);
}
