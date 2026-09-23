package com.fakrudeenmart;

import com.fakrudeenmart.dao.AdminDAO;
import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.OrderItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.SellerDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.AdminOrder;
import com.fakrudeenmart.model.AdminStats;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.AdminService;
import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminIntegrationTest {
    @Test
    void adminCanSearchManageAccountsAndCannotDisableSelfOrLastAdmin() {
        try (HikariDataSource ds = dataSource("jdbc:h2:mem:admin_users;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users = new UserDAO(ds); User admin = users.createUser(new User("Platform Admin", "admin@example.com", "hash", "1", UserRole.ADMIN, UserStatus.ACTIVE)); User buyer = users.createUser(new User("Buyer Searchable", "buyer-search@example.com", "hash", "2", UserRole.BUYER, UserStatus.ACTIVE)); User seller = users.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow();
            AdminService service = service(ds, users);
            assertEquals(1, service.getBuyers(admin.getId(), "searchable", null).size()); assertEquals(1, service.getSellers(admin.getId(), "northstar", UserStatus.ACTIVE).size()); assertTrue(service.getUsers(admin.getId(), null, UserRole.ADMIN, UserStatus.ACTIVE).stream().anyMatch(u -> u.getId().equals(admin.getId())));
            service.updateBuyerProfile(admin.getId(), buyer.getId(), "Updated Buyer", "buyer-updated@example.com", "3"); assertEquals("Updated Buyer", service.getBuyer(admin.getId(), buyer.getId()).getFullName()); assertEquals("buyer-updated@example.com", service.getBuyer(admin.getId(), buyer.getId()).getEmail());
            service.updateBuyerStatus(admin.getId(), buyer.getId(), UserStatus.INACTIVE); assertEquals(UserStatus.INACTIVE, service.getBuyer(admin.getId(), buyer.getId()).getStatus()); service.updateBuyerStatus(admin.getId(), buyer.getId(), UserStatus.ACTIVE); assertEquals(UserStatus.ACTIVE, service.getBuyer(admin.getId(), buyer.getId()).getStatus());
            service.updateSellerStatus(admin.getId(), seller.getId(), UserStatus.INACTIVE); assertEquals(UserStatus.INACTIVE, service.getSeller(admin.getId(), seller.getId()).getStatus()); service.updateSellerStatus(admin.getId(), seller.getId(), UserStatus.ACTIVE);
            assertThrows(AdminService.AdminException.class, () -> service.updateUserStatus(admin.getId(), admin.getId(), UserStatus.INACTIVE));
            assertThrows(AdminService.AdminException.class, () -> service.getBuyer(admin.getId(), seller.getId()));
            assertThrows(AdminService.AdminException.class, () -> service.getBuyer(admin.getId(), 999999L));
        }
    }

    @Test
    void adminManagesProductsAcrossSellersWithoutChangingOwnership() {
        try (HikariDataSource ds = dataSource("jdbc:h2:mem:admin_products;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users = new UserDAO(ds); User admin = users.createUser(new User("Product Admin", "product-admin@example.com", "hash", null, UserRole.ADMIN, UserStatus.ACTIVE)); AdminService service = service(ds, users); Product product = service.getProducts(admin.getId(), "headphones", null, null, ProductStatus.ACTIVE, true).stream().findFirst().orElseThrow(); Long originalSeller = product.getSellerId();
            Product updated = service.updateProduct(admin.getId(), product.getId(), product.getCategoryId(), "Admin Updated Headphones", product.getDescription(), new BigDecimal("2599.00"), product.getStockQuantity(), product.getImageUrl(), ProductStatus.ACTIVE); assertEquals(originalSeller, updated.getSellerId()); assertEquals("Admin Updated Headphones", service.getProduct(admin.getId(), product.getId()).getName());
            service.updateProductStatus(admin.getId(), product.getId(), ProductStatus.INACTIVE); assertEquals(ProductStatus.INACTIVE, service.getProduct(admin.getId(), product.getId()).getStatus()); service.updateProductStatus(admin.getId(), product.getId(), ProductStatus.ACTIVE); assertEquals(ProductStatus.ACTIVE, service.getProduct(admin.getId(), product.getId()).getStatus());
            assertTrue(service.getProducts(admin.getId(), product.getSellerName().split(" ")[0].toLowerCase(), null, null, null, null).stream().anyMatch(p -> p.getSellerId().equals(originalSeller))); assertThrows(AdminService.AdminException.class, () -> service.updateProduct(admin.getId(), product.getId(), 999999L, "Bad", "Bad", new BigDecimal("1.00"), 1, "", ProductStatus.ACTIVE));
        }
    }

    @Test
    void adminSeesHistoricalMultiSellerOrdersAndPlatformStatistics() {
        try (HikariDataSource ds = dataSource("jdbc:h2:mem:admin_orders;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users = new UserDAO(ds); User admin = users.createUser(new User("Order Admin", "order-admin@example.com", "hash", null, UserRole.ADMIN, UserStatus.ACTIVE)); User buyer = users.createUser(new User("Admin Test Buyer", "admin-test-buyer@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE)); AdminService service = service(ds, users); Product product = service.getProducts(admin.getId(), null, null, null, ProductStatus.ACTIVE, true).stream().findFirst().orElseThrow(); CartDAO cart = new CartDAO(ds); CartItemDAO cartItems = new CartItemDAO(ds); ProductDAO products = new ProductDAO(ds); UserService userService = new UserService(users); ProductService productService = new ProductService(products, new CategoryDAO(ds), users); CartService cartService = new CartService(cart, cartItems, productService, userService); OrderDAO orders = new OrderDAO(ds); OrderItemDAO orderItems = new OrderItemDAO(ds); OrderService orderService = new OrderService(ds, cart, cartItems, products, orders, orderItems, userService);
            cartService.addToCart(buyer.getId(), product.getId(), 1); com.fakrudeenmart.model.Order placed = orderService.placeOrder(buyer.getId()); AdminOrder detail = service.getOrder(admin.getId(), placed.getId()); assertNotNull(detail); assertEquals(1, detail.getItems().size()); assertEquals(product.getName(), detail.getItems().get(0).getProductName()); assertEquals(product.getSellerName(), detail.getItems().get(0).getSellerName());
            AdminStats stats = service.getDashboardStatistics(admin.getId()); assertTrue(stats.getTotalUsers() >= 3); assertTrue(stats.getTotalOrders() >= 1); assertTrue(stats.getTotalSales().compareTo(BigDecimal.ZERO) > 0); assertEquals(placed.getTotalAmount(), stats.getTotalSales());
            assertEquals(1, service.getOrders(admin.getId(), "admin-test-buyer", null).size()); service.updateOrderStatus(admin.getId(), placed.getId(), OrderStatus.PROCESSING); assertEquals(OrderStatus.PROCESSING, service.getOrder(admin.getId(), placed.getId()).getStatus()); assertThrows(AdminService.AdminException.class, () -> service.updateOrderStatus(admin.getId(), placed.getId(), OrderStatus.DELIVERED));
            Product current = service.getProduct(admin.getId(), product.getId()); service.updateProduct(admin.getId(), product.getId(), current.getCategoryId(), "Current Product Renamed", current.getDescription(), new BigDecimal("9999.00"), current.getStockQuantity(), current.getImageUrl(), current.getStatus()); assertEquals(product.getName(), service.getOrder(admin.getId(), placed.getId()).getItems().get(0).getProductName()); assertEquals(product.getPrice(), service.getOrder(admin.getId(), placed.getId()).getItems().get(0).getUnitPrice());
        }
    }

    private AdminService service(HikariDataSource ds, UserDAO users) { return new AdminService(new AdminDAO(ds), new SellerDAO(ds), new UserService(users), new CategoryService(new CategoryDAO(ds))); }
    private HikariDataSource dataSource(String url) { Properties p=new Properties(); p.setProperty("database.url",url); p.setProperty("database.driver","org.h2.Driver"); p.setProperty("database.username","sa"); p.setProperty("database.password",""); p.setProperty("database.pool.maximum-size","5"); p.setProperty("database.pool.minimum-idle","1"); p.setProperty("database.pool.connection-timeout-ms","5000"); return DatabaseConnectionManager.createDataSource(p); }
}
