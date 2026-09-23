package com.fakrudeenmart;

import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.OrderItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.model.Order;
import com.fakrudeenmart.model.OrderItem;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderIntegrationTest {
    @Test
    void successfulCheckoutCreatesSnapshotsDeductsStockAndClearsCart() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:orders_success;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            Fixture fixture = fixture(dataSource, "order-buyer@example.com");
            Product product = availableProduct(fixture.productDAO);
            setProduct(dataSource, product.getId(), "Snapshot Headphones", new BigDecimal("2499.00"), 10, "ACTIVE");
            fixture.cartService.addToCart(fixture.buyer.getId(), product.getId(), 3);

            Order order = fixture.orderService.placeOrder(fixture.buyer.getId());
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            assertEquals(new BigDecimal("7497.00"), order.getTotalAmount());
            assertEquals(1, order.getItems().size());
            assertEquals("Snapshot Headphones", order.getItems().get(0).getProductName());
            assertEquals(new BigDecimal("2499.00"), order.getItems().get(0).getUnitPrice());
            assertEquals(new BigDecimal("7497.00"), order.getItems().get(0).getSubtotal());
            assertEquals(7, stock(dataSource, product.getId()));
            assertTrue(fixture.cartService.getCart(fixture.buyer.getId()).getItems().isEmpty());

            setProduct(dataSource, product.getId(), "Renamed Headphones", new BigDecimal("2999.00"), 7, "ACTIVE");
            Order historical = fixture.orderService.getOrderById(fixture.buyer.getId(), order.getId());
            assertEquals("Snapshot Headphones", historical.getItems().get(0).getProductName());
            assertEquals(new BigDecimal("2499.00"), historical.getItems().get(0).getUnitPrice());
            assertEquals(new BigDecimal("7497.00"), historical.getTotalAmount());
        }
    }

    @Test
    void emptyInsufficientAndInactiveCheckoutsCreateNoOrderAndPreserveCart() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:orders_validation;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            Fixture fixture = fixture(dataSource, "validation-buyer@example.com");
            assertThrows(OrderService.OrderException.class, () -> fixture.orderService.placeOrder(fixture.buyer.getId()));
            Product product = availableProduct(fixture.productDAO);
            setProduct(dataSource, product.getId(), "Limited Product", new BigDecimal("100.00"), 3, "ACTIVE");
            fixture.cartService.addToCart(fixture.buyer.getId(), product.getId(), 2);
            setProduct(dataSource, product.getId(), "Limited Product", new BigDecimal("100.00"), 1, "ACTIVE");
            assertThrows(OrderService.OrderException.class, () -> fixture.orderService.placeOrder(fixture.buyer.getId()));
            assertEquals(0, fixture.orderDAO.findByUserId(fixture.buyer.getId()).size());
            assertEquals(1, stock(dataSource, product.getId()));
            assertEquals(2, fixture.cartService.getCart(fixture.buyer.getId()).getItems().get(0).getQuantity());

            setProduct(dataSource, product.getId(), "Limited Product", new BigDecimal("100.00"), 1, "INACTIVE");
            assertThrows(OrderService.OrderException.class, () -> fixture.orderService.placeOrder(fixture.buyer.getId()));
            assertEquals(0, fixture.orderDAO.findByUserId(fixture.buyer.getId()).size());
            assertEquals(2, fixture.cartService.getCart(fixture.buyer.getId()).getItems().get(0).getQuantity());
        }
    }

    @Test
    void transactionFailureRollsBackOrderItemsStockAndCart() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:orders_rollback;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            Fixture fixture = fixture(dataSource, "rollback-buyer@example.com");
            Product product = availableProduct(fixture.productDAO);
            setProduct(dataSource, product.getId(), "Rollback Product", new BigDecimal("50.00"), 8, "ACTIVE");
            fixture.cartService.addToCart(fixture.buyer.getId(), product.getId(), 2);
            OrderItemDAO failingItems = new OrderItemDAO(dataSource) {
                @Override public OrderItem create(Connection connection, OrderItem item) { throw new DataAccessException("Deliberate order item failure", null); }
            };
            OrderService failingService = new OrderService(dataSource, fixture.cartDAO, fixture.cartItemDAO, fixture.productDAO, fixture.orderDAO, failingItems, fixture.userService);
            assertThrows(DataAccessException.class, () -> failingService.placeOrder(fixture.buyer.getId()));
            assertEquals(0, fixture.orderDAO.findByUserId(fixture.buyer.getId()).size());
            assertEquals(8, stock(dataSource, product.getId()));
            assertEquals(2, fixture.cartService.getCart(fixture.buyer.getId()).getItems().get(0).getQuantity());
        }
    }

    @Test
    void buyerCannotReadAnotherUsersOrder() throws Exception {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:orders_ownership;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            Fixture owner = fixture(dataSource, "owner@example.com");
            Fixture other = fixture(dataSource, "other@example.com");
            Product product = availableProduct(owner.productDAO);
            setProduct(dataSource, product.getId(), "Owned Product", new BigDecimal("25.00"), 4, "ACTIVE");
            owner.cartService.addToCart(owner.buyer.getId(), product.getId(), 1);
            Order order = owner.orderService.placeOrder(owner.buyer.getId());
            assertThrows(OrderService.OrderException.class, () -> other.orderService.getOrderById(other.buyer.getId(), order.getId()));
        }
    }

    private Fixture fixture(HikariDataSource dataSource, String email) {
        UserService userService = new UserService(new com.fakrudeenmart.dao.UserDAO(dataSource));
        User buyer = new com.fakrudeenmart.dao.UserDAO(dataSource).createUser(new User("Order Buyer", email, "hash", null, UserRole.BUYER, UserStatus.ACTIVE));
        CartDAO cartDAO = new CartDAO(dataSource); CartItemDAO cartItemDAO = new CartItemDAO(dataSource); ProductDAO productDAO = new ProductDAO(dataSource); OrderDAO orderDAO = new OrderDAO(dataSource); OrderItemDAO orderItemDAO = new OrderItemDAO(dataSource);
        ProductService productService = new ProductService(productDAO, new com.fakrudeenmart.dao.CategoryDAO(dataSource), new com.fakrudeenmart.dao.UserDAO(dataSource));
        com.fakrudeenmart.service.CartService cartService = new com.fakrudeenmart.service.CartService(cartDAO, cartItemDAO, productService, userService);
        OrderService orderService = new OrderService(dataSource, cartDAO, cartItemDAO, productDAO, orderDAO, orderItemDAO, userService);
        return new Fixture(buyer, cartDAO, cartItemDAO, productDAO, orderDAO, userService, cartService, orderService);
    }

    private Product availableProduct(ProductDAO productDAO) { return productDAO.findAll(1, 50).getItems().stream().filter(Product::isInStock).findFirst().orElseThrow(); }
    private void setProduct(HikariDataSource dataSource, Long id, String name, BigDecimal price, int stock, String status) throws Exception { try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("UPDATE products SET name = ?, price = ?, stock_quantity = ?, status = ?, active = ? WHERE id = ?")) { statement.setString(1, name); statement.setBigDecimal(2, price); statement.setInt(3, stock); statement.setString(4, status); statement.setBoolean(5, "ACTIVE".equals(status)); statement.setLong(6, id); statement.executeUpdate(); } }
    private int stock(HikariDataSource dataSource, Long id) throws Exception { try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT stock_quantity FROM products WHERE id = ?")) { statement.setLong(1, id); try (var result = statement.executeQuery()) { result.next(); return result.getInt(1); } } }
    private HikariDataSource dataSource(String url) { Properties properties = new Properties(); properties.setProperty("database.url", url); properties.setProperty("database.driver", "org.h2.Driver"); properties.setProperty("database.username", "sa"); properties.setProperty("database.password", ""); properties.setProperty("database.pool.maximum-size", "3"); properties.setProperty("database.pool.minimum-idle", "1"); properties.setProperty("database.pool.connection-timeout-ms", "5000"); return DatabaseConnectionManager.createDataSource(properties); }

    private record Fixture(User buyer, CartDAO cartDAO, CartItemDAO cartItemDAO, ProductDAO productDAO, OrderDAO orderDAO, UserService userService, com.fakrudeenmart.service.CartService cartService, OrderService orderService) {}
}
