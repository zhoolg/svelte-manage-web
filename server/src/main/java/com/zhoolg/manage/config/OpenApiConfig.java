package com.zhoolg.manage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI manageWebOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Svelte Manage Web Server API")
                        .version("0.2.0")
                        .description("REST API, metadata and AI generation endpoints for svelte-manage-web"));
    }
}
