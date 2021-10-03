package org.briarheart.tictactask.security.oauth2.client.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class ReactiveAccessTokenTypeWebClientFilterTest {
    private static final String DEFAULT_DEFAULT_TOKEN_TYPE = "Bearer";

    private ReactiveAccessTokenTypeWebClientFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ReactiveAccessTokenTypeWebClientFilter();
        filter.setDefaultTokenType(DEFAULT_DEFAULT_TOKEN_TYPE);
    }

    @Test
    void shouldInsertAccessTokenTypeIntoResponseBody() {
        ClientRequest clientRequestMock = mockClientRequest();
        ClientResponse clientResponseMock = mockClientResponse(MediaType.APPLICATION_JSON, Collections.emptyMap());
        ExchangeFunction exchangeFunctionMock = mockExchangeFunction(clientResponseMock);

        ClientResponse response = filter.filter(clientRequestMock, exchangeFunctionMock).block();
        assertNotNull(response);

        Map<String, Object> params = readResponseParameters(response);
        assertNotNull(params);
        assertEquals(DEFAULT_DEFAULT_TOKEN_TYPE, params.get("token_type"));
    }

    @Test
    void shouldIgnoreResponseWhenContentTypeIsNotCompatibleWithApplicationJson() {
        ClientRequest clientRequestMock = mockClientRequest();
        ClientResponse clientResponseMock = mockClientResponse(MediaType.APPLICATION_XML, Collections.emptyMap());
        ExchangeFunction exchangeFunctionMock = mockExchangeFunction(clientResponseMock);

        ClientResponse response = filter.filter(clientRequestMock, exchangeFunctionMock).block();
        assertNotNull(response);

        Map<String, Object> params = readResponseParameters(response);
        assertNotNull(params);
        assertNull(params.get("token_type"));
    }

    @Test
    void shouldPreserveOriginalTokenType() {
        ClientRequest clientRequestMock = mockClientRequest();
        ClientResponse clientResponseMock = mockClientResponse(MediaType.APPLICATION_JSON, Map.of("token_type", "Mac"));
        ExchangeFunction exchangeFunctionMock = mockExchangeFunction(clientResponseMock);

        ClientResponse response = filter.filter(clientRequestMock, exchangeFunctionMock).block();
        assertNotNull(response);

        Map<String, Object> params = readResponseParameters(response);
        assertNotNull(params);
        assertEquals("Mac", params.get("token_type"));
    }

    private ClientRequest mockClientRequest() {
        return mock(ClientRequest.class);
    }

    private ClientResponse mockClientResponse(MediaType contentType, Map<String, Object> parameters) {
        List<HttpMessageReader<?>> messageReaders = new ArrayList<>();
        messageReaders.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));

        ExchangeStrategies exchangeStrategiesMock = mock(ExchangeStrategies.class);
        when(exchangeStrategiesMock.messageReaders()).thenReturn(messageReaders);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, contentType.toString());

        ClientResponse.Headers headersMock = mock(ClientResponse.Headers.class);
        when(headersMock.contentType()).thenReturn(Optional.of(contentType));
        when(headersMock.asHttpHeaders()).thenReturn(httpHeaders);

        ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.strategies()).thenReturn(exchangeStrategiesMock);
        when(clientResponseMock.headers()).thenReturn(headersMock);
        when(clientResponseMock.body(any())).thenReturn(Mono.just(parameters));

        return clientResponseMock;
    }

    private ExchangeFunction mockExchangeFunction(ClientResponse clientResponse) {
        ExchangeFunction exchangeFunctionMock = mock(ExchangeFunction.class);
        when(exchangeFunctionMock.exchange(any())).thenReturn(Mono.just(clientResponse));
        return exchangeFunctionMock;
    }

    private Map<String, Object> readResponseParameters(ClientResponse response) {
        ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {
        };
        BodyExtractor<Mono<Map<String, Object>>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(type);
        return response.body(extractor).block();
    }
}
