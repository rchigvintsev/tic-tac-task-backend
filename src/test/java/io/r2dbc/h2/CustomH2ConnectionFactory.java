package io.r2dbc.h2;

import io.r2dbc.h2.H2DatabaseExceptionFactory.H2R2dbcNonTransientResourceException;
import io.r2dbc.h2.client.Client;
import io.r2dbc.h2.client.SessionClient;
import io.r2dbc.h2.codecs.DefaultCodecs;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.h2.engine.ConnectionInfo;
import org.h2.message.DbException;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import static org.h2.engine.Constants.START_URL;

/**
 * Custom implementation of {@link ConnectionFactory} that allows registration of external codecs.
 *
 * @author Roman Chigvintsev
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomH2ConnectionFactory implements ConnectionFactory {
    /**
     * Creates new instance of {@link CloseableConnectionFactory} to create connections for in-memory database with
     * the given name.
     *
     * @param name database name (must not be {@code null} or empty)
     * @return new instance of this class
     */
    public static CloseableConnectionFactory inMemory(String name) {
        return inMemory(name, "sa", "");
    }

    /**
     * Creates new instance of {@link CloseableConnectionFactory} to create connections for in-memory database with
     * the given name.
     *
     * @param name database name (must not be {@code null} or empty)
     * @param username database username (must not be {@code null} or empty)
     * @param password database password (must not be {@code null})
     * @return new instance of this class
     */
    public static CloseableConnectionFactory inMemory(String name, String username, CharSequence password) {
        return inMemory(name, username, password, Map.of());
    }

    /**
     * Creates new instance of {@link CloseableConnectionFactory} to create connections for in-memory database with
     * the given name.
     *
     * @param name database name (must not be {@code null} or empty)
     * @param username database username (must not be {@code null} or empty)
     * @param password database password (must not be {@code null})
     * @param properties database properties (must not be {@code null})
     * @return new instance of this class
     */
    public static CloseableConnectionFactory inMemory(String name,
                                                      String username,
                                                      CharSequence password,
                                                      Map<H2ConnectionOption, String> properties) {
        Assert.hasLength(name, "Database name must not be null or empty");
        Assert.hasLength(username, "Database username must not be null or empty");
        Assert.notNull(password, "Database password must not be null or empty");
        Assert.notNull(properties, "Database properties must not be null");

        H2ConnectionConfiguration.Builder configurationBuilder = H2ConnectionConfiguration.builder()
                .inMemory(name)
                .username(username)
                .password(password);
        for (Map.Entry<H2ConnectionOption, String> entry : properties.entrySet()) {
            configurationBuilder.property(entry.getKey(), entry.getValue());
        }
        return new DefaultCloseableConnectionFactory(configurationBuilder.build());
    }

    @Override
    public Mono<H2Connection> create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return H2ConnectionFactoryMetadata.INSTANCE;
    }

    private static SessionClient getSessionClient(H2ConnectionConfiguration configuration,
                                                  boolean shutdownDatabaseOnClose) {
        Assert.notNull(configuration, "Connection configuration must not be null");
        try {
            return new SessionClient(getConnectionInfo(configuration), shutdownDatabaseOnClose);
        } catch (DbException e) {
            throw H2DatabaseExceptionFactory.convert(e);
        }
    }

    private static ConnectionInfo getConnectionInfo(H2ConnectionConfiguration configuration) {
        StringBuilder urlBuilder = new StringBuilder(START_URL).append(configuration.getUrl());
        configuration.getUsername().ifPresent(username -> urlBuilder.append(";USER=").append(username));
        configuration.getPassword().ifPresent(password -> urlBuilder.append(";PASSWORD=").append(password));

        try {
            Properties properties = new Properties();
            properties.putAll(configuration.getProperties());
            return new ConnectionInfo(urlBuilder.toString(), properties);
        } catch (DbException e) {
            throw H2DatabaseExceptionFactory.convert(e);
        }
    }

    private static class DefaultCloseableConnectionFactory implements CloseableConnectionFactory {
        private final H2ConnectionConfiguration configuration;
        private final Supplier<SessionClient> clientFactory;

        private volatile SessionClient persistentConnection;

        DefaultCloseableConnectionFactory(H2ConnectionConfiguration configuration) {
            this.configuration = configuration;
            this.clientFactory = () -> getSessionClient(configuration, false);
            this.persistentConnection = getSessionClient(configuration, true);
        }

        @Override
        public Mono<Void> close() {
            return Mono.defer(() -> {
                SessionClient connection = persistentConnection;
                persistentConnection = null;
                if (connection != null) {
                    return connection.close();
                }
                return Mono.empty();
            });
        }

        @Override
        public Mono<H2Connection> create() {
            return Mono.fromSupplier(() -> {
                if (persistentConnection == null) {
                    String message = "Connection factory for " + configuration.getUrl() + " is closed";
                    throw new H2R2dbcNonTransientResourceException(message);
                }
                Client client = clientFactory.get();
                return new H2Connection(client, new DefaultCodecs(client));
            });
        }

        @Override
        public ConnectionFactoryMetadata getMetadata() {
            return H2ConnectionFactoryMetadata.INSTANCE;
        }
    }
}
