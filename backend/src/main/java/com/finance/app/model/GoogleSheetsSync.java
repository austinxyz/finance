package com.finance.app.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Google Sheets同步记录
 */
@Entity
@Table(name = "google_sheets_sync")
@Data
public class GoogleSheetsSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 家庭ID
     */
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /**
     * 年份
     */
    @Column(name = "year", nullable = false)
    private Integer year;

    /**
     * Google Sheets电子表格ID
     */
    @Column(name = "spreadsheet_id", nullable = false)
    private String spreadsheetId;

    /**
     * 分享链接
     */
    @Column(name = "share_url", nullable = false, columnDefinition = "TEXT")
    private String shareUrl;

    /**
     * 权限设置：reader或writer
     */
    @Column(name = "permission", nullable = false, length = 20)
    private String permission;

    /**
     * 任务状态：PENDING, IN_PROGRESS, COMPLETED, FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 进度百分比 (0-100)
     */
    @Column(name = "progress")
    private Integer progress = 0;

    /**
     * 错误信息（任务失败时）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 首次创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
