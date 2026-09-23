package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.Order;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDAO.class);
    private final DataSource dataSource;
    public OrderDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public OrderDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public Order create(Connection connection, Order order) {
        String sql = "INSERT INTO orders (user_id, total_amount, status, shipping_address) VALUES (?, ?, ?, '')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, order.getUserId()); statement.setBigDecimal(2, order.getTotalAmount()); statement.setString(3, order.getStatus().name()); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new DataAccessException("Order was created without an id", null);
                order.setId(keys.getLong(1)); return order;
            }
        } catch (SQLException exception) { LOGGER.error("Unable to create order", exception); throw new DataAccessException("Unable to create order", exception); }
    }

    public Optional<Order> findById(Long orderId, Long userId) {
        String sql = "SELECT id, user_id, total_amount, status, created_at, updated_at FROM orders WHERE id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, orderId); statement.setLong(2, userId); try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); } } catch (SQLException exception) { LOGGER.error("Unable to find owned order", exception); throw new DataAccessException("Unable to find order", exception); }
    }

    public List<Order> findByUserId(Long userId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, user_id, total_amount, status, created_at, updated_at FROM orders WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, userId); try (ResultSet result = statement.executeQuery()) { while (result.next()) orders.add(map(result)); } return orders; } catch (SQLException exception) { LOGGER.error("Unable to load order history", exception); throw new DataAccessException("Unable to load orders", exception); }
    }

    public boolean hasBuyerPurchasedProduct(Long userId, Long productId) {
        String sql = "SELECT 1 FROM orders o JOIN order_items oi ON oi.order_id = o.id "
                + "WHERE o.user_id = ? AND oi.product_id = ? AND o.status <> 'CANCELLED' LIMIT 1";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId); statement.setLong(2, productId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException exception) {
            LOGGER.error("Unable to verify product purchase", exception);
            throw new DataAccessException("Unable to verify purchase", exception);
        }
    }

    private Order map(ResultSet result) throws SQLException { Order order = new Order(); order.setId(result.getLong("id")); order.setUserId(result.getLong("user_id")); order.setTotalAmount(result.getBigDecimal("total_amount")); order.setStatus(OrderStatus.valueOf(result.getString("status"))); Timestamp created = result.getTimestamp("created_at"); Timestamp updated = result.getTimestamp("updated_at"); order.setCreatedAt(created == null ? null : created.toLocalDateTime()); order.setUpdatedAt(updated == null ? null : updated.toLocalDateTime()); return order; }
}
