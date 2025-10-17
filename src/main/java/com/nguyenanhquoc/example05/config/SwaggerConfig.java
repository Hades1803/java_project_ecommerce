package com.nguyenanhquoc.example05.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Example05 E-Commerce Application")
                        .description("Backend APIs for Example05 E-Commerce app")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Nguyen Anh Quoc")
                                .url("https://github.com/Hades1803")
                                .email("anhquoc@example.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Example05 E-Commerce App Documentation")
                        .url("http://localhost:8082/swagger-ui/index.html"));
    }
}
