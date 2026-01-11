package com.finance.app.dto;

import lombok.Data;

@Data
public class BackupFileDTO {
    private String filename;
    private String filepath;
    private String type;  // daily, weekly, monthly
    private Long size;
    private String timestamp;
    private Double mtime;
}
