package com.junction.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI / Swagger 配置。
 *
 * 访问地址：
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 *
 * Bearer Token 鉴权按钮已配置，前端可在 Swagger 页面填 token 调试。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "Bearer Token";
        return new OpenAPI()
                .info(new Info()
                        .title("Junction API")
                        .description("Spring Boot 项目模板接口文档")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
