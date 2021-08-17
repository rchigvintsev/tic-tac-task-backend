package org.briarheart.orchestra.config;

import org.briarheart.orchestra.flyway.FlywayDataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Configuration of <a href="https://flywaydb.org/">Flyway</a> {@link DataSource} to enable database schema migration
 * on application startup. It requires presence of "DATABASE_URL" environment variable.
 * <p>
 * Credentials to connect to the database can be passed in two ways:
 * <ol>
 *     <li>in the database URL; for example "postgresql://username:password@host:5432/database"</li>
 *     <li>through separate environment variables "DATABASE_USERNAME" and "DATABASE_PASSWORD".</li>
 * </ol>
 * <p>
 * Optionally database driver class name can be explicitly set via environment variable "DATABASE_DRIVER_CLASS_NAME".
 *
 * @author Roman Chigvintsev
 */
@Configuration
public class FlywayDataSourceConfig {
    @Value("${DATABASE_DRIVER_CLASS_NAME:}")
    private String databaseDriverClassName;

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
        return FlywayDataSourceBuilder.create()
                .driverClassName(databaseDriverClassName)
                .url(databaseUrl)
                .username(credentials[0])
                .password(credentials[1])
                .type(DriverManagerDataSource.class)
                .build();
    }

    private String[] getCredentials() {
        if (StringUtils.hasText(databaseUsername)) {
            return new String[]{databaseUsername, !StringUtils.hasLength(databasePassword) ? null : databasePassword};
        }
        return parseCredentials(databaseUrl);
    }

    private static String[] parseCredentials(String databaseUrl) {
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
                return new String[]{usernameAndPassword, null};
            }
        }
        return new String[2];
    }
}
