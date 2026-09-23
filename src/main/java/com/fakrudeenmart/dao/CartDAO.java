package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class CartDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartDAO.class);
    private final DataSource dataSource;
    public CartDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public CartDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public Optional<Cart> findByUserId(Long userId) { return find("SELECT id, user_id, created_at, updated_at FROM cart WHERE user_id = ?", userId); }
    public Optional<Cart> findById(Long cartId) { return find("SELECT id, user_id, created_at, updated_at FROM cart WHERE id = ?", cartId); }

    public Cart createCart(Long userId) {
        String sql = "INSERT INTO cart (user_id) VALUES (?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new DataAccessException("Cart was created without an id", null);
                Cart cart = new Cart(); cart.setId(keys.getLong(1)); cart.setUserId(userId); return findById(cart.getId()).orElse(cart);
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to create cart", exception);
            throw new DataAccessException("Unable to create cart", exception);
        }
    }

    public Cart getOrCreateCart(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("User id is required.");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement("SELECT id, user_id, created_at, updated_at FROM cart WHERE user_id = ?")) {
                find.setLong(1, userId);
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) { connection.commit(); return map(result); }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO cart (user_id) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insert.setLong(1, userId); insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Missing cart id");
                    Cart cart = new Cart(); cart.setId(keys.getLong(1)); cart.setUserId(userId); connection.commit(); return cart;
                }
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to get or create cart", exception);
            throw new DataAccessException("Unable to get or create cart", exception);
        }
    }

    public Optional<Cart> findByUserId(Connection connection, Long userId) {
        String sql = "SELECT id, user_id, created_at, updated_at FROM cart WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find cart inside checkout transaction", exception);
            throw new DataAccessException("Unable to find cart", exception);
        }
    }

    private Optional<Cart> find(String sql, Long id) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find cart", exception);
            throw new DataAccessException("Unable to find cart", exception);
        }
    }

    private Cart map(ResultSet result) throws SQLException {
        Cart cart = new Cart(); cart.setId(result.getLong("id")); cart.setUserId(result.getLong("user_id"));
        Timestamp created = result.getTimestamp("created_at"); Timestamp updated = result.getTimestamp("updated_at");
        cart.setCreatedAt(created == null ? null : created.toLocalDateTime()); cart.setUpdatedAt(updated == null ? null : updated.toLocalDateTime()); return cart;
    }
}
