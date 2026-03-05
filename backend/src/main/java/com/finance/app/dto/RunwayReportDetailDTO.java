package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunwayReportDetailDTO {
    private Long id;
    private String reportName;
    private LocalDateTime savedAt;
    private String snapshotJson;
}
