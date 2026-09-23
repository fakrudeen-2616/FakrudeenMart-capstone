package com.fakrudeenmart.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/** Owns the application connection pool; SQL remains in the DAO layer. */
public final class DatabaseConnectionManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private static final String CONFIG_RESOURCE = "application.properties";
    private static volatile DatabaseConnectionManager instance;

    private final HikariDataSource dataSource;

    private DatabaseConnectionManager(Properties properties) {
        this.dataSource = createDataSource(properties);
    }

    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager(loadProperties());
                }
            }
        }
        return instance;
    }

    public static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnectionManager.class.getClassLoader()
                .getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing classpath resource: " + CONFIG_RESOURCE);
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load database configuration", exception);
        }
    }

    public static HikariDataSource createDataSource(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(required(properties, "database.url"));
        config.setDriverClassName(required(properties, "database.driver"));
        config.setUsername(properties.getProperty("database.username", ""));
        config.setPassword(properties.getProperty("database.password", ""));
        config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("database.pool.maximum-size", "5")));
        config.setMinimumIdle(Integer.parseInt(properties.getProperty("database.pool.minimum-idle", "1")));
        config.setConnectionTimeout(Long.parseLong(properties.getProperty("database.pool.connection-timeout-ms", "10000")));
        config.setPoolName("FakrudeenMartPool");
        return new HikariDataSource(config);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required database property: " + key);
        }
        return value.trim();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            LOGGER.info("Closing database connection pool");
            dataSource.close();
        }
    }
}
