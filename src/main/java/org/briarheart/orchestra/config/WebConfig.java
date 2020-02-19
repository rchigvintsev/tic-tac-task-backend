package org.briarheart.orchestra.config;

import org.briarheart.orchestra.web.filter.LocaleContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@EnableWebFlux
public class WebConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public LocaleContextFilter localeContextFilter() {
        return new LocaleContextFilter();
    }
}
