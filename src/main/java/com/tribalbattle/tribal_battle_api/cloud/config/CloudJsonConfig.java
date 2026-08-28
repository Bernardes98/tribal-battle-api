package com.tribalbattle.tribal_battle_api.cloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudJsonConfig {

    @Bean
    public ObjectMapper cloudObjectMapper() {
        return new ObjectMapper();
    }
}
