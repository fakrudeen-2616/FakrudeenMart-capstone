package com.fakrudeenmart;

import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.OrderItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.SellerDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.SellerOrder;
import com.fakrudeenmart.model.SellerStats;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.SellerService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerIntegrationTest {
    @Test
    void sellerCanManageOwnedProductsButCannotTouchAnotherSellerProduct() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:seller_products;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO userDAO = new UserDAO(dataSource); User sellerOne = userDAO.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow(); User sellerTwo = userDAO.findByEmail("catalog-seller-2@fakrudeenmart.local").orElseThrow();
            SellerService service = sellerService(dataSource, userDAO);
            Product own = service.getSellerProducts(sellerOne.getId()).stream().findFirst().orElseThrow();
            Product other = service.getSellerProducts(sellerTwo.getId()).stream().findFirst().orElseThrow();
            assertEquals(sellerOne.getId(), service.getSellerProduct(sellerOne.getId(), own.getId()).getSellerId());
            assertThrows(SellerService.SellerException.class, () -> service.getSellerProduct(sellerOne.getId(), other.getId()));
            assertThrows(SellerService.SellerException.class, () -> service.updateStock(sellerOne.getId(), other.getId(), 99));
            assertThrows(SellerService.SellerException.class, () -> service.deactivateProduct(sellerOne.getId(), other.getId()));

            Long categoryId = own.getCategoryId();
            Product created = service.createProduct(sellerOne.getId(), categoryId, "Seller Managed Product", "A valid seller product", new BigDecimal("19.99"), 12, "", ProductStatus.ACTIVE);
            assertEquals(sellerOne.getId(), created.getSellerId());
            Product updated = service.updateProduct(sellerOne.getId(), created.getId(), categoryId, "Updated Seller Product", "Updated description", new BigDecimal("29.99"), 20, "", ProductStatus.ACTIVE);
            assertEquals("Updated Seller Product", updated.getName()); assertEquals(new BigDecimal("29.99"), updated.getPrice());
            service.updateStock(sellerOne.getId(), created.getId(), 7); assertEquals(7, service.getSellerProduct(sellerOne.getId(), created.getId()).getStockQuantity());
            service.deactivateProduct(sellerOne.getId(), created.getId()); assertEquals(ProductStatus.INACTIVE, service.getSellerProduct(sellerOne.getId(), created.getId()).getStatus());
            service.activateProduct(sellerOne.getId(), created.getId()); assertEquals(ProductStatus.ACTIVE, service.getSellerProduct(sellerOne.getId(), created.getId()).getStatus());
            assertThrows(SellerService.SellerException.class, () -> service.createProduct(sellerOne.getId(), categoryId, "", "bad", new BigDecimal("1.00"), 1, "", ProductStatus.ACTIVE));
            assertThrows(SellerService.SellerException.class, () -> service.createProduct(sellerOne.getId(), categoryId, "Bad", "bad", new BigDecimal("0"), 1, "", ProductStatus.ACTIVE));
            assertThrows(SellerService.SellerException.class, () -> service.createProduct(sellerOne.getId(), categoryId, "Bad", "bad", new BigDecimal("1.00"), -1, "", ProductStatus.ACTIVE));
            assertThrows(SellerService.SellerException.class, () -> service.createProduct(sellerOne.getId(), 999999L, "Bad", "bad", new BigDecimal("1.00"), 1, "", ProductStatus.ACTIVE));

            userDAO.updateStatus(sellerOne.getId(), UserStatus.INACTIVE);
            assertThrows(SellerService.SellerException.class, () -> service.getSellerProducts(sellerOne.getId()));
        }
    }

    @Test
    void sellerSeesOnlyOwnedItemsInMultiSellerOrdersAndCanAdvanceStatus() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:seller_orders;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            UserDAO userDAO = new UserDAO(dataSource); User sellerOne = userDAO.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow(); User sellerTwo = userDAO.findByEmail("catalog-seller-2@fakrudeenmart.local").orElseThrow();
            User buyer = userDAO.createUser(new User("Seller Order Buyer", "seller-order-buyer@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE));
            SellerService sellerService = sellerService(dataSource, userDAO); ProductDAO productDAO = new ProductDAO(dataSource); Product one = sellerService.getSellerProducts(sellerOne.getId()).stream().filter(Product::isInStock).findFirst().orElseThrow(); Product two = sellerService.getSellerProducts(sellerTwo.getId()).stream().filter(Product::isInStock).findFirst().orElseThrow();
            CartDAO cartDAO = new CartDAO(dataSource); CartItemDAO cartItemDAO = new CartItemDAO(dataSource); UserService userService = new UserService(userDAO); ProductService productService = new ProductService(productDAO, new CategoryDAO(dataSource), userDAO); CartService cartService = new CartService(cartDAO, cartItemDAO, productService, userService); OrderDAO orderDAO = new OrderDAO(dataSource); OrderItemDAO orderItemDAO = new OrderItemDAO(dataSource); OrderService orderService = new OrderService(dataSource, cartDAO, cartItemDAO, productDAO, orderDAO, orderItemDAO, userService);
            cartService.addToCart(buyer.getId(), one.getId(), 1); cartService.addToCart(buyer.getId(), two.getId(), 1);
            com.fakrudeenmart.model.Order order = orderService.placeOrder(buyer.getId());
            SellerOrder sellerOneOrder = sellerService.getSellerOrder(sellerOne.getId(), order.getId()); SellerOrder sellerTwoOrder = sellerService.getSellerOrder(sellerTwo.getId(), order.getId());
            assertEquals(1, sellerOneOrder.getItems().size()); assertEquals(one.getId(), sellerOneOrder.getItems().get(0).getProductId()); assertEquals(1, sellerTwoOrder.getItems().size()); assertEquals(two.getId(), sellerTwoOrder.getItems().get(0).getProductId());
            assertEquals(order.getItems().get(0).getSubtotal().add(order.getItems().get(1).getSubtotal()), order.getTotalAmount());
            SellerStats stats = sellerService.getDashboardStatistics(sellerOne.getId()); assertTrue(stats.getTotalOrders() >= 1); assertEquals(sellerOneOrder.getSellerSubtotal(), stats.getTotalSales());
            sellerService.updateOrderStatus(sellerOne.getId(), order.getId(), OrderStatus.PROCESSING); assertEquals(OrderStatus.PROCESSING, sellerService.getSellerOrder(sellerOne.getId(), order.getId()).getStatus());
            assertThrows(SellerService.SellerException.class, () -> sellerService.updateOrderStatus(sellerOne.getId(), order.getId(), OrderStatus.DELIVERED));
            sellerService.updateOrderStatus(sellerOne.getId(), order.getId(), OrderStatus.SHIPPED); sellerService.updateOrderStatus(sellerOne.getId(), order.getId(), OrderStatus.DELIVERED); assertEquals(OrderStatus.DELIVERED, sellerService.getSellerOrder(sellerTwo.getId(), order.getId()).getStatus());
            assertThrows(SellerService.SellerException.class, () -> sellerService.getSellerOrder(sellerOne.getId(), 999999L));
        }
    }

    private SellerService sellerService(HikariDataSource dataSource, UserDAO userDAO) { return new SellerService(new SellerDAO(dataSource), new ProductDAO(dataSource), new CategoryService(new CategoryDAO(dataSource)), new UserService(userDAO)); }
    private HikariDataSource dataSource(String url) { Properties p = new Properties(); p.setProperty("database.url", url); p.setProperty("database.driver", "org.h2.Driver"); p.setProperty("database.username", "sa"); p.setProperty("database.password", ""); p.setProperty("database.pool.maximum-size", "4"); p.setProperty("database.pool.minimum-idle", "1"); p.setProperty("database.pool.connection-timeout-ms", "5000"); return DatabaseConnectionManager.createDataSource(p); }
}
