package com.hackathon.codegen.config;

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
    public OpenAPI codeGeneratorServiceOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8085")
                .description("Local Development Server");

        Server prodServer = new Server()
                .url("https://code-generator-service.run.app")
                .description("Production GCP Cloud Run Server");

        Contact contact = new Contact()
                .name("SaveYourMoney Team")
                .email("support@saveyourmoney.ai")
                .url("https://github.com/saveyourmoney");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("SaveYourMoney Code Generator Service API")
                .version("1.0.0")
                .description("Automated configuration file generator for optimized Kubernetes deployments. " +
                        "Generates YAML manifests, Spring Boot properties, Helm values, and creates " +
                        "Azure DevOps Pull Requests automatically.")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, prodServer));
    }
}
