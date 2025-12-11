package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 年度支出小类汇总DTO
 */
public class ExpenseAnnualMinorCategoryDTO {

    private Long minorCategoryId;
    private String minorCategoryName;
    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryIcon;
    private String expenseType;
    private BigDecimal totalAmount;
    private String currency;

    // Constructors
    public ExpenseAnnualMinorCategoryDTO() {
    }

    public ExpenseAnnualMinorCategoryDTO(Long minorCategoryId, String minorCategoryName,
                                         Long majorCategoryId, String majorCategoryName,
                                         String majorCategoryIcon, String expenseType,
                                         BigDecimal totalAmount, String currency) {
        this.minorCategoryId = minorCategoryId;
        this.minorCategoryName = minorCategoryName;
        this.majorCategoryId = majorCategoryId;
        this.majorCategoryName = majorCategoryName;
        this.majorCategoryIcon = majorCategoryIcon;
        this.expenseType = expenseType;
        this.totalAmount = totalAmount;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
