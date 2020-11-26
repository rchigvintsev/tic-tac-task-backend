package org.briarheart.orchestra.config;

import io.r2dbc.h2.CustomH2ConnectionFactory;
import io.r2dbc.h2.codecs.DefaultCodecs;
import io.r2dbc.h2.codecs.DelegatingExternalCodecs;
import io.r2dbc.h2.codecs.EnumCodec;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Roman Chigvintsev
 */
@Configuration
public class TestR2dbcConnectionFactoryConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        return CustomH2ConnectionFactory.inMemory("testdb")
                .withCodecsProvider(client -> {
                    DefaultCodecs delegate = new DefaultCodecs(client);
                    return new DelegatingExternalCodecs(delegate, List.of(new EnumCodec()));
                })
                .build();
    }
}
