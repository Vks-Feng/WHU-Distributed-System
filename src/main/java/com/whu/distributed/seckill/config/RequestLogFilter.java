package com.whu.distributed.seckill.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private final String instanceId;

    public RequestLogFilter(@Value("${app.instance-id:local}") String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.put("instanceId", instanceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("instanceId");
        }
    }
}
