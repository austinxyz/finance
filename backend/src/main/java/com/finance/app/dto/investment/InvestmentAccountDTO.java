package com.finance.app.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投资账户DTO（用于投资管理页面账户列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentAccountDTO {

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 资产分类ID
     */
    private Long categoryId;

    /**
     * 资产分类名称
     */
    private String categoryName;

    /**
     * 资产分类类型
     */
    private String categoryType;

    /**
     * 资产分类图标
     */
    private String categoryIcon;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 货币代码
     */
    private String currency;

    /**
     * 所属金融机构
     */
    private String institution;

    /**
     * 记录数量（该账户的交易记录总数）
     */
    private Integer recordCount;

    /**
     * 最新总值（从asset_records获取最近的值）
     */
    private Double latestValue;

    /**
     * 最新记录日期
     */
    private String latestRecordDate;
}
