package org.briarheart.orchestra.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Roman Chigvintsev
 */
class LocaleContextFilterTest {
    private LocaleContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LocaleContextFilter();
    }

    @Test
    void shouldSetLocaleContext() {
        Locale targetLocale = Locale.FRENCH;
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .acceptLanguageAsLocales(targetLocale)
                .build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterChain webFilterChainMock = mock(WebFilterChain.class);
        filter.filter(webExchangeMock, webFilterChainMock);

        LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
        assertNotNull(localeContext);
        assertEquals(targetLocale, localeContext.getLocale());
    }
}
