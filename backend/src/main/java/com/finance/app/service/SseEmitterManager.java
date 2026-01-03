package com.finance.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE连接管理器
 * 管理Google Sheets同步任务的SSE连接
 */
@Service
@Slf4j
public class SseEmitterManager {

    // syncId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 创建SSE连接
     * @param syncId 同步任务ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long syncId) {
        // 30分钟超时（足够长，避免任务未完成就超时）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitter.onCompletion(() -> {
            log.info("SSE连接完成: syncId={}", syncId);
            removeEmitter(syncId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: syncId={}", syncId);
            removeEmitter(syncId);
        });

        emitter.onError((e) -> {
            log.error("SSE连接错误: syncId={}", syncId, e);
            removeEmitter(syncId);
        });

        emitters.put(syncId, emitter);
        log.info("创建SSE连接: syncId={}, 当前连接数={}", syncId, emitters.size());

        return emitter;
    }

    /**
     * 发送进度更新
     * @param syncId 同步任务ID
     * @param progress 进度（0-100）
     * @param status 状态
     * @param message 消息
     */
    public void sendProgress(Long syncId, int progress, String status, String message) {
        SseEmitter emitter = emitters.get(syncId);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of(
                    "syncId", syncId,
                    "progress", progress,
                    "status", status,
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                );

                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(data));

                log.debug("发送进度更新: syncId={}, progress={}, status={}", syncId, progress, status);
            } catch (IOException e) {
                log.error("发送SSE消息失败: syncId={}", syncId, e);
                removeEmitter(syncId);
            }
        } else {
            log.debug("未找到SSE连接: syncId={}", syncId);
        }
    }

    /**
     * 发送成功消息
     * @param syncId 同步任务ID
     * @param shareUrl 分享链接
     * @param spreadsheetId 电子表格ID
     */
    public void sendSuccess(Long syncId, String shareUrl, String spreadsheetId) {
        SseEmitter emitter = emitters.get(syncId);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of(
                    "syncId", syncId,
                    "progress", 100,
                    "status", "COMPLETED",
                    "shareUrl", shareUrl,
                    "spreadsheetId", spreadsheetId,
                    "message", "同步完成",
                    "timestamp", System.currentTimeMillis()
                );

                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(data));

                log.info("发送完成消息: syncId={}", syncId);

                // 发送完成后关闭连接
                emitter.complete();
            } catch (IOException e) {
                log.error("发送完成消息失败: syncId={}", syncId, e);
            } finally {
                removeEmitter(syncId);
            }
        }
    }

    /**
     * 发送错误消息
     * @param syncId 同步任务ID
     * @param errorMessage 错误信息
     */
    public void sendError(Long syncId, String errorMessage) {
        SseEmitter emitter = emitters.get(syncId);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of(
                    "syncId", syncId,
                    "status", "FAILED",
                    "errorMessage", errorMessage,
                    "message", "同步失败",
                    "timestamp", System.currentTimeMillis()
                );

                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(data));

                log.info("发送错误消息: syncId={}, error={}", syncId, errorMessage);

                // 发送错误后关闭连接
                emitter.complete();
            } catch (IOException e) {
                log.error("发送错误消息失败: syncId={}", syncId, e);
            } finally {
                removeEmitter(syncId);
            }
        }
    }

    /**
     * 移除连接
     */
    private void removeEmitter(Long syncId) {
        emitters.remove(syncId);
        log.info("移除SSE连接: syncId={}, 剩余连接数={}", syncId, emitters.size());
    }

    /**
     * 获取当前连接数
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
