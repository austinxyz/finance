package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.UserDTO;
import com.finance.app.dto.auth.LoginRequest;
import com.finance.app.dto.auth.LoginResponse;
import com.finance.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含Token和用户信息）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());

        LoginResponse response = authService.login(request.getUsername(), request.getPassword());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取当前用户信息（验证Token）
     *
     * @param authHeader Authorization header
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        UserDTO user = authService.validateToken(token);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 登出（客户端清除Token）
     * 由于使用无状态JWT，服务端不需要处理登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        log.info("用户登出");
        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }

    /**
     * 加密现有用户密码（管理员专用，仅用于数据迁移）
     * WARNING: 这是一次性操作，仅在初始部署时使用
     */
    @PostMapping("/admin/encrypt-passwords")
    public ResponseEntity<ApiResponse<String>> encryptPasswords() {
        log.info("开始加密用户密码");
        authService.encryptExistingPasswords();
        return ResponseEntity.ok(ApiResponse.success("密码加密完成"));
    }

    /**
     * 从Authorization header中提取Token
     *
     * @param authHeader Authorization header
     * @return Token字符串
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
