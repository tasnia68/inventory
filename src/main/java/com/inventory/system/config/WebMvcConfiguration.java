package com.inventory.system.config;

import com.inventory.system.config.interceptor.RequestResponseLoggingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final ThreadPoolTaskExecutor mvcAsyncTaskExecutor;

    public WebMvcConfiguration(@Qualifier("mvcAsyncTaskExecutor") ThreadPoolTaskExecutor mvcAsyncTaskExecutor) {
        this.mvcAsyncTaskExecutor = mvcAsyncTaskExecutor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestResponseLoggingInterceptor())
                .addPathPatterns("/api/**");
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcAsyncTaskExecutor);
    }
}
