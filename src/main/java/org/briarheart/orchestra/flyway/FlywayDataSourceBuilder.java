package org.briarheart.orchestra.flyway;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Specialized version of {@link DataSourceBuilder} that tolerant to some otherwise incorrect input values like empty
 * driver class name or database URL without "jdbc:" prefix.
 *
 * @author Roman Chigvintsev
 */
@SuppressWarnings("unchecked")
public class FlywayDataSourceBuilder<T extends DataSource> {
    private final DataSourceBuilder<T> delegate;

    private FlywayDataSourceBuilder(ClassLoader classLoader) {
        this.delegate = (DataSourceBuilder<T>) DataSourceBuilder.create(classLoader);
    }

    private FlywayDataSourceBuilder(T deriveFrom) {
        Assert.notNull(deriveFrom, "Data source must not be null");
        this.delegate = (DataSourceBuilder<T>) DataSourceBuilder.derivedFrom(deriveFrom);
    }

    /**
     * Sets {@link DataSource} type that should be built.
     *
     * @param <D> data source type
     * @param type data source type
     * @return this builder
     */
    public <D extends DataSource> FlywayDataSourceBuilder<D> type(Class<D> type) {
        delegate.type(type);
        return (FlywayDataSourceBuilder<D>) this;
    }

    /**
     * Sets driver class name that should be used when building data source.
     *
     * @param driverClassName driver class name
     * @return this builder
     */
    public FlywayDataSourceBuilder<T> driverClassName(String driverClassName) {
        if (StringUtils.hasLength(driverClassName)) {
            delegate.driverClassName(driverClassName);
        }
        return this;
    }

    /**
     * Sets URL that should be used when building data source.
     *
     * @param url JDBC url
     * @return this builder
     */
    public FlywayDataSourceBuilder<T> url(String url) {
        if (StringUtils.hasLength(url)) {
            url = getUrlWithoutCredentials(url);
            UrlNormalizer urlNormalizer = getUrlNormalizer(url);
            delegate.url(urlNormalizer.normalize(url));
        }
        return this;
    }

    /**
     * Sets username that should be used when building data source.
     *
     * @param username username
     * @return this builder
     */
    public FlywayDataSourceBuilder<T> username(String username) {
        delegate.username(username);
        return this;
    }

    /**
     * Sets password that should be used when building data source.
     *
     * @param password password
     * @return this builder
     */
    public FlywayDataSourceBuilder<T> password(String password) {
        delegate.password(password);
        return this;
    }

    /**
     * Returns a newly built {@link DataSource} instance.
     *
     * @return built data source
     */
    public T build() {
        return delegate.build();
    }

    /**
     * Creates new instance of this class.
     *
     * @return new Flyway data source builder instance
     */
    public static FlywayDataSourceBuilder<?> create() {
        return create(null);
    }

    /**
     * Creates new instance of this class.
     *
     * @param classLoader classloader used to discover preferred settings
     * @return new Flyway data source builder instance
     */
    public static FlywayDataSourceBuilder<?> create(ClassLoader classLoader) {
        return new FlywayDataSourceBuilder<>(classLoader);
    }

    /**
     * Creates new instance of this class derived from the specified data source. Returned builder can be used to build
     * the same type of {@link DataSource} with {@code username}, {@code password}, {@code url} and
     * {@code driverClassName} properties copied from the original when not specifically set.
     *
     * @param dataSource data source
     * @return new Flyway data source builder instance
     */
    public static FlywayDataSourceBuilder<?> derivedFrom(DataSource dataSource) {
        if (dataSource instanceof EmbeddedDatabase) {
            try {
                dataSource = dataSource.unwrap(DataSource.class);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to unwrap embedded database", e);
            }
        }
        return new FlywayDataSourceBuilder<>(dataSource);
    }

    /**
     * Finds {@link DataSource} type preferred for the given class loader.
     *
     * @param classLoader class loader used to discover preferred settings
     * @return preferred {@link DataSource} type
     */
    public static Class<? extends DataSource> findType(ClassLoader classLoader) {
        return DataSourceBuilder.findType(classLoader);
    }

    private static String getUrlWithoutCredentials(String url) {
        int idx = url.indexOf("://");
        if (idx > 0) {
            int atIdx = url.indexOf('@', idx);
            if (atIdx > 0) {
                return url.substring(0, idx + 3) + url.substring(atIdx + 1);
            }
        }
        return url;
    }

    private static UrlNormalizer getUrlNormalizer(String url) {
        if (PostgresUrlNormalizer.INSTANCE.supports(url)) {
            return PostgresUrlNormalizer.INSTANCE;
        }
        return DefaultUrlNormalizer.INSTANCE;
    }

    private interface UrlNormalizer {
        boolean supports(String url);

        String normalize(String url);
    }

    private static class DefaultUrlNormalizer implements UrlNormalizer {
        private static final DefaultUrlNormalizer INSTANCE = new DefaultUrlNormalizer();

        @Override
        public boolean supports(String url) {
            return StringUtils.hasLength(url);
        }

        @Override
        public String normalize(String url) {
            if (!url.toLowerCase().startsWith("jdbc:")) {
                return "jdbc:" + url;
            }
            return url;
        }
    }

    private static class PostgresUrlNormalizer extends DefaultUrlNormalizer {
        private static final PostgresUrlNormalizer INSTANCE = new PostgresUrlNormalizer();

        @Override
        public boolean supports(String url) {
            return super.supports(url) && url.toLowerCase().contains("postgres");
        }

        @Override
        public String normalize(String url) {
            String normalizedUrl = super.normalize(url);
            if (normalizedUrl.toLowerCase().startsWith("jdbc:postgres://")) {
                normalizedUrl = "jdbc:postgresql" + normalizedUrl.substring(13);
            }
            return normalizedUrl;
        }
    }
}
