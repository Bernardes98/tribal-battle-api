package com.tribalbattle.tribal_battle_api.web;

import tools.jackson.databind.ObjectMapper;
import com.tribalbattle.tribal_battle_api.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${app.http.max-request-bytes:12582912}")
    private long maxRequestBytes;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();

        if (
                contentLength > 0 &&
                contentLength > Math.max(1024, maxRequestBytes)
        ) {
            ApiErrorResponse body = new ApiErrorResponse(
                    Instant.now(),
                    HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    "PAYLOAD_TOO_LARGE",
                    "PAYLOAD_TOO_LARGE",
                    "Request payload is too large.",
                    request.getRequestURI(),
                    RequestIdFilter.getRequestId(request)
            );

            response.setStatus(
                    HttpStatus.PAYLOAD_TOO_LARGE.value()
            );
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            objectMapper.writeValue(
                    response.getOutputStream(),
                    body
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}
