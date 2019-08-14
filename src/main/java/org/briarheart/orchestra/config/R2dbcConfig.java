package org.briarheart.orchestra.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.convert.converter.UtcInstantToLocalDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.support.R2dbcExceptionTranslator;

import java.util.Collections;
import java.util.Set;

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

    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        // By default R2DBC converts database TIMESTAMP to LocalDateTime using system default time zone.
        // Here we override this behaviour by converting TIMESTAMP to LocalDateTime using UTC zone offset.
        Set<Converter<?, ?>> converters = Collections.singleton(new UtcInstantToLocalDateTimeConverter());
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }
}
