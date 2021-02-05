package org.briarheart.orchestra.web.filter;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * This filter sets current locale context obtained from the given {@link ServerWebExchange}. It is required for
 * correct translation of bean validation messages
 * (see {@link org.springframework.validation.beanvalidation.LocaleContextMessageInterpolator} for details).
 *
 * @author Roman Chigvintsev
 * @see LocaleContextHolder
 */
public class LocaleContextFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        LocaleContextHolder.setLocaleContext(exchange.getLocaleContext());
        return chain.filter(exchange);
    }
}
