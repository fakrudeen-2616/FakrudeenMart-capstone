package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.OrderItem;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.SellerOrder;
import com.fakrudeenmart.model.SellerStats;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SellerDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerDAO.class);
    private final DataSource dataSource;
    public SellerDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public SellerDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Product> findProductsBySellerId(Long sellerId) {
        List<Product> products = new ArrayList<>();
        String sql = productSelect() + " WHERE p.seller_id = ? ORDER BY p.created_at DESC, p.id DESC";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, sellerId); try (ResultSet r = s.executeQuery()) { while (r.next()) products.add(mapProduct(r)); } return products; } catch (SQLException e) { throw failure("Unable to load seller products", e); }
    }

    public Optional<Product> findProductByIdAndSellerId(Long productId, Long sellerId) {
        String sql = productSelect() + " WHERE p.id = ? AND p.seller_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, productId); s.setLong(2, sellerId); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(mapProduct(r)) : Optional.empty(); } } catch (SQLException e) { throw failure("Unable to find seller product", e); }
    }

    public List<SellerOrder> findOrdersBySellerId(Long sellerId) { return findSellerOrders(sellerId, null); }
    public Optional<SellerOrder> findOrderByIdAndSellerId(Long orderId, Long sellerId) { List<SellerOrder> orders = findSellerOrders(sellerId, orderId); return orders.stream().findFirst(); }

    public boolean updateOrderStatus(Long orderId, Long sellerId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND EXISTS (SELECT 1 FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = orders.id AND p.seller_id = ?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, status.name()); s.setLong(2, orderId); s.setLong(3, sellerId); return s.executeUpdate() == 1; } catch (SQLException e) { throw failure("Unable to update seller order status", e); }
    }

    public SellerStats getStats(Long sellerId) {
        SellerStats stats = new SellerStats();
        String productSql = "SELECT COUNT(*), COALESCE(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN stock_quantity = 0 THEN 1 ELSE 0 END), 0) FROM products WHERE seller_id = ?";
        String orderSql = "SELECT COUNT(DISTINCT o.id), COALESCE(SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN o.status = 'PROCESSING' THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN o.status = 'SHIPPED' THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), 0) FROM orders o JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id WHERE p.seller_id = ? AND o.status <> 'CANCELLED'";
        String salesSql = "SELECT COALESCE(SUM(oi.subtotal), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id JOIN products p ON p.id = oi.product_id WHERE p.seller_id = ? AND o.status <> 'CANCELLED'";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement s = c.prepareStatement(productSql)) { s.setLong(1, sellerId); try (ResultSet r = s.executeQuery()) { r.next(); stats.setTotalProducts(r.getInt(1)); stats.setActiveProducts(r.getInt(2)); stats.setOutOfStockProducts(r.getInt(3)); } }
            try (PreparedStatement s = c.prepareStatement(orderSql)) { s.setLong(1, sellerId); try (ResultSet r = s.executeQuery()) { r.next(); stats.setTotalOrders(r.getInt(1)); stats.setPendingOrders(r.getInt(2)); stats.setProcessingOrders(r.getInt(3)); stats.setShippedOrders(r.getInt(4)); stats.setDeliveredOrders(r.getInt(5)); } }
            try (PreparedStatement s = c.prepareStatement(salesSql)) { s.setLong(1, sellerId); try (ResultSet r = s.executeQuery()) { r.next(); stats.setTotalSales(r.getBigDecimal(1)); } }
            return stats;
        } catch (SQLException e) { throw failure("Unable to load seller statistics", e); }
    }

    private List<SellerOrder> findSellerOrders(Long sellerId, Long orderId) {
        Map<Long, SellerOrder> grouped = new LinkedHashMap<>();
        String sql = "SELECT o.id AS order_id, o.user_id AS buyer_id, u.full_name AS buyer_name, o.created_at, o.status, oi.id AS item_id, oi.product_id, oi.product_name, oi.unit_price, oi.quantity, oi.subtotal FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id WHERE p.seller_id = ?" + (orderId == null ? "" : " AND o.id = ?") + " ORDER BY o.created_at DESC, o.id DESC, oi.id";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, sellerId); if (orderId != null) s.setLong(2, orderId);
            try (ResultSet r = s.executeQuery()) { while (r.next()) { Long id = r.getLong("order_id"); SellerOrder order = grouped.computeIfAbsent(id, ignored -> mapSellerOrder(r)); order.addItem(mapOrderItem(r)); } }
            return new ArrayList<>(grouped.values());
        } catch (SQLException e) { throw failure("Unable to load seller orders", e); }
    }

    private SellerOrder mapSellerOrder(ResultSet r) {
        try { SellerOrder order = new SellerOrder(); order.setOrderId(r.getLong("order_id")); order.setBuyerId(r.getLong("buyer_id")); order.setBuyerName(r.getString("buyer_name")); Timestamp created = r.getTimestamp("created_at"); order.setCreatedAt(created == null ? null : created.toLocalDateTime()); order.setStatus(OrderStatus.valueOf(r.getString("status"))); return order; } catch (SQLException e) { throw new DataAccessException("Unable to map seller order", e); }
    }
    private OrderItem mapOrderItem(ResultSet r) {
        try { OrderItem item = new OrderItem(); item.setId(r.getLong("item_id")); item.setOrderId(r.getLong("order_id")); item.setProductId(r.getLong("product_id")); item.setProductName(r.getString("product_name")); item.setUnitPrice(r.getBigDecimal("unit_price")); item.setQuantity(r.getInt("quantity")); item.setSubtotal(r.getBigDecimal("subtotal")); return item; } catch (SQLException e) { throw new DataAccessException("Unable to map seller order item", e); }
    }
    private Product mapProduct(ResultSet r) {
        try { Product p = new Product(); p.setId(r.getLong("id")); p.setSellerId(r.getLong("seller_id")); p.setCategoryId(r.getLong("category_id")); p.setName(r.getString("name")); p.setDescription(r.getString("description")); p.setPrice(r.getBigDecimal("price")); p.setStockQuantity(r.getInt("stock_quantity")); p.setImageUrl(r.getString("image_url")); p.setStatus(ProductStatus.valueOf(r.getString("status"))); p.setCategoryName(r.getString("category_name")); Timestamp created = r.getTimestamp("created_at"); Timestamp updated = r.getTimestamp("updated_at"); p.setCreatedAt(created == null ? null : created.toLocalDateTime()); p.setUpdatedAt(updated == null ? null : updated.toLocalDateTime()); return p; } catch (SQLException e) { throw new DataAccessException("Unable to map seller product", e); }
    }
    private String productSelect() { return "SELECT p.id, p.seller_id, p.category_id, p.name, p.description, p.price, p.stock_quantity, p.image_url, p.status, p.created_at, p.updated_at, c.name AS category_name FROM products p JOIN categories c ON c.id = p.category_id"; }
    private DataAccessException failure(String message, SQLException e) { LOGGER.error(message, e); return new DataAccessException(message, e); }
}
