package com.finance.app.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 数据库迁移工具 - 执行SQL脚本
 * 使用方式：将此类的 @Component 注解取消注释，重启应用即可自动执行
 */
// @Component
public class DatabaseMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {

        // 读取SQL文件
        String sqlFilePath = "/Users/yanzxu/claude/finance/database/add_retirement_fund_net_category.sql";
        String sqlContent = Files.readString(Paths.get(sqlFilePath), StandardCharsets.UTF_8);

        // 按分号分割SQL语句（简单实现，不处理存储过程等复杂情况）
        String[] statements = sqlContent.split(";");

        int successCount = 0;
        int errorCount = 0;

        for (String statement : statements) {
            String trimmed = statement.trim();
            // 跳过空语句和注释
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }

            // 跳过 SELECT 语句（验证查询）
            if (trimmed.toUpperCase().startsWith("SELECT")) {
                continue;
            }

            try {
                jdbcTemplate.execute(trimmed);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                System.err.println("✗ 失败: " + e.getMessage());
            }
        }

    }
}
