package com.finance.app.model;

public enum TaxStatus {
    TAXABLE("Taxable"),           // 产生应税收益
    TAX_FREE("Tax-Free"),         // 收益完全免税
    TAX_DEFERRED("Tax-Deferred"); // 收益延迟纳税

    private final String displayName;

    TaxStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
