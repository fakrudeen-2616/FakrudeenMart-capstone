package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.OrderItem;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderItemDAO.class);
    private final DataSource dataSource;
    public OrderItemDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public OrderItemDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public OrderItem create(Connection connection, OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { statement.setLong(1, item.getOrderId()); statement.setLong(2, item.getProductId()); statement.setString(3, item.getProductName()); statement.setBigDecimal(4, item.getUnitPrice()); statement.setInt(5, item.getQuantity()); statement.setBigDecimal(6, item.getSubtotal()); statement.executeUpdate(); try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new DataAccessException("Order item was created without an id", null); item.setId(keys.getLong(1)); return item; } } catch (SQLException exception) { LOGGER.error("Unable to create order item", exception); throw new DataAccessException("Unable to create order item", exception); }
    }

    public List<OrderItem> findByOrderId(Long orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT id, order_id, product_id, product_name, unit_price, quantity, subtotal FROM order_items WHERE order_id = ? ORDER BY id";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, orderId); try (ResultSet result = statement.executeQuery()) { while (result.next()) items.add(map(result)); } return items; } catch (SQLException exception) { LOGGER.error("Unable to load order items", exception); throw new DataAccessException("Unable to load order items", exception); }
    }

    private OrderItem map(ResultSet result) throws SQLException { OrderItem item = new OrderItem(); item.setId(result.getLong("id")); item.setOrderId(result.getLong("order_id")); item.setProductId(result.getLong("product_id")); item.setProductName(result.getString("product_name")); item.setUnitPrice(result.getBigDecimal("unit_price")); item.setQuantity(result.getInt("quantity")); item.setSubtotal(result.getBigDecimal("subtotal")); return item; }
}
