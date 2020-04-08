package org.briarheart.orchestra.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.support.R2dbcExceptionTranslator;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@RequiredArgsConstructor
public class R2dbcConfig extends AbstractR2dbcConfiguration {
    private final ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Bean
    @Override
    public DatabaseClient databaseClient(ReactiveDataAccessStrategy dataAccessStrategy,
                                         R2dbcExceptionTranslator exceptionTranslator) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory())
                .dataAccessStrategy(dataAccessStrategy)
                .exceptionTranslator(exceptionTranslator)
                .build();
    }
}
