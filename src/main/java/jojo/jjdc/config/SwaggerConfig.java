package jojo.jjdc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;



import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Rest API Document")
                        .description("Spring REST API")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("개발 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Swagger Authorize 클릭 후 access token 값만 입력하면 자동으로 Bearer prefix가 붙습니다."))
                )
                .addSecurityItem(new SecurityRequirement().addList(
                        "BearerAuth"
                ));
    }
}
