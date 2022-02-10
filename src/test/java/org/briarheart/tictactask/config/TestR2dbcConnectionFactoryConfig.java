package org.briarheart.tictactask.config;

import io.r2dbc.spi.ConnectionFactory;
import org.briarheart.tictactask.data.database.postgresql.EmbeddedPostgresqlConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Roman Chigvintsev
 */
@Configuration
public class TestR2dbcConnectionFactoryConfig {
    @Bean
    public ConnectionFactory connectionFactory(DataSource dataSource) {
        return new EmbeddedPostgresqlConnectionFactory(dataSource);
    }
}
