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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private final Map<String, WindowCounter> counters =
            new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.http.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    @Value("${app.rate-limit.login-per-minute:10}")
    private int loginPerMinute;

    @Value("${app.rate-limit.register-per-minute:5}")
    private int registerPerMinute;

    @Value("${app.rate-limit.password-reset-per-15-minutes:5}")
    private int passwordResetPer15Minutes;

    @Value("${app.rate-limit.shared-create-per-minute:60}")
    private int sharedCreatePerMinute;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = resolveRule(request);

        if (!enabled || rule == null) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        long now = System.currentTimeMillis();
        String key = rule.name()
                + ":"
                + clientAddress(request);

        WindowCounter counter = counters.compute(
                key,
                (ignored, current) -> nextCounter(
                        current,
                        now,
                        rule.windowMillis()
                )
        );

        if (counter.count() > rule.limit()) {
            long retryAfterSeconds = Math.max(
                    1,
                    (counter.windowStartedAt()
                            + rule.windowMillis()
                            - now
                            + 999) / 1000
            );

            response.setHeader(
                    "Retry-After",
                    String.valueOf(retryAfterSeconds)
            );

            ApiErrorResponse body = new ApiErrorResponse(
                    Instant.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "TOO_MANY_REQUESTS",
                    "RATE_LIMITED",
                    "Too many requests. Please try again later.",
                    request.getRequestURI(),
                    RequestIdFilter.getRequestId(request)
            );

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
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

        cleanupOccasionally(now);
    }

    private Rule resolveRule(
            HttpServletRequest request
    ) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();

        if ("/api/v1/auth/login".equals(path)) {
            return new Rule(
                    "login",
                    Math.max(1, loginPerMinute),
                    60_000
            );
        }

        if ("/api/v1/auth/register".equals(path)) {
            return new Rule(
                    "register",
                    Math.max(1, registerPerMinute),
                    60_000
            );
        }

        if (
                "/api/v1/auth/password/forgot".equals(path) ||
                "/api/v1/auth/password/reset".equals(path)
        ) {
            return new Rule(
                    "password-reset",
                    Math.max(1, passwordResetPer15Minutes),
                    15 * 60_000L
            );
        }

        if ("/api/v1/shared-simulations".equals(path)) {
            return new Rule(
                    "shared-create",
                    Math.max(1, sharedCreatePerMinute),
                    60_000
            );
        }

        return null;
    }

    private WindowCounter nextCounter(
            WindowCounter current,
            long now,
            long windowMillis
    ) {
        if (
                current == null ||
                now - current.windowStartedAt() >= windowMillis
        ) {
            return new WindowCounter(
                    now,
                    1
            );
        }

        return new WindowCounter(
                current.windowStartedAt(),
                current.count() + 1
        );
    }

    private String clientAddress(
            HttpServletRequest request
    ) {
        String forwardedFor = request.getHeader(
                "X-Forwarded-For"
        );

        if (
                trustForwardedFor &&
                forwardedFor != null &&
                !forwardedFor.isBlank()
        ) {
            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }

    private void cleanupOccasionally(
            long now
    ) {
        if ((now / 1000) % 60 != 0) {
            return;
        }

        counters.entrySet().removeIf(
                entry ->
                        now - entry.getValue().windowStartedAt()
                                > 30 * 60_000L
        );
    }

    private record Rule(
            String name,
            int limit,
            long windowMillis
    ) {
    }

    private record WindowCounter(
            long windowStartedAt,
            int count
    ) {
    }
}
