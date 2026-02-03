package com.hackathon.analyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyzerServiceOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8084")
                .description("Local Development Server");

        Server prodServer = new Server()
                .url("https://analyzer-service.run.app")
                .description("Production GCP Cloud Run Server");

        Contact contact = new Contact()
                .name("SaveYourMoney Team")
                .email("support@saveyourmoney.ai")
                .url("https://github.com/saveyourmoney");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("SaveYourMoney Analyzer Service API")
                .version("1.0.0")
                .description("AI-powered resource optimization analyzer for Kubernetes microservices. " +
                        "Collects metrics, detects performance issues, and generates intelligent recommendations " +
                        "to reduce cloud costs while improving application performance.")
                .contact(contact)
                .license(license);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token for API authentication");

        Components components = new Components()
                .addSecuritySchemes("bearerAuth", securityScheme);

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, prodServer))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}
