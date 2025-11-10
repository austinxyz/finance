package com.finance.app.repository;

import com.finance.app.model.NetAssetCategoryAssetTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetAssetCategoryAssetTypeMappingRepository extends JpaRepository<NetAssetCategoryAssetTypeMapping, Long> {

    // 根据净资产类别ID查找所有资产类型映射
    List<NetAssetCategoryAssetTypeMapping> findByNetAssetCategoryId(Long netAssetCategoryId);

    // 根据资产类型查找所有净资产类别映射
    List<NetAssetCategoryAssetTypeMapping> findByAssetType(String assetType);
}
