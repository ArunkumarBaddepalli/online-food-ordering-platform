package com.app.fooddelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Online Food Delivery System API")
                        .version("1.0")
                        .description(
                                "API Documentation for the Online Food Delivery System. This system allows users to browse restaurants, add items to cart, place orders, and track delivery.")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com")));
    }
}
