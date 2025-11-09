package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRecordCheckDTO {
    private LocalDate recordDate;
    private List<Long> accountIds;  // 要检查的账户ID列表
}
