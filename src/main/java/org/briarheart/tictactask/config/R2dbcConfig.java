package org.briarheart.tictactask.config;

import org.briarheart.tictactask.data.convert.converter.CustomConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class R2dbcConfig {
    private final R2dbcDialect dialect;

    public R2dbcConfig(DatabaseClient databaseClient) {
        this.dialect = DialectResolver.getDialect(databaseClient.getConnectionFactory());
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(List<CustomConverter> customConverters) {
        List<Object> converters = new ArrayList<>(dialect.getConverters());
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
        converters.addAll(customConverters);
        StoreConversions conversions = StoreConversions.of(dialect.getSimpleTypeHolder(), converters);
        return new R2dbcCustomConversions(conversions, List.of());
    }
}
