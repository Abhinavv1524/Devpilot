package com.devpilot.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executes agent workflows on a bounded background pool so that
 * POST /api/tasks returns immediately with a RECEIVED task instead of
 * blocking the HTTP request thread for the whole research/coding run.
 */
@Configuration
public class AsyncConfig {

    public static final String TASK_EXECUTOR = "devpilotTaskExecutor";

    @Bean(name = TASK_EXECUTOR)
    public ThreadPoolTaskExecutor devpilotTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("devpilot-task-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
