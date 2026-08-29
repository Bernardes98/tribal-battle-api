package com.tribalbattle.tribal_battle_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.frontend-origins:http://localhost:5173}")
    private String frontendOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(
                        frontendOrigins.split(",")
                )
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);

        registry
                .addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-Request-ID"
                )
                .exposedHeaders(
                        "X-Request-ID",
                        "Retry-After"
                )
                .allowCredentials(false)
                .maxAge(3600);
    }
}
