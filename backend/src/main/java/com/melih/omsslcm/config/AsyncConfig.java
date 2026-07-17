package com.melih.omsslcm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Order processing runs on its own bounded pool rather than Spring's default
 * SimpleAsyncTaskExecutor (unbounded, one thread per task) so concurrent order
 * volume can't spawn unbounded threads all contending for the single SQLite
 * connection (see application.yml's Hikari pool-size note).
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "orderProcessingExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("order-proc-");
        executor.initialize();
        return executor;
    }

    /**
     * @Async void/fire-and-forget methods swallow uncaught exceptions by default,
     * which would leave an order silently stuck in VALIDATING/PROVISIONING forever.
     * OrderProcessingService already catches its own exceptions and marks the order
     * FAILED, but this handler is a backstop for anything that escapes that (e.g. a
     * failure in the executor itself) so it's at least visible in the logs.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Uncaught async exception in {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
