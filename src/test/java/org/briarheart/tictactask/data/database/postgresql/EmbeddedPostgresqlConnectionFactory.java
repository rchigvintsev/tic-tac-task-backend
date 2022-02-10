package org.briarheart.tictactask.data.database.postgresql;

import io.jsonwebtoken.lang.Assert;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.postgresql.ds.common.BaseDataSource;
import org.reactivestreams.Publisher;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

public class EmbeddedPostgresqlConnectionFactory implements ConnectionFactory {
    private final DataSource dataSource;

    private volatile BaseDataSource baseDataSource;
    private volatile PostgresqlConnectionFactory connectionFactory;

    public EmbeddedPostgresqlConnectionFactory(DataSource dataSource) {
        Assert.notNull(dataSource, "Data source must not be null");
        this.dataSource = dataSource;
    }

    @Override
    public Publisher<PostgresqlConnection> create() {
        return connectionFactory().create();
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return connectionFactory().getMetadata();
    }

    private PostgresqlConnectionFactory connectionFactory() {
        BaseDataSource baseDataSource = getBaseDataSource();
        if (this.connectionFactory == null || this.baseDataSource != baseDataSource) {
            this.baseDataSource = baseDataSource;
            this.connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                    .host(baseDataSource.getServerNames()[0])
                    .port(baseDataSource.getPortNumbers()[0])
                    .username(Objects.requireNonNull(baseDataSource.getUser()))
                    .password(baseDataSource.getPassword())
                    .database(baseDataSource.getDatabaseName())
                    .build());
        }
        return connectionFactory;
    }

    private BaseDataSource getBaseDataSource() {
        try {
            return dataSource.unwrap(BaseDataSource.class);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to unwrap " + BaseDataSource.class.getName() + ": " + e.getMessage(), e);
        }
    }
}
