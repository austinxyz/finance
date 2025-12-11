package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 预算执行分析DTO
 */
public class BudgetExecutionDTO {

    private Long minorCategoryId;
    private String minorCategoryName;
    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryIcon;
    private String expenseType;

    private BigDecimal budgetAmount;      // 预算金额
    private BigDecimal actualAmount;      // 实际支出
    private BigDecimal variance;          // 差异（实际-预算）
    private BigDecimal executionRate;     // 执行率（实际/预算 * 100）

    private String currency;

    // Constructors
    public BudgetExecutionDTO() {
    }

    public BudgetExecutionDTO(Long minorCategoryId, String minorCategoryName,
                              Long majorCategoryId, String majorCategoryName,
                              String majorCategoryIcon, String expenseType,
                              BigDecimal budgetAmount, BigDecimal actualAmount,
                              BigDecimal variance, BigDecimal executionRate,
                              String currency) {
        this.minorCategoryId = minorCategoryId;
        this.minorCategoryName = minorCategoryName;
        this.majorCategoryId = majorCategoryId;
        this.majorCategoryName = majorCategoryName;
        this.majorCategoryIcon = majorCategoryIcon;
        this.expenseType = expenseType;
        this.budgetAmount = budgetAmount;
        this.actualAmount = actualAmount;
        this.variance = variance;
        this.executionRate = executionRate;
        this.currency = currency;
    }

    // Getters and Setters
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

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public BigDecimal getExecutionRate() {
        return executionRate;
    }

    public void setExecutionRate(BigDecimal executionRate) {
        this.executionRate = executionRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
