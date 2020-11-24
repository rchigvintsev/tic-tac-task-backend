package org.briarheart.orchestra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Roman Chigvintsev
 */
class FlywayDataSourceConfigTest {
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = "DATABASE_URL=h2:mem:test")
    static class DatabaseUrlWithoutRootSlashesTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldCreateDataSource() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("jdbc:h2:mem:test", driverManagerDataSource.getUrl());
        }
    }

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = "DATABASE_URL=postgresql://host:5432/test")
    static class DatabaseUrlWithoutCredentialsTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldCreateDataSource() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("jdbc:postgresql://host:5432/test", driverManagerDataSource.getUrl());
        }
    }

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = {
            "DATABASE_URL=postgresql://admin:secret@host:5432/test",
            "DATABASE_USERNAME=",
            "DATABASE_PASSWORD="
    })
    static class DatabaseUrlWithCredentialsTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldExtractDatabaseCredentialsFromUrl() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("jdbc:postgresql://host:5432/test", driverManagerDataSource.getUrl());
            assertEquals("admin", driverManagerDataSource.getUsername());
            assertEquals("secret", driverManagerDataSource.getPassword());
        }
    }

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = {
            "DATABASE_URL=postgresql://admin@host:5432/test",
            "DATABASE_USERNAME=",
            "DATABASE_PASSWORD="
    })
    static class DatabaseUrlWithCredentialsWithoutPasswordTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldExtractDatabaseCredentialsFromUrl() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("admin", driverManagerDataSource.getUsername());
            assertNull(driverManagerDataSource.getPassword());
        }
    }

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = {
            "DATABASE_URL=postgresql://admin:secret@host:5432/test",
            "DATABASE_USERNAME=alice",
            "DATABASE_PASSWORD=qwerty"
    })
    static class CredentialsPriorityTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldPreferIndividualDatabaseCredentialsEnvironmentVariables() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("alice", driverManagerDataSource.getUsername());
            assertEquals("qwerty", driverManagerDataSource.getPassword());
        }
    }

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = {
            "DATABASE_URL=postgresql://admin:secret@host:5432/test",
            "DATABASE_USERNAME=alice"
    })
    static class CredentialsPriorityWithoutDatabasePasswordEnvironmentVariableTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldPreferIndividualDatabaseCredentialsEnvironmentVariables() throws SQLException {
            DriverManagerDataSource driverManagerDataSource = flywayDataSource.unwrap(DriverManagerDataSource.class);
            assertEquals("alice", driverManagerDataSource.getUsername());
            assertNull(driverManagerDataSource.getPassword());
        }
    }
}
