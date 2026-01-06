package com.inventory.system.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        logger.info("Incoming request: method={}, uri={}, params={}", request.getMethod(), request.getRequestURI(), request.getQueryString());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Can be used to log model view data if needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTimeObj = request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = 0;
        if (startTimeObj instanceof Long) {
            duration = System.currentTimeMillis() - (Long) startTimeObj;
        }
        logger.info("Request completed: method={}, uri={}, status={}, duration={}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        if (ex != null) {
            logger.error("Request failed with exception", ex);
        }
    }
}
