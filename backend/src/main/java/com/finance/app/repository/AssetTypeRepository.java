package com.finance.app.repository;

import com.finance.app.model.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 资产大类Repository
 */
@Repository
public interface AssetTypeRepository extends JpaRepository<AssetType, Long> {

    /**
     * 根据type代码查询
     * @param type 类型代码（如STOCKS）
     */
    Optional<AssetType> findByType(String type);

    /**
     * 按显示顺序获取所有大类
     */
    List<AssetType> findAllByOrderByDisplayOrderAsc();

    /**
     * 获取所有投资类型
     * @return 投资类型列表（is_investment = TRUE）
     */
    List<AssetType> findByIsInvestmentTrueOrderByDisplayOrderAsc();

    /**
     * 获取所有非投资类型
     * @return 非投资类型列表（is_investment = FALSE）
     */
    List<AssetType> findByIsInvestmentFalseOrderByDisplayOrderAsc();

    /**
     * 检查type代码是否存在
     */
    boolean existsByType(String type);
}
