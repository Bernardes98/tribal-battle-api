package com.tribalbattle.tribal_battle_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tribalBattleOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Tribal Battle API")
                                .description(
                                        "REST API for Tribal Battle Simulator."
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Tribal Battle Simulator")
                                )
                );
    }
}