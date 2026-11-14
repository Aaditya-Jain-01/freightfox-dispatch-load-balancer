package com.freightfox.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchLoadBalancerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreightFox Dispatch Load Balancer API")
                        .description("RESTful fleet dispatch optimization engine using Haversine distance, priority scheduling, and capacity-aware routing.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aaditya Jain")
                                .url("https://github.com/Aaditya-Jain-01")));
    }
}