package org.briarheart.orchestra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration that permits all requests.
 *
 * @author Roman Chigvintsev
 */
@EnableWebFluxSecurity
public class PermitAllSecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity security) {
        return  security.authorizeExchange().anyExchange().permitAll().and().build();
    }
}
