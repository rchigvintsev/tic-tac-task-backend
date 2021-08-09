package org.briarheart.orchestra.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

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
}
