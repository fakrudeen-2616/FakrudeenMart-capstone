package com.fakrudeenmart;

import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationIntegrationTest {
    @Test
    void registrationAndLoginUseTheRealH2Database() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:auth_integration;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserService service = new UserService(new UserDAO(dataSource));
            User created = service.register("Integration Buyer", "integration@example.com", "123456789", "secret1", "secret1");

            assertNotEquals("secret1", created.getPasswordHash());
            assertEquals(UserRole.BUYER, created.getRole());
            assertEquals(UserStatus.ACTIVE, created.getStatus());
            assertEquals(created.getId(), service.authenticate("integration@example.com", "secret1").getId());
        }
    }

    @Test
    void inactiveDatabaseUserCannotAuthenticate() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:inactive_integration;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO dao = new UserDAO(dataSource);
            UserService service = new UserService(dao);
            User created = service.register("Inactive Buyer", "inactive@example.com", "123456789", "secret1", "secret1");
            service.changeAccountStatus(created.getId(), UserStatus.INACTIVE);

            UserService.AuthenticationException exception = assertThrows(UserService.AuthenticationException.class,
                    () -> service.authenticate("inactive@example.com", "secret1"));
            assertTrue(exception.getMessage().toLowerCase().contains("inactive"));
        }
    }

    private HikariDataSource dataSource(String url) {
        Properties properties = new Properties();
        properties.setProperty("database.url", url);
        properties.setProperty("database.driver", "org.h2.Driver");
        properties.setProperty("database.username", "sa");
        properties.setProperty("database.password", "");
        properties.setProperty("database.pool.maximum-size", "2");
        properties.setProperty("database.pool.minimum-idle", "1");
        properties.setProperty("database.pool.connection-timeout-ms", "5000");
        return DatabaseConnectionManager.createDataSource(properties);
    }
}
