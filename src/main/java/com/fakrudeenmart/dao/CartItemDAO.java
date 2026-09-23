package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.CartItem;
import com.fakrudeenmart.model.CartItemView;
import com.fakrudeenmart.model.ProductStatus;
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

public class CartItemDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartItemDAO.class);
    private final DataSource dataSource;
    public CartItemDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public CartItemDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public List<CartItem> findByCartId(Long cartId) {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT id, cart_id, product_id, quantity, created_at, updated_at FROM cart_items WHERE cart_id = ? ORDER BY id";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cartId); try (ResultSet result = statement.executeQuery()) { while (result.next()) items.add(map(result)); } return items;
        } catch (SQLException exception) { LOGGER.error("Unable to load cart items", exception); throw new DataAccessException("Unable to load cart items", exception); }
    }

    public Optional<CartItem> findById(Long itemId) { return findItem("SELECT id, cart_id, product_id, quantity, created_at, updated_at FROM cart_items WHERE id = ?", itemId); }
    public Optional<CartItem> findByCartAndProduct(Long cartId, Long productId) { String sql = "SELECT id, cart_id, product_id, quantity, created_at, updated_at FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, cartId); statement.setLong(2, productId); try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); } } catch (SQLException exception) { throw new DataAccessException("Unable to find cart item", exception); }
    }

    public List<CartItemView> findViewsByCartId(Long cartId) {
        List<CartItemView> items = new ArrayList<>();
        String sql = "SELECT ci.id AS item_id, ci.product_id, ci.quantity, p.name, p.image_url, p.price, p.stock_quantity, p.status FROM cart_items ci JOIN products p ON p.id = ci.product_id WHERE ci.cart_id = ? ORDER BY ci.id";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, cartId); try (ResultSet result = statement.executeQuery()) { while (result.next()) items.add(mapView(result)); } return items; } catch (SQLException exception) { LOGGER.error("Unable to load cart item views", exception); throw new DataAccessException("Unable to load cart items", exception); }
    }

    public List<CartItemView> findViewsByCartId(Connection connection, Long cartId) {
        List<CartItemView> items = new ArrayList<>();
        String sql = "SELECT ci.id AS item_id, ci.product_id, ci.quantity, p.name, p.image_url, p.price, p.stock_quantity, p.status FROM cart_items ci JOIN products p ON p.id = ci.product_id WHERE ci.cart_id = ? ORDER BY ci.id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cartId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(mapView(result));
            }
            return items;
        } catch (SQLException exception) {
            LOGGER.error("Unable to lock cart items", exception);
            throw new DataAccessException("Unable to load cart items", exception);
        }
    }

    public CartItem addItem(CartItem item) {
        String sql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { statement.setLong(1, item.getCartId()); statement.setLong(2, item.getProductId()); statement.setInt(3, item.getQuantity()); statement.executeUpdate(); try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new DataAccessException("Cart item was created without an id", null); item.setId(keys.getLong(1)); return item; } } catch (SQLException exception) { LOGGER.error("Unable to add cart item", exception); throw new DataAccessException("Unable to add cart item", exception); }
    }

    public void updateQuantity(Long itemId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        execute(sql, quantity, itemId);
    }
    public void removeItem(Long itemId) { execute("DELETE FROM cart_items WHERE id = ?", itemId); }
    public void deleteItem(Long itemId) { removeItem(itemId); }
    public void clearCart(Long cartId) { execute("DELETE FROM cart_items WHERE cart_id = ?", cartId); }
    public void clearCart(Connection connection, Long cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cartId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to clear cart inside checkout transaction", exception);
            throw new DataAccessException("Unable to clear cart", exception);
        }
    }

    private void execute(String sql, Object... values) { try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) { if (values[i] instanceof Integer) statement.setInt(i + 1, (Integer) values[i]); else statement.setLong(i + 1, (Long) values[i]); } statement.executeUpdate(); } catch (SQLException exception) { LOGGER.error("Unable to modify cart item", exception); throw new DataAccessException("Unable to modify cart", exception); } }
    private Optional<CartItem> findItem(String sql, Long id) { try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, id); try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); } } catch (SQLException exception) { throw new DataAccessException("Unable to find cart item", exception); } }
    private CartItem map(ResultSet result) throws SQLException { CartItem item = new CartItem(); item.setId(result.getLong("id")); item.setCartId(result.getLong("cart_id")); item.setProductId(result.getLong("product_id")); item.setQuantity(result.getInt("quantity")); Timestamp created = result.getTimestamp("created_at"); Timestamp updated = result.getTimestamp("updated_at"); item.setCreatedAt(created == null ? null : created.toLocalDateTime()); item.setUpdatedAt(updated == null ? null : updated.toLocalDateTime()); return item; }
    private CartItemView mapView(ResultSet result) throws SQLException { CartItemView view = new CartItemView(); view.setItemId(result.getLong("item_id")); view.setProductId(result.getLong("product_id")); view.setProductName(result.getString("name")); view.setImageUrl(result.getString("image_url")); view.setUnitPrice(result.getBigDecimal("price")); view.setQuantity(result.getInt("quantity")); view.setStockQuantity(result.getInt("stock_quantity")); view.setProductStatus(ProductStatus.valueOf(result.getString("status"))); return view; }
}
