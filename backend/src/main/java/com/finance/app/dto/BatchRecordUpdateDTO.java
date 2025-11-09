package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRecordUpdateDTO {
    private LocalDate recordDate;  // 记录日期，如果为null则使用当前日期
    private List<AccountUpdate> accounts;  // 账户更新列表
    private Boolean overwriteExisting;  // 是否覆盖已存在的记录，默认false

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountUpdate {
        private Long accountId;
        private BigDecimal amount;           // 金额（原币种）
        private BigDecimal quantity;         // 数量
        private BigDecimal unitPrice;        // 单价
        private BigDecimal exchangeRate;     // 汇率
        private String currency;             // 币种
    }
}
