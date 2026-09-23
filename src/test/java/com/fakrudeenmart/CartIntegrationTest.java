package com.fakrudeenmart;

import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartIntegrationTest {
    @Test
    void cartPersistsMergesQuantitiesUsesCurrentPricesAndEnforcesStock() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:cart_integration;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO userDAO = new UserDAO(dataSource);
            User buyer = userDAO.createUser(new User("Cart Buyer", "cart-buyer@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE));
            ProductService productService = new ProductService(new ProductDAO(dataSource), new com.fakrudeenmart.dao.CategoryDAO(dataSource), userDAO);
            Product product = new ProductDAO(dataSource).findAll(1, 12).getItems().stream().filter(Product::isInStock).findFirst().orElseThrow();
            CartService service = new CartService(new CartDAO(dataSource), new CartItemDAO(dataSource), productService, new UserService(userDAO));

            Cart afterFirstAdd = service.addToCart(buyer.getId(), product.getId(), 2);
            assertEquals(1, afterFirstAdd.getItems().size());
            service.addToCart(buyer.getId(), product.getId(), 3);
            Cart merged = service.getCart(buyer.getId());
            assertEquals(5, merged.getItems().get(0).getQuantity());
            assertEquals(product.getPrice().multiply(new BigDecimal("5")), merged.getSubtotal());

            try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("UPDATE products SET price = ? WHERE id = ?")) {
                statement.setBigDecimal(1, new BigDecimal("10.00")); statement.setLong(2, product.getId()); statement.executeUpdate();
            }
            assertEquals(new BigDecimal("50.00"), service.getCart(buyer.getId()).getSubtotal());
            assertThrows(CartService.CartException.class, () -> service.addToCart(buyer.getId(), product.getId(), product.getStockQuantity()));

            Cart persisted = new CartService(new CartDAO(dataSource), new CartItemDAO(dataSource), productService, new UserService(userDAO)).getCart(buyer.getId());
            assertEquals(5, persisted.getItems().get(0).getQuantity());
            Long itemId = persisted.getItems().get(0).getItemId();
            service.updateQuantity(buyer.getId(), itemId, 1);
            assertEquals(1, service.getCart(buyer.getId()).getItems().get(0).getQuantity());
            service.removeItem(buyer.getId(), itemId);
            assertTrue(service.getCart(buyer.getId()).getItems().isEmpty());
        }
    }

    @Test
    void inactiveAndOutOfStockProductsAreRejectedAndUsersCannotUseOtherCarts() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:cart_security;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO userDAO = new UserDAO(dataSource);
            User buyerA = userDAO.createUser(new User("Buyer A", "cart-a@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE));
            User buyerB = userDAO.createUser(new User("Buyer B", "cart-b@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE));
            User seller = userDAO.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow();
            ProductDAO productDAO = new ProductDAO(dataSource);
            ProductService productService = new ProductService(productDAO, new com.fakrudeenmart.dao.CategoryDAO(dataSource), userDAO);
            CartService service = new CartService(new CartDAO(dataSource), new CartItemDAO(dataSource), productService, new UserService(userDAO));
            Product available = productDAO.findAll(1, 12).getItems().stream().filter(Product::isInStock).findFirst().orElseThrow();
            service.addToCart(buyerA.getId(), available.getId(), 1);
            Long itemId = service.getCart(buyerA.getId()).getItems().get(0).getItemId();
            service.getCart(buyerB.getId());
            assertThrows(CartService.CartException.class, () -> service.updateQuantity(buyerB.getId(), itemId, 1));
            assertThrows(CartService.CartException.class, () -> service.removeItem(buyerB.getId(), itemId));
            assertThrows(CartService.CartException.class, () -> service.getCart(seller.getId()));

            Product inactive = findByStatus(dataSource, "INACTIVE");
            assertThrows(CartService.CartException.class, () -> service.addToCart(buyerA.getId(), inactive.getId(), 1));
            Product outOfStock = productDAO.findByFilters(new ProductDAO.CatalogFilter(null, null, null, null, com.fakrudeenmart.model.CatalogOptions.StockFilter.OUT_OF_STOCK, com.fakrudeenmart.model.CatalogOptions.SortOption.NEWEST, 1, 12)).getItems().get(0);
            assertThrows(CartService.CartException.class, () -> service.addToCart(buyerA.getId(), outOfStock.getId(), 1));
        }
    }

    private Product findByStatus(HikariDataSource dataSource, String status) throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT id FROM products WHERE status = ? LIMIT 1")) {
            statement.setString(1, status);
            try (var result = statement.executeQuery()) { result.next(); return new ProductDAO(dataSource).findById(result.getLong(1)).orElseGet(() -> { Product product = new Product(); product.setId(resultId(result)); return product; }); }
        }
    }
    private long resultId(java.sql.ResultSet result) { try { return result.getLong(1); } catch (Exception exception) { throw new AssertionError(exception); } }
    private HikariDataSource dataSource(String url) {
        Properties properties = new Properties(); properties.setProperty("database.url", url); properties.setProperty("database.driver", "org.h2.Driver"); properties.setProperty("database.username", "sa"); properties.setProperty("database.password", ""); properties.setProperty("database.pool.maximum-size", "2"); properties.setProperty("database.pool.minimum-idle", "1"); properties.setProperty("database.pool.connection-timeout-ms", "5000"); return DatabaseConnectionManager.createDataSource(properties);
    }
}
