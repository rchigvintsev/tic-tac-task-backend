package org.briarheart.tictactask.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration that permits all requests.
 * <p>
 * To exclude this configuration from integration tests set system property "security.disabled" to {@code false}.
 *
 * @author Roman Chigvintsev
 */
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "test.security.disabled", havingValue = "true")
public class PermitAllSecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity security) {
        return security
                .authorizeExchange()
                    .anyExchange().permitAll()
                .and()
                    .csrf().disable()
                .build();
    }
}
