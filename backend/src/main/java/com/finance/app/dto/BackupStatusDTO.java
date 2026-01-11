package com.finance.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import java.util.Map;

@Data
public class BackupStatusDTO {
    private Boolean healthy;

    private String dbName;  // Current database name

    @JsonAlias("disk_usage")
    private DiskUsage diskUsage;

    @JsonAlias("latest_backups")
    private Map<String, LatestBackup> latestBackups;

    @JsonAlias("retention_policy")
    private Map<String, String> retentionPolicy;

    @Data
    public static class DiskUsage {
        private String total;
        private String used;
        private String available;

        @JsonAlias("use_percent")
        private String usePercent;
    }

    @Data
    public static class LatestBackup {
        private String filename;
        private Long size;
        private String timestamp;
    }
}
