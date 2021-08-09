package org.briarheart.orchestra.config;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.controller.format.LocalDateFormatter;
import org.briarheart.orchestra.controller.format.LocalDateTimeFormatter;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.validation.NoFallbackResourceBundleLocator;
import org.briarheart.orchestra.service.FileTooLargeException;
import org.briarheart.orchestra.web.error.ApiErrorAttributes;
import org.briarheart.orchestra.web.error.HttpStatusExceptionTypeMapper;
import org.briarheart.orchestra.web.filter.LocaleContextFilter;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import javax.validation.MessageInterpolator;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@EnableWebFlux
@RequiredArgsConstructor
public class WebConfig implements WebFluxConfigurer {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public LocaleContextFilter localeContextFilter() {
        return new LocaleContextFilter();
    }

    @Bean
    public ErrorAttributes errorAttributes(HttpStatusExceptionTypeMapper httpStatusExceptionTypeMapper) {
        return new ApiErrorAttributes(httpStatusExceptionTypeMapper);
    }

    @Bean
    public HttpStatusExceptionTypeMapper httpStatusExceptionTypeMapper() {
        return exceptionType -> {
            Assert.notNull(exceptionType, "Exception type must not be null");
            if (exceptionType == EntityNotFoundException.class) {
                return HttpStatus.NOT_FOUND;
            }
            if (exceptionType == EntityAlreadyExistsException.class || exceptionType == FileTooLargeException.class) {
                return HttpStatus.BAD_REQUEST;
            }
            return null;
        };
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

    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean validatorBean = new LocalValidatorFactoryBean();
        ResourceBundleLocator resourceBundleLocator = new NoFallbackResourceBundleLocator();
        MessageInterpolator messageInterpolator = new ResourceBundleMessageInterpolator(resourceBundleLocator);
        validatorBean.setMessageInterpolator(messageInterpolator);
        return validatorBean;
    }
}
