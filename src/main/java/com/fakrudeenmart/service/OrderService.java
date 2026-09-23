package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.OrderItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.model.CartItemView;
import com.fakrudeenmart.model.CheckoutData;
import com.fakrudeenmart.model.Order;
import com.fakrudeenmart.model.OrderItem;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final DataSource dataSource;
    private final CartDAO cartDAO;
    private final CartItemDAO cartItemDAO;
    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final UserService userService;

    public OrderService() { this(DatabaseConnectionManager.getInstance().getDataSource(), new CartDAO(), new CartItemDAO(), new ProductDAO(), new OrderDAO(), new OrderItemDAO(), new UserService()); }
    public OrderService(DataSource dataSource, CartDAO cartDAO, CartItemDAO cartItemDAO, ProductDAO productDAO, OrderDAO orderDAO, OrderItemDAO orderItemDAO, UserService userService) {
        this.dataSource = dataSource; this.cartDAO = cartDAO; this.cartItemDAO = cartItemDAO; this.productDAO = productDAO; this.orderDAO = orderDAO; this.orderItemDAO = orderItemDAO; this.userService = userService;
    }

    public CheckoutData getCheckoutData(Long userId) {
        assertBuyer(userId);
        Cart cart = cartDAO.findByUserId(userId).orElseThrow(() -> new OrderException("Your cart is empty."));
        List<CartItemView> views = cartItemDAO.findViewsByCartId(cart.getId());
        if (views.isEmpty()) throw new OrderException("Your cart is empty.");
        return checkoutDataFromViews(views);
    }

    public Order placeOrder(Long userId) {
        assertBuyer(userId);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Cart cart = cartDAO.findByUserId(connection, userId).orElseThrow(() -> new OrderException("Your cart is empty."));
                List<CartItemView> views = cartItemDAO.findViewsByCartId(connection, cart.getId());
                if (views.isEmpty()) throw new OrderException("Your cart is empty.");
                List<OrderItem> orderItems = validateAndSnapshot(connection, views);
                BigDecimal total = orderItems.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                Order order = new Order(); order.setUserId(userId); order.setTotalAmount(total); order.setStatus(OrderStatus.CONFIRMED);
                orderDAO.create(connection, order);
                for (OrderItem item : orderItems) {
                    item.setOrderId(order.getId());
                    orderItemDAO.create(connection, item);
                    if (!productDAO.decreaseStock(connection, item.getProductId(), item.getQuantity())) throw new OrderException("Stock changed while placing your order. Please try again.");
                }
                cartItemDAO.clearCart(connection, cart.getId());
                connection.commit();
                order.setItems(orderItems);
                return order;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new DataAccessException("Unable to place order", exception);
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to open checkout transaction", exception);
            throw new DataAccessException("Unable to place order", exception);
        }
    }

    public List<Order> getUserOrders(Long userId) { assertBuyer(userId); return orderDAO.findByUserId(userId); }

    public Order getOrderById(Long userId, Long orderId) {
        assertBuyer(userId); validateId(orderId);
        Order order = orderDAO.findById(orderId, userId).orElseThrow(() -> new OrderException("Order was not found."));
        order.setItems(orderItemDAO.findByOrderId(orderId));
        return order;
    }

    public List<OrderItem> getOrderItems(Long userId, Long orderId) { return getOrderById(userId, orderId).getItems(); }

    private List<OrderItem> validateAndSnapshot(Connection connection, List<CartItemView> views) {
        List<OrderItem> items = new ArrayList<>();
        for (CartItemView view : views) {
            Product product = productDAO.findById(connection, view.getProductId()).orElseThrow(() -> new OrderException("One or more products are no longer available."));
            if (product.getStatus() != com.fakrudeenmart.model.ProductStatus.ACTIVE) throw new OrderException("One or more products are no longer available.");
            if (view.getQuantity() < 1) throw new OrderException("Cart quantity is invalid.");
            if (view.getQuantity() > product.getStockQuantity()) throw new OrderException("Not enough stock is available for " + product.getName() + ". Only " + product.getStockQuantity() + " units remain.");
            items.add(new OrderItem(product.getId(), product.getName(), product.getPrice(), view.getQuantity()));
        }
        return items;
    }

    private CheckoutData checkoutDataFromViews(List<CartItemView> views) {
        CheckoutData data = new CheckoutData();
        List<OrderItem> items = new ArrayList<>();
        for (CartItemView view : views) {
            if (view.getProductStatus() != com.fakrudeenmart.model.ProductStatus.ACTIVE) throw new OrderException("One or more products are no longer available.");
            if (view.getQuantity() < 1 || view.getQuantity() > view.getStockQuantity()) throw new OrderException("Not enough stock is available for " + view.getProductName() + ". Only " + view.getStockQuantity() + " units remain.");
            items.add(new OrderItem(view.getProductId(), view.getProductName(), view.getUnitPrice(), view.getQuantity()));
        }
        data.setItems(items); data.setTotal(items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)); return data;
    }

    private void assertBuyer(Long userId) {
        if (userId == null || userId <= 0) throw new OrderException("You must be signed in as a buyer.");
        User user = userService.findById(userId).orElseThrow(() -> new OrderException("You must be signed in as a buyer."));
        if (user.getRole() != UserRole.BUYER || user.getStatus() != UserStatus.ACTIVE) throw new OrderException("Only active buyers can checkout.");
    }

    private void validateId(Long id) { if (id == null || id <= 0) throw new OrderException("Order was not found."); }
    private void rollback(Connection connection, Exception cause) { try { connection.rollback(); } catch (SQLException rollbackException) { LOGGER.error("Checkout rollback failed", rollbackException); } LOGGER.warn("Checkout transaction rolled back: {}", cause.getMessage()); }

    public static class OrderException extends RuntimeException { public OrderException(String message) { super(message); } }
}
