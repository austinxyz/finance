package com.finance.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Google Sheets导出专用线程池
     */
    @Bean(name = "googleSheetsExecutor")
    public Executor googleSheetsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 核心线程数
        executor.setMaxPoolSize(5);  // 最大线程数
        executor.setQueueCapacity(10); // 队列容量
        executor.setThreadNamePrefix("google-sheets-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
