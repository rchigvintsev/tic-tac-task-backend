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
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

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

    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = FlywayDataSourceConfig.class)
    @TestPropertySource(properties = {
            "DATABASE_URL=heffalump://host:1111/test",
            "DATABASE_DRIVER_CLASS_NAME=org.briarheart.orchestra.config.FlywayDataSourceConfigTest$HeffalumpDriver"
    })
    static class DatabaseDriverClassNameExplicitlySetTest {
        @Autowired
        @FlywayDataSource
        private DataSource flywayDataSource;

        @Test
        void shouldCreateDataSource() {
            assertNotNull(flywayDataSource);
        }
    }

    @SuppressWarnings("unused")
    public static class HeffalumpDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) {
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return false;
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 0;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
