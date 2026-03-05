package com.finance.app.dto;

import lombok.Data;

@Data
public class SaveRunwayReportRequest {
    private Long familyId;
    private String snapshotJson;
}
