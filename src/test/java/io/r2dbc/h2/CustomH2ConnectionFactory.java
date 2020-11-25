package io.r2dbc.h2;

import io.r2dbc.h2.H2DatabaseExceptionFactory.H2R2dbcNonTransientResourceException;
import io.r2dbc.h2.client.Client;
import io.r2dbc.h2.client.SessionClient;
import io.r2dbc.h2.codecs.Codecs;
import io.r2dbc.h2.codecs.DefaultCodecs;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import lombok.RequiredArgsConstructor;
import org.h2.engine.ConnectionInfo;
import org.h2.message.DbException;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.h2.engine.Constants.START_URL;

/**
 * Custom implementation of {@link CloseableConnectionFactory} that allows registration of external codecs.
 *
 * @author Roman Chigvintsev
 */
public class CustomH2ConnectionFactory implements CloseableConnectionFactory {
    private final H2ConnectionConfiguration configuration;
    private final Supplier<SessionClient> clientProvider;
    private final Function<Client, Codecs> codecsProvider;

    private volatile SessionClient persistentConnection;

    private CustomH2ConnectionFactory(H2ConnectionConfiguration configuration,
                                      Function<Client, Codecs> codecsProvider) {
        this.configuration = configuration;
        this.clientProvider = () -> getSessionClient(false);
        this.persistentConnection = getSessionClient(true);
        this.codecsProvider = codecsProvider;
    }

    /**
     * Creates new instance of {@link InMemoryBuilder} for in-memory database with the given name.
     *
     * @param name database name (must not be {@code null} or empty)
     * @return new instance of {@link InMemoryBuilder}
     */
    public static InMemoryBuilder inMemory(String name) {
        return inMemory(name, "sa", "");
    }

    /**
     * Creates new instance of {@link InMemoryBuilder} for in-memory database with the given name.
     *
     * @param databaseName database name (must not be {@code null} or empty)
     * @param username database username (must not be {@code null} or empty)
     * @param password database password (must not be {@code null})
     * @return new instance of {@link InMemoryBuilder}
     */
    public static InMemoryBuilder inMemory(String databaseName,
                                           String username,
                                           CharSequence password) {
        Assert.hasLength(databaseName, "Database name must not be null or empty");
        Assert.hasLength(username, "Database username must not be null or empty");
        Assert.notNull(password, "Database password must not be null or empty");
        return new InMemoryBuilder(databaseName, username, password);
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
                String message = "Connection factory for \"" + configuration.getUrl() + "\" is closed";
                throw new H2R2dbcNonTransientResourceException(message);
            }
            Client client = clientProvider.get();
            return new H2Connection(client, codecsProvider.apply(client));
        });
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return H2ConnectionFactoryMetadata.INSTANCE;
    }

    private SessionClient getSessionClient(boolean shutdownDatabaseOnClose) {
        try {
            return new SessionClient(getConnectionInfo(), shutdownDatabaseOnClose);
        } catch (DbException e) {
            throw H2DatabaseExceptionFactory.convert(e);
        }
    }

    private ConnectionInfo getConnectionInfo() {
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

    @RequiredArgsConstructor
    public static class InMemoryBuilder {
        private final String name;
        private final String username;
        private final CharSequence password;

        private Map<H2ConnectionOption, String> properties;
        private Function<Client, Codecs> codecsProvider = DefaultCodecs::new;

        public InMemoryBuilder withProperties(Map<H2ConnectionOption, String> properties) {
            Assert.notNull(properties, "Properties must not be null");
            this.properties = properties;
            return this;
        }

        public InMemoryBuilder withCodecsProvider(Function<Client, Codecs> codecsProvider) {
            Assert.notNull(codecsProvider, "Codecs provider must not be null");
            this.codecsProvider = codecsProvider;
            return this;
        }

        public CloseableConnectionFactory build() {
            H2ConnectionConfiguration.Builder configurationBuilder = H2ConnectionConfiguration.builder()
                    .inMemory(name)
                    .username(username)
                    .password(password);
            if (properties != null) {
                for (Map.Entry<H2ConnectionOption, String> entry : properties.entrySet()) {
                    configurationBuilder.property(entry.getKey(), entry.getValue());
                }
            }
            return new CustomH2ConnectionFactory(configurationBuilder.build(), codecsProvider);
        }
    }
}
