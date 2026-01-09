package com.finance.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 *
 * 采用手动Controller层认证模式（参考growing项目）：
 * 1. 禁用CSRF（使用JWT Token，无需CSRF保护）
 * 2. 无状态会话管理（STATELESS）
 * 3. 所有端点permitAll（在Controller层手动验证Token）
 * 4. 提供BCrypt密码加密器
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护（JWT不需要）
                .csrf(csrf -> csrf.disable())

                // 配置授权规则 - 所有端点允许访问（Controller层手动验证）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()         // 认证端点公开
                        .requestMatchers("/swagger-ui/**").permitAll()   // Swagger文档公开
                        .requestMatchers("/v3/api-docs/**").permitAll()  // OpenAPI文档公开
                        .requestMatchers("/actuator/**").permitAll()     // Actuator端点公开
                        .anyRequest().permitAll()                        // 其他端点也允许（Controller层验证）
                )

                // 配置会话管理 - 无状态（不创建Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * 密码编码器Bean
     * 使用BCrypt算法（10轮）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
