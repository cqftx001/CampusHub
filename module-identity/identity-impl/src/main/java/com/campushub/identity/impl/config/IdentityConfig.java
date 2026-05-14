package com.campushub.identity.impl.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
@EnableConfigurationProperties(EmailVerificationProperties.class)
public class IdentityConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 优雅停机：等队列里的邮件发完
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}
