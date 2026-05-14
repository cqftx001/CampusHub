package com.campushub.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI campusHubOpenApi() {
        return new OpenAPI().info(new Info().title("CampusHub API").version("v1"));
    }
}
