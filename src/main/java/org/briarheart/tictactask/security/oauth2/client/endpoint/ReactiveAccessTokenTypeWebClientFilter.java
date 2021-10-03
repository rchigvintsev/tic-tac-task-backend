package org.briarheart.tictactask.security.oauth2.client.endpoint;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This filter is a workaround that fixes issue when OAuth 2 authentication provider like VK does not return token type
 * in the access token response. Spring expects token type and throws exception when it is not found in the response.
 * This filter inserts some default token type (see {@link #setDefaultTokenType(String)} method) into the response body
 * when it is not present. By default &quot;Bearer&quot; token type is used.
 * <p>
 * This filter expects that content type of the access token response will be &quot;application/json&quot;. If content
 * type is not &quot;application/json&quot; this filter will do nothing.
 *
 * @author Roman Chigvintsev
 */
public class ReactiveAccessTokenTypeWebClientFilter implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(ReactiveAccessTokenTypeWebClientFilter.class);

    private static final DataBufferFactory DEFAULT_DATA_BUFFER_FACTORY = new DefaultDataBufferFactory();
    private static final String DEFAULT_DEFAULT_TOKEN_TYPE = "Bearer";

    private DataBufferFactory dataBufferFactory = DEFAULT_DATA_BUFFER_FACTORY;

    private String defaultTokenType = DEFAULT_DEFAULT_TOKEN_TYPE;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request)
                .flatMap(response -> {
                    if (!isJsonCompatibleResponse(response)) {
                        log.warn("Content type of access token response is not compatible with {}",
                                MediaType.APPLICATION_JSON);
                        return Mono.just(response);
                    }
                    return readParameters(response)
                            .map(this::addTokenTypeIfNecessary)
                            .map(params -> createNewResponse(response, params));
                });
    }

    public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
        Assert.notNull(dataBufferFactory, "Data buffer factory must not be null");
        this.dataBufferFactory = dataBufferFactory;
    }

    public void setDefaultTokenType(String defaultTokenType) {
        Assert.hasText(defaultTokenType, "Default token type must not be null or empty");
        this.defaultTokenType = defaultTokenType;
    }

    private boolean isJsonCompatibleResponse(ClientResponse response) {
        return response.headers().contentType()
                .map(mediaType -> mediaType.isCompatibleWith(MediaType.APPLICATION_JSON))
                .orElse(false);
    }

    private Mono<? extends Map<String, Object>> readParameters(ClientResponse response) {
        ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {
        };
        BodyExtractor<Mono<Map<String, Object>>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(type);
        return response.body(extractor);
    }

    private Map<String, Object> addTokenTypeIfNecessary(Map<String, Object> params) {
        if (params.get("token_type") == null) {
            Map<String, Object> newParams = new HashMap<>(params);
            newParams.put("token_type", defaultTokenType);
            return newParams;
        }
        return params;
    }

    private ClientResponse createNewResponse(ClientResponse originalResponse, Map<String, Object> params) {
        Publisher<Map<String, Object>> input = Mono.just(params);
        ResolvableType bodyType = ResolvableType.forInstance(params);
        HttpMessageEncoder<Object> encoder = new Jackson2JsonEncoder();
        MimeType elementType = MimeTypeUtils.APPLICATION_JSON;
        Map<String, Object> hints = Collections.emptyMap();
        Flux<DataBuffer> newBody = encoder.encode(input, dataBufferFactory, bodyType, elementType, hints);
        return ClientResponse.create(originalResponse.statusCode(), originalResponse.strategies())
                .headers(headers -> headers.addAll(originalResponse.headers().asHttpHeaders()))
                .body(newBody)
                .build();
    }
}
