package com.finance.app.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class RestoreBackupRequest {
    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Database name confirmation is required")
    private String confirmDbName;
}
