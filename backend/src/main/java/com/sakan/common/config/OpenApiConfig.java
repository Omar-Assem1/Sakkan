package com.sakan.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sakanOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sakan API")
                        .description("""
                                REST API for the Sakan property rental platform.
                                
                                **Error responses** follow [RFC 9457 ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457).
                                Every error body contains `status`, `title`, `detail`, `type`, and `timestamp`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sakan Team")
                                .url("https://github.com/KimoStore/Sakkan")));
    }
}
