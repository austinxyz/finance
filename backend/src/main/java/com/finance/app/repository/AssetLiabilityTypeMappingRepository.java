package com.finance.app.repository;

import com.finance.app.model.AssetLiabilityTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetLiabilityTypeMappingRepository extends JpaRepository<AssetLiabilityTypeMapping, Long> {

    // 根据负债类型查找对应的资产类型
    Optional<AssetLiabilityTypeMapping> findByLiabilityType(String liabilityType);

    // 根据资产类型查找所有对应的负债类型
    List<AssetLiabilityTypeMapping> findByAssetType(String assetType);

    // 获取所有映射关系
    List<AssetLiabilityTypeMapping> findAll();
}
