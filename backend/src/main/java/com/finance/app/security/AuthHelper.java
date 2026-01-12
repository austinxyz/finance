package com.finance.app.security;

import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.User;
import com.finance.app.repository.UserRepository;
import com.finance.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 认证辅助工具类
 * 提供统一的Token提取和验证方法
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * 从Authorization header中提取Token
     *
     * @param authHeader Authorization header
     * @return Token字符串
     * @throws UnauthorizedException 如果header为空或格式错误
     */
    public String extractToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new UnauthorizedException("未提供认证Token");
        }

        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return authHeader;
    }

    /**
     * 验证Token并返回family_id
     *
     * @param authHeader Authorization header
     * @return family_id
     * @throws UnauthorizedException 如果Token无效或用户无权访问
     */
    public Long getFamilyIdFromAuth(String authHeader) {
        String token = extractToken(authHeader);
        return authService.getFamilyIdFromToken(token);
    }

    /**
     * 验证Token并返回user_id
     *
     * @param authHeader Authorization header
     * @return user_id
     * @throws UnauthorizedException 如果Token无效
     */
    public Long getUserIdFromAuth(String authHeader) {
        String token = extractToken(authHeader);
        return authService.getUserIdFromToken(token);
    }

    /**
     * 验证是否为管理员
     *
     * @param authHeader Authorization header
     * @throws UnauthorizedException 如果不是管理员
     */
    public void requireAdmin(String authHeader) {
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }
    }

    /**
     * 检查是否为管理员（不抛出异常）
     *
     * @param authHeader Authorization header
     * @return 是否为管理员
     */
    public boolean isAdmin(String authHeader) {
        try {
            String token = extractToken(authHeader);
            return authService.isAdminByToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证用户是否属于指定family
     *
     * @param authHeader Authorization header
     * @param familyId   要验证的family_id
     * @throws UnauthorizedException 如果用户不属于该family（管理员除外）
     */
    public void requireFamilyAccess(String authHeader, Long familyId) {
        String token = extractToken(authHeader);

        // 管理员可以访问所有数据
        if (authService.isAdminByToken(token)) {
            return;
        }

        // 验证family_id匹配
        Long userFamilyId = authService.getFamilyIdFromToken(token);
        if (!userFamilyId.equals(familyId)) {
            throw new UnauthorizedException("无权访问其他家庭的数据");
        }
    }

    /**
     * 获取授权的family_id
     * - 管理员：可以访问请求的familyId（如果提供），否则使用自己的familyId
     * - 普通用户：只能访问自己的familyId，忽略请求参数
     *
     * @param authHeader Authorization header
     * @param requestedFamilyId 请求的family_id（可为null）
     * @return 授权的family_id
     * @throws UnauthorizedException 如果Token无效或权限不足
     */
    public Long getAuthorizedFamilyId(String authHeader, Long requestedFamilyId) {
        String token = extractToken(authHeader);
        Long userFamilyId = authService.getFamilyIdFromToken(token);
        boolean isAdmin = authService.isAdminByToken(token);

        // 管理员可以访问任何family
        if (isAdmin) {
            // 如果管理员指定了familyId，使用指定的；否则使用自己的
            return requestedFamilyId != null ? requestedFamilyId : userFamilyId;
        }

        // 普通用户只能访问自己的family，忽略请求参数
        return userFamilyId;
    }

    /**
     * 验证账户的userId是否属于authenticated用户的family
     *
     * @param authHeader Authorization header
     * @param accountUserId 账户的user_id
     * @throws UnauthorizedException 如果账户不属于该family（管理员除外）
     */
    public void requireAccountAccess(String authHeader, Long accountUserId) {
        String token = extractToken(authHeader);

        // 管理员可以访问所有数据
        if (authService.isAdminByToken(token)) {
            return;
        }

        // 获取认证用户的family_id
        Long authenticatedFamilyId = authService.getFamilyIdFromToken(token);

        // 查询账户所属用户的family_id
        User accountUser = userRepository.findById(accountUserId)
            .orElseThrow(() -> new UnauthorizedException("账户关联的用户不存在"));

        if (!accountUser.getFamilyId().equals(authenticatedFamilyId)) {
            throw new UnauthorizedException("无权访问其他家庭的账户");
        }
    }
}
