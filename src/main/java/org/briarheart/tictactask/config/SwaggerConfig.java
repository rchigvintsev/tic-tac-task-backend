package org.briarheart.tictactask.config;

import io.jsonwebtoken.lang.Assert;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        type = SecuritySchemeType.APIKEY,
        name = "apiSecurityScheme",
        paramName = "access_token",
        in = SecuritySchemeIn.COOKIE
)
public class SwaggerConfig {
    private final ApplicationProperties applicationProperties;

    public SwaggerConfig(ApplicationProperties applicationProperties) {
        Assert.notNull(applicationProperties, "Application properties must not be null");
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public OpenAPI openAPI() {
        Info apiInfo = new Info()
                .title(applicationProperties.getName())
                .version(applicationProperties.getVersion())
                .contact(new Contact().name("Roman Chigvintsev"));
        return new OpenAPI().info(apiInfo);
    }
}
