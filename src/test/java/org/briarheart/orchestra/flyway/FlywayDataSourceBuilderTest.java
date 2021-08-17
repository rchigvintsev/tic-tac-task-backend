package org.briarheart.orchestra.flyway;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Roman Chigvintsev
 */
class FlywayDataSourceBuilderTest {
    @Test
    void shouldCreateDataSource() {
        DataSource dataSource = FlywayDataSourceBuilder.create()
                .url("jdbc:postgresql://host:5432/test")
                .username("admin")
                .password("secret")
                .type(DriverManagerDataSource.class)
                .build();
        assertNotNull(dataSource);
    }

    @Test
    void shouldPrependJdbcPrefixToUrlIfNecessary() {
        DriverManagerDataSource dataSource = FlywayDataSourceBuilder.create()
                .url("postgresql://host:5432/test")
                .username("admin")
                .password("secret")
                .type(DriverManagerDataSource.class)
                .build();
        assertEquals("jdbc:postgresql://host:5432/test", dataSource.getUrl());
    }

    @Test
    void shouldReplacePostgresWithPostgreSqlInUrl() {
        DriverManagerDataSource dataSource = FlywayDataSourceBuilder.create()
                .url("jdbc:postgres://host:5432/test")
                .username("admin")
                .password("secret")
                .type(DriverManagerDataSource.class)
                .build();
        assertEquals("jdbc:postgresql://host:5432/test", dataSource.getUrl());
    }
}
