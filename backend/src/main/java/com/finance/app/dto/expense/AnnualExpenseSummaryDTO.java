package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 年度支出汇总DTO (包含资产/负债调整)
 */
public class AnnualExpenseSummaryDTO {

    private Integer summaryYear;
    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryIcon;
    private String majorCategoryCode;
    private Long minorCategoryId;
    private String minorCategoryName;

    // 金额字段
    private BigDecimal baseExpenseAmount;        // 基础支出（未调整）
    private BigDecimal assetAdjustment;          // 资产调整
    private BigDecimal liabilityAdjustment;      // 负债调整
    private BigDecimal actualExpenseAmount;      // 实际支出（调整后）
    private String currency;

    // 调整详情 (JSON字符串)
    private String adjustmentDetails;

    // Constructors
    public AnnualExpenseSummaryDTO() {
    }

    public AnnualExpenseSummaryDTO(Integer summaryYear, Long majorCategoryId,
                                   String majorCategoryName, String majorCategoryIcon,
                                   String majorCategoryCode, Long minorCategoryId,
                                   String minorCategoryName, BigDecimal baseExpenseAmount,
                                   BigDecimal assetAdjustment, BigDecimal liabilityAdjustment,
                                   BigDecimal actualExpenseAmount, String currency,
                                   String adjustmentDetails) {
        this.summaryYear = summaryYear;
        this.majorCategoryId = majorCategoryId;
        this.majorCategoryName = majorCategoryName;
        this.majorCategoryIcon = majorCategoryIcon;
        this.majorCategoryCode = majorCategoryCode;
        this.minorCategoryId = minorCategoryId;
        this.minorCategoryName = minorCategoryName;
        this.baseExpenseAmount = baseExpenseAmount;
        this.assetAdjustment = assetAdjustment;
        this.liabilityAdjustment = liabilityAdjustment;
        this.actualExpenseAmount = actualExpenseAmount;
        this.currency = currency;
        this.adjustmentDetails = adjustmentDetails;
    }

    // Getters and Setters
    public Integer getSummaryYear() {
        return summaryYear;
    }

    public void setSummaryYear(Integer summaryYear) {
        this.summaryYear = summaryYear;
    }

    public Long getMajorCategoryId() {
        return majorCategoryId;
    }

    public void setMajorCategoryId(Long majorCategoryId) {
        this.majorCategoryId = majorCategoryId;
    }

    public String getMajorCategoryName() {
        return majorCategoryName;
    }

    public void setMajorCategoryName(String majorCategoryName) {
        this.majorCategoryName = majorCategoryName;
    }

    public String getMajorCategoryIcon() {
        return majorCategoryIcon;
    }

    public void setMajorCategoryIcon(String majorCategoryIcon) {
        this.majorCategoryIcon = majorCategoryIcon;
    }

    public String getMajorCategoryCode() {
        return majorCategoryCode;
    }

    public void setMajorCategoryCode(String majorCategoryCode) {
        this.majorCategoryCode = majorCategoryCode;
    }

    public Long getMinorCategoryId() {
        return minorCategoryId;
    }

    public void setMinorCategoryId(Long minorCategoryId) {
        this.minorCategoryId = minorCategoryId;
    }

    public String getMinorCategoryName() {
        return minorCategoryName;
    }

    public void setMinorCategoryName(String minorCategoryName) {
        this.minorCategoryName = minorCategoryName;
    }

    public BigDecimal getBaseExpenseAmount() {
        return baseExpenseAmount;
    }

    public void setBaseExpenseAmount(BigDecimal baseExpenseAmount) {
        this.baseExpenseAmount = baseExpenseAmount;
    }

    public BigDecimal getAssetAdjustment() {
        return assetAdjustment;
    }

    public void setAssetAdjustment(BigDecimal assetAdjustment) {
        this.assetAdjustment = assetAdjustment;
    }

    public BigDecimal getLiabilityAdjustment() {
        return liabilityAdjustment;
    }

    public void setLiabilityAdjustment(BigDecimal liabilityAdjustment) {
        this.liabilityAdjustment = liabilityAdjustment;
    }

    public BigDecimal getActualExpenseAmount() {
        return actualExpenseAmount;
    }

    public void setActualExpenseAmount(BigDecimal actualExpenseAmount) {
        this.actualExpenseAmount = actualExpenseAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAdjustmentDetails() {
        return adjustmentDetails;
    }

    public void setAdjustmentDetails(String adjustmentDetails) {
        this.adjustmentDetails = adjustmentDetails;
    }
}
