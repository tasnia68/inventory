package com.inventory.system.config;

import com.inventory.system.config.tenant.TenantAwareTaskDecorator;
import com.inventory.system.config.tenant.TenantAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
public class AsyncExecutionConfiguration {

    @Bean(name = "reportingTaskExecutor")
    public TaskExecutor reportingTaskExecutor() {
        return createTenantAwareExecutor("reporting-", 4, 8, 200);
    }

    @Bean(name = "mvcAsyncTaskExecutor")
    public ThreadPoolTaskExecutor mvcAsyncTaskExecutor() {
        return createTenantAwareExecutor("mvc-async-", 4, 8, 100);
    }

    @Bean(name = "tenantAwareCommonPoolExecutor")
    public Executor tenantAwareCommonPoolExecutor() {
        return TenantAsync.commonPoolExecutor();
    }

    private ThreadPoolTaskExecutor createTenantAwareExecutor(String threadNamePrefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}