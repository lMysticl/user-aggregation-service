package com.aggregation.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI userAggregationApi() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("Project repository")
                .url("https://github.com/lMysticl/Aggregation_Service");

        License license = new License()
                .name("All rights reserved")
                .url("https://github.com/lMysticl/Aggregation_Service/blob/main/LICENSE");

        Info info = new Info()
                .title("User Aggregation API")
                .version("1.0.0")
                .description("Service for aggregating user data from multiple databases (PostgreSQL and MongoDB)")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
