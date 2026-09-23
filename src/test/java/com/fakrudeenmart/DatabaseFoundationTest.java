package com.fakrudeenmart;

import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseFoundationTest {
    @Test
    void applicationConfigurationCanBeLoaded() {
        Properties properties = DatabaseConnectionManager.loadProperties();
        assertEquals("org.h2.Driver", properties.getProperty("database.driver"));
        assertTrue(properties.getProperty("database.url").startsWith("jdbc:h2:file:"));
    }

    @Test
    void hikariCanCreateAnH2Connection() throws Exception {
        Properties properties = testProperties("jdbc:h2:mem:connection_test;DB_CLOSE_DELAY=-1");
        try (HikariDataSource dataSource = DatabaseConnectionManager.createDataSource(properties);
             Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(2));
        }
    }

    @Test
    void schemaInitializationCreatesAllFoundationTables() throws Exception {
        Properties properties = testProperties("jdbc:h2:mem:schema_test;DB_CLOSE_DELAY=-1");
        try (HikariDataSource dataSource = DatabaseConnectionManager.createDataSource(properties)) {
            DatabaseInitializer.initialize(dataSource);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet tables = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                         + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME IN "
                         + "('USERS','CATEGORIES','PRODUCTS','CART','CART_ITEMS','ORDERS','ORDER_ITEMS','REVIEWS')")) {
                assertTrue(tables.next());
                assertEquals(8, tables.getInt(1));
            }
        }
    }

    @Test
    void schemaInitializationIsRepeatableAndNonDestructive() throws Exception {
        Properties properties = testProperties("jdbc:h2:mem:repeatable_test;DB_CLOSE_DELAY=-1");
        try (HikariDataSource dataSource = DatabaseConnectionManager.createDataSource(properties)) {
            DatabaseInitializer.initialize(dataSource);
            int initialCategoryCount;
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM categories")) {
                rows.next();
                initialCategoryCount = rows.getInt(1);
            }
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO categories(name) VALUES ('Test category')");
            }
            DatabaseInitializer.initialize(dataSource);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM categories")) {
                assertTrue(rows.next());
                assertEquals(initialCategoryCount + 1, rows.getInt(1));
            }
        }
    }

    private Properties testProperties(String url) {
        Properties properties = new Properties();
        properties.setProperty("database.url", url);
        properties.setProperty("database.driver", "org.h2.Driver");
        properties.setProperty("database.username", "sa");
        properties.setProperty("database.password", "");
        properties.setProperty("database.pool.maximum-size", "2");
        properties.setProperty("database.pool.minimum-idle", "1");
        properties.setProperty("database.pool.connection-timeout-ms", "5000");
        return properties;
    }
}
