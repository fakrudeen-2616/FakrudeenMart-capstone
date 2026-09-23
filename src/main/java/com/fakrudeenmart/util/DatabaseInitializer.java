package com.fakrudeenmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Applies CREATE IF NOT EXISTS statements without dropping existing application data. */
public final class DatabaseInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String SCHEMA_RESOURCE = "schema.sql";

    private DatabaseInitializer() {
    }

    public static void initialize(DataSource dataSource) {
        try (InputStream input = DatabaseInitializer.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing classpath resource: " + SCHEMA_RESOURCE);
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            executeStatements(dataSource, schema);
            LOGGER.info("Database schema initialized successfully");
        } catch (IOException | SQLException exception) {
            LOGGER.error("Database schema initialization failed", exception);
            throw new IllegalStateException("Database schema initialization failed", exception);
        }
    }

    static void executeStatements(DataSource dataSource, String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }
}
