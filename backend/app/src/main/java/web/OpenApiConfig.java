package web;

import java.util.*;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot API Documentation")
                        .version("1.0.0")
                        .description("API Documentation for trade analysis and WTO data")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")))
                .servers(List.of(
                        new Server()
                                .url("https://teamcmiggwp.duckdns.org")
                                .description("Production Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                // Declare a global security requirement; method-level @SecurityRequirement still supported
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
