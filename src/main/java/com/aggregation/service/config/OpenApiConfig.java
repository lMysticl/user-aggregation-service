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
                .url("http://127.0.0.1:8080")
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("Project repository")
                .url("https://github.com/lMysticl/user-aggregation-service");

        License license = new License()
                .name("MIT License")
                .url("https://github.com/lMysticl/user-aggregation-service/blob/main/LICENSE");

        Info info = new Info()
                .title("User Aggregation API")
                .version("1.0.0")
                .description("REST API that aggregates PostgreSQL and MongoDB user data")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
