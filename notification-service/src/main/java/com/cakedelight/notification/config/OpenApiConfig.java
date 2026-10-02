package com.cakedelight.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI description served at /v3/api-docs.
 *
 * The server URL is relative, so "Try it out" in Swagger UI sends requests to
 * the host that served the UI (the API Gateway), not to this container's
 * internal address.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("1.1.0")
                        .description("Read the in-app order confirmations created from order-completion events."))
                .servers(List.of(new Server().url("/").description("Host serving this documentation")));
    }
}
