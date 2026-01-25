package com.finance.app.controller;

import com.finance.app.service.GoogleOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Google OAuth 2.0 授权控制器
 * 提供Web端OAuth授权流程API
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Google OAuth", description = "Google OAuth 2.0授权API")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    /**
     * 检查OAuth授权状态
     */
    @GetMapping("/google-oauth/status")
    @Operation(summary = "检查OAuth授权状态", description = "检查是否已有有效的Google OAuth授权令牌")
    public ResponseEntity<Map<String, Object>> checkAuthStatus() {
        boolean hasToken = googleOAuthService.hasValidToken();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "authorized", hasToken,
            "message", hasToken ? "已授权" : "未授权，请先授权"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取OAuth授权URL
     */
    @GetMapping("/google-oauth/auth-url")
    @Operation(summary = "获取OAuth授权URL", description = "获取Google OAuth授权页面URL，用户需要访问此URL完成授权")
    public ResponseEntity<Map<String, Object>> getAuthUrl() {
        try {
            String authUrl = googleOAuthService.getAuthorizationUrl();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "authUrl", authUrl,
                "message", "请访问此URL完成授权"
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成授权URL失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "生成授权URL失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * OAuth回调处理
     */
    @GetMapping("/google-sheets/oauth2callback")
    @Operation(summary = "OAuth授权回调", description = "处理Google OAuth授权回调，保存访问令牌")
    public ResponseEntity<String> handleCallback(
            @Parameter(description = "授权码", required = true)
            @RequestParam String code,
            @Parameter(description = "状态参数")
            @RequestParam(required = false) String state) {

        try {
            log.info("收到OAuth回调，授权码长度: {}", code.length());

            // 使用授权码换取访问令牌
            googleOAuthService.exchangeCodeForToken(code);

            // 返回成功页面（跳转回前端）
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>授权成功</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        .container {
                            background: white;
                            padding: 2rem;
                            border-radius: 8px;
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                            text-align: center;
                            max-width: 400px;
                        }
                        .success-icon {
                            font-size: 4rem;
                            color: #10b981;
                            margin-bottom: 1rem;
                        }
                        h1 {
                            color: #1f2937;
                            margin-bottom: 1rem;
                        }
                        p {
                            color: #6b7280;
                            margin-bottom: 1.5rem;
                        }
                        .btn {
                            background: #667eea;
                            color: white;
                            padding: 0.75rem 1.5rem;
                            border-radius: 6px;
                            text-decoration: none;
                            display: inline-block;
                            transition: background 0.2s;
                        }
                        .btn:hover {
                            background: #5568d3;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success-icon">✓</div>
                        <h1>授权成功！</h1>
                        <p>您已成功授权应用访问 Google Sheets。现在可以关闭此窗口并返回应用。</p>
                        <a href="#" class="btn" onclick="window.close(); return false;">关闭窗口</a>
                        <script>
                            // 5秒后自动关闭窗口
                            setTimeout(() => {
                                window.close();
                                // 如果无法关闭（不是弹窗），则跳转回前端
                                window.location.href = 'http://localhost:3000';
                            }, 5000);
                        </script>
                    </div>
                </body>
                </html>
                """;

            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);

        } catch (Exception e) {
            log.error("处理OAuth回调失败", e);

            String errorHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>授权失败</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                        }
                        .container {
                            background: white;
                            padding: 2rem;
                            border-radius: 8px;
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                            text-align: center;
                            max-width: 400px;
                        }
                        .error-icon {
                            font-size: 4rem;
                            color: #ef4444;
                            margin-bottom: 1rem;
                        }
                        h1 {
                            color: #1f2937;
                            margin-bottom: 1rem;
                        }
                        p {
                            color: #6b7280;
                            margin-bottom: 1.5rem;
                        }
                        .error-detail {
                            background: #fef2f2;
                            border: 1px solid #fecaca;
                            border-radius: 4px;
                            padding: 0.75rem;
                            margin-bottom: 1.5rem;
                            color: #991b1b;
                            font-size: 0.875rem;
                        }
                        .btn {
                            background: #ef4444;
                            color: white;
                            padding: 0.75rem 1.5rem;
                            border-radius: 6px;
                            text-decoration: none;
                            display: inline-block;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error-icon">✗</div>
                        <h1>授权失败</h1>
                        <p>处理Google授权时出现错误。</p>
                        <div class="error-detail">%s</div>
                        <a href="#" class="btn" onclick="window.close(); return false;">关闭窗口</a>
                    </div>
                </body>
                </html>
                """.formatted(e.getMessage());

            return ResponseEntity.status(500)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(errorHtml);
        }
    }

    /**
     * 清除授权令牌（重新授权）
     */
    @PostMapping("/google-oauth/revoke")
    @Operation(summary = "清除授权令牌", description = "清除已保存的OAuth令牌，用于重新授权")
    public ResponseEntity<Map<String, Object>> revokeAuthorization() {
        try {
            googleOAuthService.clearTokens();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("message", "授权已清除，请重新授权"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清除授权失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "清除授权失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
