package com.kama.jchatmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步执行配置。
 * <p>
 * 统一定义项目中 {@code @Async} 方法使用的线程池，
 * 主要服务于聊天事件处理、Agent 异步执行等后台任务。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 创建项目通用异步线程池。
     *
     * @return Spring 异步执行器
     */
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-event-");
        executor.initialize();
        return executor;
    }
}
