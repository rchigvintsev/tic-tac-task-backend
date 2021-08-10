package org.briarheart.orchestra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@RequiredArgsConstructor
public class R2dbcConfig /*extends AbstractR2dbcConfiguration*/ {
    /*private final ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }*/

    /*@Bean
    public TransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }*/
}
