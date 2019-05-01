package org.briarheart.orchestra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;

@Configuration
public class RepositoryRestConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        // TODO: save client origin somewhere in the application settings
        config.getCorsRegistry().addMapping("/**").allowedOrigins("http://localhost:4200");
    }
}
