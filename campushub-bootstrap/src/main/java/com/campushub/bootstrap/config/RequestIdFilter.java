package com.campushub.bootstrap.config;

import com.campushub.shared.constant.GlobalConstant;
import com.campushub.shared.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);

        request.setAttribute(GlobalConstant.REQUEST_ID, requestId);
        response.setHeader(GlobalConstant.REQUEST_ID_HEADER, requestId);
        RequestContext.setRequestId(requestId);
        MDC.put(GlobalConstant.REQUEST_ID, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(GlobalConstant.REQUEST_ID);
            RequestContext.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(GlobalConstant.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
