package org.briarheart.orchestra.config;

import org.briarheart.orchestra.controller.format.LocalDateFormatter;
import org.briarheart.orchestra.controller.format.LocalDateTimeFormatter;
import org.briarheart.orchestra.web.filter.LocaleContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public LocaleContextFilter localeContextFilter() {
        return new LocaleContextFilter();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new LocalDateFormatter());
        registry.addFormatter(new LocalDateTimeFormatter());
    }

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new ReactivePageableHandlerMethodArgumentResolver());
    }
}
