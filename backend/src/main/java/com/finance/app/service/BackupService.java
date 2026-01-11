package com.finance.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.BackupFileDTO;
import com.finance.app.dto.BackupStatusDTO;
import com.finance.app.dto.RestoreBackupRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BackupService {

    @Value("${backup.webhook.url:http://backup:5000}")
    private String backupWebhookUrl;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BackupService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 手动触发备份
     */
    public Map<String, Object> triggerBackup() {
        try {
            String url = backupWebhookUrl + "/backup/trigger";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("type", "manual");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Backup triggered successfully");
                return response.getBody();
            } else {
                log.error("Failed to trigger backup: {}", response.getStatusCode());
                throw new RuntimeException("Failed to trigger backup");
            }
        } catch (Exception e) {
            log.error("Error triggering backup", e);
            throw new RuntimeException("Error triggering backup: " + e.getMessage());
        }
    }

    /**
     * 列出所有备份文件
     */
    public List<BackupFileDTO> listBackups(String type) {
        try {
            String url = backupWebhookUrl + "/backup/list";
            if (type != null && !type.isEmpty()) {
                url += "?type=" + type;
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> backupsData = (List<Map<String, Object>>) body.get("backups");

                return objectMapper.convertValue(
                    backupsData,
                    new TypeReference<List<BackupFileDTO>>() {}
                );
            } else {
                throw new RuntimeException("Failed to list backups");
            }
        } catch (Exception e) {
            log.error("Error listing backups", e);
            throw new RuntimeException("Error listing backups: " + e.getMessage());
        }
    }

    /**
     * 恢复备份
     */
    public Map<String, Object> restoreBackup(RestoreBackupRequest request) {
        try {
            String url = backupWebhookUrl + "/backup/restore";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RestoreBackupRequest> httpRequest = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.warn("Database restored from backup: {}", request.getFilename());
                return response.getBody();
            } else {
                log.error("Failed to restore backup: {}", response.getStatusCode());
                throw new RuntimeException("Failed to restore backup");
            }
        } catch (Exception e) {
            log.error("Error restoring backup", e);
            throw new RuntimeException("Error restoring backup: " + e.getMessage());
        }
    }

    /**
     * 获取备份日志
     */
    public List<String> getLogs(String type, Integer lines) {
        try {
            String url = backupWebhookUrl + "/backup/logs?type=" + (type != null ? type : "backup")
                    + "&lines=" + (lines != null ? lines : 100);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return (List<String>) body.get("logs");
            } else {
                throw new RuntimeException("Failed to get logs");
            }
        } catch (Exception e) {
            log.error("Error getting logs", e);
            throw new RuntimeException("Error getting logs: " + e.getMessage());
        }
    }

    /**
     * 获取备份服务状态
     */
    public BackupStatusDTO getStatus() {
        try {
            String url = backupWebhookUrl + "/backup/status";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // webhook返回格式: {"success": true, "status": {...}}
                Object statusData = body.get("status");

                if (statusData == null) {
                    // 如果status为null，尝试直接解析body
                    statusData = body;
                }

                BackupStatusDTO status = objectMapper.convertValue(statusData, BackupStatusDTO.class);

                // Extract database name from datasource URL
                // Format: jdbc:mysql://host:port/dbname?params
                String dbName = extractDbNameFromUrl(datasourceUrl);
                status.setDbName(dbName);

                return status;
            } else {
                throw new RuntimeException("Failed to get status");
            }
        } catch (Exception e) {
            log.error("Error getting status", e);
            throw new RuntimeException("Error getting status: " + e.getMessage());
        }
    }

    private String extractDbNameFromUrl(String url) {
        try {
            // Extract database name from JDBC URL
            // Format: jdbc:mysql://host:port/dbname?params
            String[] parts = url.split("/");
            if (parts.length >= 4) {
                String dbPart = parts[3];
                // Remove query parameters if present
                int queryIndex = dbPart.indexOf('?');
                if (queryIndex > 0) {
                    return dbPart.substring(0, queryIndex);
                }
                return dbPart;
            }
        } catch (Exception e) {
            log.warn("Failed to extract database name from URL: {}", url, e);
        }
        return "finance";  // fallback default
    }

    /**
     * 健康检查
     */
    public Map<String, Object> healthCheck() {
        try {
            String url = backupWebhookUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Backup service health check failed", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "unhealthy");
            result.put("error", e.getMessage());
            return result;
        }
    }
}
