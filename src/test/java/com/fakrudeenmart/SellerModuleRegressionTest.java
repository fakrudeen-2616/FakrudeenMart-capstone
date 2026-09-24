package com.fakrudeenmart;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.SellerDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.SellerService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerModuleRegressionTest {
    @Test
    void seededSellerUsesBCryptAndProfileUpdatesDoNotChangeRoleOrPassword() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:seller_auth;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO users = new UserDAO(dataSource);
            User seller = users.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow();
            UserService userService = new UserService(users);
            assertEquals(seller.getId(), userService.authenticate(seller.getEmail(), "SellerTest2026").getId());
            String hash = seller.getPasswordHash();
            SellerService service = service(dataSource, users);
            User updated = service.updateSellerProfile(seller.getId(), "Northstar Updated", "+919999999999");
            assertEquals("Northstar Updated", updated.getFullName());
            assertEquals("+919999999999", updated.getPhone());
            assertEquals(seller.getRole(), updated.getRole());
            assertEquals(hash, updated.getPasswordHash());
            assertThrows(SellerService.SellerException.class, () -> service.updateSellerProfile(seller.getId(), "", "bad"));
        }
    }

    @Test
    void sellerOwnershipAndStatusTransitionsRemainServerSide() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:seller_status;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO users = new UserDAO(dataSource);
            User seller = users.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow();
            SellerService service = service(dataSource, users);
            Product product = service.getSellerProducts(seller.getId()).stream().filter(Product::isInStock).findFirst().orElseThrow();
            assertThrows(SellerService.SellerException.class, () -> service.updateStock(seller.getId(), product.getId(), -1));
            assertThrows(SellerService.SellerException.class, () -> service.updateOrderStatus(seller.getId(), 999999L, OrderStatus.CANCELLED));
        }
    }

    private SellerService service(HikariDataSource dataSource, UserDAO users) {
        return new SellerService(new SellerDAO(dataSource), new ProductDAO(dataSource), new CategoryService(new CategoryDAO(dataSource)), new UserService(users));
    }

    private HikariDataSource dataSource(String url) {
        Properties p = new Properties();
        p.setProperty("database.url", url); p.setProperty("database.driver", "org.h2.Driver");
        p.setProperty("database.username", "sa"); p.setProperty("database.password", "");
        p.setProperty("database.pool.maximum-size", "4"); p.setProperty("database.pool.minimum-idle", "1");
        p.setProperty("database.pool.connection-timeout-ms", "5000");
        return DatabaseConnectionManager.createDataSource(p);
    }
}
