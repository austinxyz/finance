package com.finance.app.service;

import com.finance.app.dto.UserDTO;
import com.finance.app.dto.auth.LoginResponse;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.User;
import com.finance.app.repository.UserRepository;
import com.finance.app.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录响应（包含Token和用户信息）
     */
    @Transactional
    public LoginResponse login(String username, String password) {
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        // 检查用户是否激活
        if (!user.getIsActive()) {
            throw new UnauthorizedException("用户已被停用");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT Token
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name(),
                user.getId(),
                user.getFamilyId()
        );

        // 构建用户DTO
        UserDTO userDTO = convertToDTO(user);

        // 返回登录响应
        return new LoginResponse(token, userDTO);
    }

    /**
     * 验证Token并获取当前用户
     *
     * @param token JWT Token
     * @return 用户DTO
     */
    public UserDTO validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Token无效或已过期");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("用户已被停用");
        }

        return convertToDTO(user);
    }

    /**
     * 检查用户是否为管理员
     *
     * @param username 用户名
     * @return 是否为管理员
     */
    public boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRole() == User.Role.ADMIN)
                .orElse(false);
    }

    /**
     * 通过Token检查是否为管理员
     *
     * @param token JWT Token
     * @return 是否为管理员
     */
    public boolean isAdminByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String role = jwtUtil.getRoleFromToken(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            log.error("检查管理员权限失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new UnauthorizedException("未提供认证Token");
        }

        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Token无效: " + e.getMessage());
        }
    }

    /**
     * 从Token中获取家庭ID
     *
     * @param token JWT Token
     * @return 家庭ID
     */
    public Long getFamilyIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new UnauthorizedException("未提供认证Token");
        }

        try {
            return jwtUtil.getFamilyIdFromToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Token无效: " + e.getMessage());
        }
    }

    /**
     * 加密所有现有用户的密码（仅用于数据迁移）
     * WARNING: 这是一次性迁移操作，仅在初始部署时使用
     */
    @Transactional
    public void encryptExistingPasswords() {
        List<User> users = userRepository.findAll();
        log.info("开始加密 {} 个用户的密码", users.size());

        for (User user : users) {
            String currentPassword = user.getPasswordHash();

            // 如果密码已经是BCrypt格式（以$2a$开头），则跳过
            if (currentPassword != null && currentPassword.startsWith("$2a$")) {
                log.info("用户 {} 的密码已加密，跳过", user.getUsername());
                continue;
            }

            // 加密密码
            String encryptedPassword = passwordEncoder.encode(currentPassword);
            user.setPasswordHash(encryptedPassword);
            userRepository.save(user);

            log.info("已加密用户 {} 的密码", user.getUsername());
        }

        log.info("密码加密完成");
    }

    /**
     * 将User实体转换为UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFamilyId(user.getFamilyId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setBirthDate(user.getBirthDate());
        dto.setAnnualIncome(user.getAnnualIncome());
        dto.setIncomeCurrency(user.getIncomeCurrency());
        dto.setRiskTolerance(user.getRiskTolerance() != null ? user.getRiskTolerance().name() : null);
        dto.setNotes(user.getNotes());
        dto.setIsActive(user.getIsActive());
        dto.setRole(user.getRole().name());
        return dto;
    }
}
