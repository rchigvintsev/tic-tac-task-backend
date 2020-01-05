package org.briarheart.orchestra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Configuration of <a href="https://flywaydb.org/">Flyway</a> {@link DataSource} to enable database schema migration
 * on application startup. It requires presence of "DATABASE_URL" environment variable. Value of this variable should
 * be database URL without "jdbc:" prefix. For example: postgresql://host:5432/database.
 * <p>
 * Credentials to connect to the database can be passed in two ways:
 * <ol>
 *     <li>in the database URL; for example "postgresql://username:password@host:5432/database"</li>
 *     <li>through separate environment variables "DATABASE_USERNAME" and "DATABASE_PASSWORD".</li>
 * </ol>
 *
 * @author Roman Chigvintsev
 */
@Configuration
public class FlywayDataSourceConfig {
    @Value("${DATABASE_URL}")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:}")
    private String databaseUsername;

    @Value("${DATABASE_PASSWORD:}")
    private String databasePassword;

    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource() {
        String[] credentials = getCredentials();
        return DataSourceBuilder.create()
                .url(getDataSourceUrl())
                .username(credentials[0])
                .password(credentials[1])
                .type(DriverManagerDataSource.class)
                .build();
    }

    private String getDataSourceUrl() {
        String url = databaseUrl;
        int idx = databaseUrl.indexOf("://");
        if (idx > 0) {
            int atIdx = databaseUrl.indexOf('@', idx);
            if (atIdx > 0) {
                url = databaseUrl.substring(0, idx + 3) + databaseUrl.substring(atIdx + 1);
            }
        }
        return "jdbc:" + url;
    }

    private String[] getCredentials() {
        if (StringUtils.hasText(databaseUsername)) {
            return new String[]{databaseUsername, StringUtils.isEmpty(databasePassword) ? null : databasePassword};
        }
        return parseCredentials();
    }

    private String[] parseCredentials() {
        int idx = databaseUrl.indexOf("://");
        if (idx > 0) {
            int atIdx = databaseUrl.indexOf('@', idx);
            if (atIdx > 0) {
                String usernameAndPassword = databaseUrl.substring(idx + 3, atIdx);
                int colonIdx = usernameAndPassword.indexOf(':');
                if (colonIdx > 0) {
                    return new String[]{
                            usernameAndPassword.substring(0, colonIdx),
                            usernameAndPassword.substring(colonIdx + 1)
                    };
                }
                return new String[]{usernameAndPassword, ""};
            }
        }
        return new String[2];
    }
}
