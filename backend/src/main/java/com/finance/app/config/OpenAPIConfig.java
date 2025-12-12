package com.finance.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger配置
 * 提供交互式API文档
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("家庭理财管理系统 API")
                        .version("1.0.0")
                        .description("基于 Spring Boot 的家庭资产负债管理系统，支持多成员协同管理、资产追踪、支出分析和税收规划。")
                        .contact(new Contact()
                                .name("Austin Xu")
                                .url("https://github.com/austinxyz/finance")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发环境")
                ));
    }
}
