package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.AdminOrder;
import com.fakrudeenmart.model.AdminOrderItem;
import com.fakrudeenmart.model.AdminStats;
import com.fakrudeenmart.model.BuyerStats;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.util.DatabaseConnectionManager;

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

public class AdminDAO {
    private final DataSource dataSource;
    public AdminDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public AdminDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public List<User> findUsers(String search, UserRole role, UserStatus status, int limit) {
        List<User> users = new ArrayList<>(); StringBuilder sql = new StringBuilder(userSelect()).append(" WHERE 1=1"); List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) { sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(COALESCE(phone, '')) LIKE ?)"); String q = "%" + search.trim().toLowerCase() + "%"; params.add(q); params.add(q); params.add(q); }
        if (role != null) { sql.append(" AND role = ?"); params.add(role.name()); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status.name()); }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql.toString())) { bind(s, params); s.setInt(params.size() + 1, Math.max(1, Math.min(limit, 500))); try (ResultSet r = s.executeQuery()) { while (r.next()) users.add(mapUser(r)); } return users; } catch (SQLException e) { throw new DataAccessException("Unable to load users", e); }
    }

    public Optional<User> findUser(Long id, UserRole requiredRole) {
        String sql = userSelect() + " WHERE id = ?" + (requiredRole == null ? "" : " AND role = ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); if (requiredRole != null) s.setString(2, requiredRole.name()); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(mapUser(r)) : Optional.empty(); } } catch (SQLException e) { throw new DataAccessException("Unable to find user", e); }
    }

    public void updateProfile(Long id, String fullName, String email, String phone) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, fullName); s.setString(2, email); s.setString(3, phone); s.setLong(4, id); if (s.executeUpdate() != 1) throw new DataAccessException("User was not found", null); } catch (SQLException e) { throw new DataAccessException("Unable to update user profile", e); }
    }

    public boolean updateStatus(Long id, UserStatus status) {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, status.name()); s.setLong(2, id); return s.executeUpdate() == 1; } catch (SQLException e) { throw new DataAccessException("Unable to update user status", e); }
    }

    public List<Product> findProducts(String search, Long categoryId, Long sellerId, ProductStatus status, Boolean inStock, int limit) {
        List<Product> products = new ArrayList<>(); StringBuilder sql = new StringBuilder(productSelect()).append(" WHERE 1=1"); List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) { sql.append(" AND (LOWER(p.name) LIKE ? OR LOWER(u.full_name) LIKE ? OR LOWER(c.name) LIKE ?)"); String q = "%" + search.trim().toLowerCase() + "%"; params.add(q); params.add(q); params.add(q); }
        if (categoryId != null) { sql.append(" AND p.category_id = ?"); params.add(categoryId); }
        if (sellerId != null) { sql.append(" AND p.seller_id = ?"); params.add(sellerId); }
        if (status != null) { sql.append(" AND p.status = ?"); params.add(status.name()); }
        if (Boolean.TRUE.equals(inStock)) sql.append(" AND p.stock_quantity > 0"); else if (Boolean.FALSE.equals(inStock)) sql.append(" AND p.stock_quantity <= 0");
        sql.append(" ORDER BY p.created_at DESC, p.id DESC LIMIT ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql.toString())) { bind(s, params); s.setInt(params.size() + 1, Math.max(1, Math.min(limit, 500))); try (ResultSet r = s.executeQuery()) { while (r.next()) products.add(mapProduct(r)); } return products; } catch (SQLException e) { throw new DataAccessException("Unable to load products", e); }
    }

    public Optional<Product> findProduct(Long id) {
        String sql = productSelect() + " WHERE p.id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(mapProduct(r)) : Optional.empty(); } } catch (SQLException e) { throw new DataAccessException("Unable to find product", e); }
    }

    public void updateProduct(Product p) {
        String sql = "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, stock_quantity = ?, image_url = ?, active = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, p.getCategoryId()); s.setString(2, p.getName()); s.setString(3, p.getDescription()); s.setBigDecimal(4, p.getPrice()); s.setInt(5, p.getStockQuantity()); s.setString(6, p.getImageUrl()); s.setBoolean(7, p.getStatus() == ProductStatus.ACTIVE); s.setString(8, p.getStatus().name()); s.setLong(9, p.getId()); if (s.executeUpdate() != 1) throw new DataAccessException("Product was not found", null); } catch (SQLException e) { throw new DataAccessException("Unable to update product", e); }
    }

    public boolean updateProductStatus(Long id, ProductStatus status) {
        String sql = "UPDATE products SET active = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setBoolean(1, status == ProductStatus.ACTIVE); s.setString(2, status.name()); s.setLong(3, id); return s.executeUpdate() == 1; } catch (SQLException e) { throw new DataAccessException("Unable to update product status", e); }
    }

    public List<AdminOrder> findOrders(String search, OrderStatus status, int limit) {
        Map<Long, AdminOrder> orders = new LinkedHashMap<>(); StringBuilder sql = new StringBuilder("SELECT o.id, o.user_id, o.total_amount, o.status, o.created_at, u.full_name, u.email, u.phone FROM orders o JOIN users u ON u.id = o.user_id WHERE 1=1"); List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) { sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR CAST(o.id AS VARCHAR) LIKE ?)"); String q = "%" + search.trim().toLowerCase() + "%"; params.add(q); params.add(q); params.add(q); }
        if (status != null) { sql.append(" AND o.status = ?"); params.add(status.name()); }
        sql.append(" ORDER BY o.created_at DESC, o.id DESC LIMIT ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql.toString())) { bind(s, params); s.setInt(params.size() + 1, Math.max(1, Math.min(limit, 500))); try (ResultSet r = s.executeQuery()) { while (r.next()) orders.put(r.getLong("id"), mapOrder(r)); } return new ArrayList<>(orders.values()); } catch (SQLException e) { throw new DataAccessException("Unable to load orders", e); }
    }

    public Optional<AdminOrder> findOrder(Long id) {
        String sql = "SELECT o.id, o.user_id, o.total_amount, o.status, o.created_at, u.full_name, u.email, u.phone, oi.id AS item_id, oi.product_id, oi.product_name, oi.unit_price, oi.quantity, oi.subtotal, seller.full_name AS seller_name FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id JOIN users seller ON seller.id = p.seller_id WHERE o.id = ? ORDER BY oi.id";
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { AdminOrder order = null; while (r.next()) { if (order == null) order = mapOrder(r); order.getItems().add(mapOrderItem(r)); } return Optional.ofNullable(order); } } catch (SQLException e) { throw new DataAccessException("Unable to find order", e); }
    }

    public boolean updateOrderStatus(Long id, OrderStatus status) { String sql = "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"; try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, status.name()); s.setLong(2, id); return s.executeUpdate() == 1; } catch (SQLException e) { throw new DataAccessException("Unable to update order status", e); } }
    public List<com.fakrudeenmart.model.Order> findBuyerOrders(Long buyerId) { List<com.fakrudeenmart.model.Order> result = new ArrayList<>(); String sql = "SELECT id, user_id, total_amount, status, created_at, updated_at FROM orders WHERE user_id = ? ORDER BY created_at DESC, id DESC"; try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, buyerId); try (ResultSet r = s.executeQuery()) { while (r.next()) { com.fakrudeenmart.model.Order o = new com.fakrudeenmart.model.Order(); o.setId(r.getLong("id")); o.setUserId(r.getLong("user_id")); o.setTotalAmount(r.getBigDecimal("total_amount")); o.setStatus(OrderStatus.valueOf(r.getString("status"))); Timestamp created = r.getTimestamp("created_at"); o.setCreatedAt(created == null ? null : created.toLocalDateTime()); result.add(o); } } return result; } catch (SQLException e) { throw new DataAccessException("Unable to load buyer orders", e); } }
    public BuyerStats getBuyerStats(Long buyerId) { BuyerStats stats = new BuyerStats(); String sql = "SELECT COUNT(*), COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END), 0) FROM orders WHERE user_id = ?"; try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, buyerId); try (ResultSet r = s.executeQuery()) { r.next(); stats.setTotalOrders(r.getInt(1)); stats.setTotalSpent(r.getBigDecimal(2)); } return stats; } catch (SQLException e) { throw new DataAccessException("Unable to load buyer statistics", e); } }

    public AdminStats getStats() {
        AdminStats stats = new AdminStats();
        try (Connection c = dataSource.getConnection()) {
            queryStats(c, "SELECT COUNT(*), SUM(CASE WHEN role='BUYER' THEN 1 ELSE 0 END), SUM(CASE WHEN role='SELLER' THEN 1 ELSE 0 END), SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END), SUM(CASE WHEN status='INACTIVE' THEN 1 ELSE 0 END) FROM users", r -> { stats.setTotalUsers(r.getInt(1)); stats.setBuyers(r.getInt(2)); stats.setSellers(r.getInt(3)); stats.setActiveUsers(r.getInt(4)); stats.setInactiveUsers(r.getInt(5)); });
            queryStats(c, "SELECT COUNT(*), SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END), SUM(CASE WHEN status='INACTIVE' THEN 1 ELSE 0 END), SUM(CASE WHEN stock_quantity=0 THEN 1 ELSE 0 END) FROM products", r -> { stats.setTotalProducts(r.getInt(1)); stats.setActiveProducts(r.getInt(2)); stats.setInactiveProducts(r.getInt(3)); stats.setOutOfStockProducts(r.getInt(4)); });
            queryStats(c, "SELECT COUNT(*), SUM(CASE WHEN status='PENDING' THEN 1 ELSE 0 END), SUM(CASE WHEN status='PROCESSING' THEN 1 ELSE 0 END), SUM(CASE WHEN status='SHIPPED' THEN 1 ELSE 0 END), SUM(CASE WHEN status='DELIVERED' THEN 1 ELSE 0 END), SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) FROM orders", r -> { stats.setTotalOrders(r.getInt(1)); stats.setPendingOrders(r.getInt(2)); stats.setProcessingOrders(r.getInt(3)); stats.setShippedOrders(r.getInt(4)); stats.setDeliveredOrders(r.getInt(5)); stats.setCancelledOrders(r.getInt(6)); });
            queryStats(c, "SELECT COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END), 0) FROM orders", r -> stats.setTotalSales(r.getBigDecimal(1))); return stats;
        } catch (SQLException e) { throw new DataAccessException("Unable to load platform statistics", e); }
    }
    private interface RowConsumer { void accept(ResultSet r) throws SQLException; }
    private void queryStats(Connection c, String sql, RowConsumer consumer) throws SQLException { try (PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) { r.next(); consumer.accept(r); } }
    private void bind(PreparedStatement s, List<Object> params) throws SQLException { for (int i=0;i<params.size();i++) s.setObject(i+1, params.get(i)); }
    private String userSelect() { return "SELECT id, full_name, email, password_hash, phone, role, status, created_at, updated_at FROM users"; }
    private String productSelect() { return "SELECT p.id, p.seller_id, p.category_id, p.name, p.description, p.price, p.stock_quantity, p.image_url, p.status, p.created_at, p.updated_at, c.name AS category_name, u.full_name AS seller_name FROM products p JOIN categories c ON c.id=p.category_id JOIN users u ON u.id=p.seller_id"; }
    private User mapUser(ResultSet r) throws SQLException { User u = new User(); u.setId(r.getLong("id")); u.setFullName(r.getString("full_name")); u.setEmail(r.getString("email")); u.setPasswordHash(r.getString("password_hash")); u.setPhone(r.getString("phone")); u.setRole(UserRole.valueOf(r.getString("role"))); u.setStatus(UserStatus.valueOf(r.getString("status"))); Timestamp created=r.getTimestamp("created_at"); Timestamp updated=r.getTimestamp("updated_at"); u.setCreatedAt(created==null?null:created.toLocalDateTime()); u.setUpdatedAt(updated==null?null:updated.toLocalDateTime()); return u; }
    private Product mapProduct(ResultSet r) throws SQLException { Product p=new Product(); p.setId(r.getLong("id")); p.setSellerId(r.getLong("seller_id")); p.setCategoryId(r.getLong("category_id")); p.setName(r.getString("name")); p.setDescription(r.getString("description")); p.setPrice(r.getBigDecimal("price")); p.setStockQuantity(r.getInt("stock_quantity")); p.setImageUrl(r.getString("image_url")); p.setStatus(ProductStatus.valueOf(r.getString("status"))); p.setCategoryName(r.getString("category_name")); p.setSellerName(r.getString("seller_name")); Timestamp created=r.getTimestamp("created_at"); Timestamp updated=r.getTimestamp("updated_at"); p.setCreatedAt(created==null?null:created.toLocalDateTime()); p.setUpdatedAt(updated==null?null:updated.toLocalDateTime()); return p; }
    private AdminOrder mapOrder(ResultSet r) throws SQLException { AdminOrder o=new AdminOrder(); o.setId(r.getLong("id")); o.setBuyerId(r.getLong("user_id")); o.setBuyerName(r.getString("full_name")); o.setBuyerEmail(r.getString("email")); o.setBuyerPhone(r.getString("phone")); o.setTotalAmount(r.getBigDecimal("total_amount")); o.setStatus(OrderStatus.valueOf(r.getString("status"))); Timestamp created=r.getTimestamp("created_at"); o.setCreatedAt(created==null?null:created.toLocalDateTime()); return o; }
    private AdminOrderItem mapOrderItem(ResultSet r) throws SQLException { AdminOrderItem i=new AdminOrderItem(); i.setId(r.getLong("item_id")); i.setProductId(r.getLong("product_id")); i.setProductName(r.getString("product_name")); i.setSellerName(r.getString("seller_name")); i.setUnitPrice(r.getBigDecimal("unit_price")); i.setQuantity(r.getInt("quantity")); i.setSubtotal(r.getBigDecimal("subtotal")); return i; }
}
