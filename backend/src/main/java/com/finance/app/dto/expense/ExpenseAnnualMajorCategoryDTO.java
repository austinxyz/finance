package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 年度支出大类汇总DTO
 */
public class ExpenseAnnualMajorCategoryDTO {

    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryIcon;
    private String majorCategoryCode;
    private BigDecimal totalAmount;
    private String currency;

    // Constructors
    public ExpenseAnnualMajorCategoryDTO() {
    }

    public ExpenseAnnualMajorCategoryDTO(Long majorCategoryId, String majorCategoryName,
                                         String majorCategoryIcon, String majorCategoryCode,
                                         BigDecimal totalAmount, String currency) {
        this.majorCategoryId = majorCategoryId;
        this.majorCategoryName = majorCategoryName;
        this.majorCategoryIcon = majorCategoryIcon;
        this.majorCategoryCode = majorCategoryCode;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    // Getters and Setters
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
