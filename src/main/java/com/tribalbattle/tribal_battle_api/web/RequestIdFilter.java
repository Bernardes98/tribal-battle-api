package com.tribalbattle.tribal_battle_api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE =
            RequestIdFilter.class.getName() + ".requestId";

    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = normalizeRequestId(
                request.getHeader(REQUEST_ID_HEADER)
        );

        request.setAttribute(
                REQUEST_ID_ATTRIBUTE,
                requestId
        );

        response.setHeader(
                REQUEST_ID_HEADER,
                requestId
        );

        MDC.put(
                "requestId",
                requestId
        );

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            MDC.remove(
                    "requestId"
            );
        }
    }

    public static String getRequestId(
            HttpServletRequest request
    ) {
        Object value = request.getAttribute(
                REQUEST_ID_ATTRIBUTE
        );

        return value instanceof String requestId
                ? requestId
                : null;
    }

    private String normalizeRequestId(
            String value
    ) {
        if (
                value == null ||
                value.isBlank() ||
                value.length() > 128 ||
                !value.matches("[A-Za-z0-9._:-]+")
        ) {
            return UUID.randomUUID().toString();
        }

        return value;
    }
}
