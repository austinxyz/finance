package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 月度支出趋势DTO
 */
public class ExpenseMonthlyTrendDTO {

    private Integer month;  // 1-12
    private String period;  // YYYY-MM格式
    private BigDecimal amount;
    private String currency;

    // Constructors
    public ExpenseMonthlyTrendDTO() {
    }

    public ExpenseMonthlyTrendDTO(Integer month, String period, BigDecimal amount, String currency) {
        this.month = month;
        this.period = period;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
