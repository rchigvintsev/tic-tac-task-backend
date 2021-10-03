package org.briarheart.tictactask.config;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.format.LocalDateFormatter;
import org.briarheart.tictactask.controller.format.LocalDateTimeFormatter;
import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.model.validation.NoFallbackResourceBundleLocator;
import org.briarheart.tictactask.service.FileTooLargeException;
import org.briarheart.tictactask.web.error.ApiErrorAttributes;
import org.briarheart.tictactask.web.error.HttpStatusExceptionTypeMapper;
import org.briarheart.tictactask.web.filter.LocaleContextFilter;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import javax.validation.MessageInterpolator;
import java.time.Duration;
import java.util.Arrays;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@EnableWebFlux
@EnableConfigurationProperties({WebProperties.class, WebFluxProperties.class})
@RequiredArgsConstructor
public class WebConfig implements WebFluxConfigurer {
    private final WebProperties webProperties;
    private final WebFluxProperties webFluxProperties;

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

    @Bean
    public RouterFunctionMapping welcomePageRouterFunctionMapping(ApplicationContext applicationContext) {
        String[] staticLocations = webProperties.getResources().getStaticLocations();
        Resource welcomePage = getWelcomePage(applicationContext, staticLocations);
        if (welcomePage != null && "/**".equals(webFluxProperties.getStaticPathPattern())) {
            RequestPredicate requestPredicate = GET("/**")
                    .and(pathExtension(ext -> !StringUtils.hasLength(ext) || ext.equalsIgnoreCase("html")))
                    .and(accept(MediaType.TEXT_HTML));
            RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(requestPredicate,
                    request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(welcomePage));
            RouterFunctionMapping routerFunctionMapping = new RouterFunctionMapping(routerFunction);
            routerFunctionMapping.setOrder(1);
            return routerFunctionMapping;
        }
        return null;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new LocalDateFormatter());
        registry.addFormatter(new LocalDateTimeFormatter());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        WebProperties.Resources resources = webProperties.getResources();
        if (!resources.isAddMappings()) {
            return;
        }

        if (!registry.hasMappingForPattern("/webjars/**")) {
            ResourceHandlerRegistration registration = registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
            configureResourceCaching(registration);
        }

        String staticPathPattern = webFluxProperties.getStaticPathPattern();
        if (!registry.hasMappingForPattern(staticPathPattern)) {
            ResourceHandlerRegistration registration = registry.addResourceHandler(staticPathPattern)
                    .addResourceLocations(resources.getStaticLocations());
            configureResourceCaching(registration);
        }
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

    private Resource getWelcomePage(ApplicationContext applicationContext, String[] staticLocations) {
        return Arrays.stream(staticLocations)
                .map(location -> applicationContext.getResource(location + "index.html"))
                .filter(Resource::exists)
                .findFirst()
                .orElse(null);
    }

    private void configureResourceCaching(ResourceHandlerRegistration registration) {
        WebProperties.Resources.Cache cache = webProperties.getResources().getCache();
        Duration cachePeriod = cache.getPeriod();
        WebProperties.Resources.Cache.Cachecontrol cacheControl = cache.getCachecontrol();
        if (cachePeriod != null && cacheControl.getMaxAge() == null) {
            cacheControl.setMaxAge(cachePeriod);
        }
        registration.setCacheControl(cacheControl.toHttpCacheControl());
        registration.setUseLastModified(cache.isUseLastModified());
    }
}
