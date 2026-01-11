package com.finance.app.controller;

import com.finance.app.dto.BackupFileDTO;
import com.finance.app.dto.BackupStatusDTO;
import com.finance.app.dto.RestoreBackupRequest;
import com.finance.app.service.BackupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backup")
@Slf4j
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * 手动触发备份（仅管理员）
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerBackup() {
        log.info("Admin triggered manual backup");
        Map<String, Object> result = backupService.triggerBackup();
        return ResponseEntity.ok(result);
    }

    /**
     * 列出所有备份文件（仅管理员）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BackupFileDTO>> listBackups(
            @RequestParam(required = false) String type
    ) {
        List<BackupFileDTO> backups = backupService.listBackups(type);
        return ResponseEntity.ok(backups);
    }

    /**
     * 恢复备份（仅管理员，需要确认）
     */
    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> restoreBackup(
            @Valid @RequestBody RestoreBackupRequest request
    ) {
        log.warn("Admin initiated database restore from: {}", request.getFilename());
        Map<String, Object> result = backupService.restoreBackup(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取备份日志（仅管理员）
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getLogs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer lines
    ) {
        List<String> logs = backupService.getLogs(type, lines);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取备份服务状态（仅管理员）
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupStatusDTO> getStatus() {
        BackupStatusDTO status = backupService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 健康检查（仅管理员）
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = backupService.healthCheck();
        return ResponseEntity.ok(health);
    }
}
