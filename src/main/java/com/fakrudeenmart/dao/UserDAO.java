package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
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
import java.util.Optional;

public class UserDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);
    private final DataSource dataSource;

    public UserDAO() {
        this(DatabaseConnectionManager.getInstance().getDataSource());
    }

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, email, password_hash, phone, role, status, created_at, updated_at "
                + "FROM users WHERE email = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find user by email", exception);
            throw new DataAccessException("Unable to find user", exception);
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, full_name, email, password_hash, phone, role, status, created_at, updated_at "
                + "FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find user by id", exception);
            throw new DataAccessException("Unable to find user", exception);
        }
    }

    public User createUser(User user) {
        String sql = "INSERT INTO users (full_name, email, password_hash, phone, role, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getPhone());
            statement.setString(5, user.getRole().name());
            statement.setString(6, user.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("User was created without an id", null);
                }
                user.setId(keys.getLong(1));
                return user;
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to create user", exception);
            throw new DataAccessException("Unable to create user", exception);
        }
    }

    public void updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, role = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getStatus().name());
            statement.setLong(6, user.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to update user", exception);
            throw new DataAccessException("Unable to update user", exception);
        }
    }

    public void updateStatus(Long userId, UserStatus status) {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to update user account status", exception);
            throw new DataAccessException("Unable to update account status", exception);
        }
    }

    public void updateProfile(Long userId, String fullName, String phone) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, phone);
            statement.setLong(3, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to update user profile", exception);
            throw new DataAccessException("Unable to update user profile", exception);
        }
    }

    private User map(ResultSet result) throws SQLException {
        User user = new User();
        user.setId(result.getLong("id"));
        user.setFullName(result.getString("full_name"));
        user.setEmail(result.getString("email"));
        user.setPasswordHash(result.getString("password_hash"));
        user.setPhone(result.getString("phone"));
        user.setRole(UserRole.valueOf(result.getString("role")));
        user.setStatus(UserStatus.valueOf(result.getString("status")));
        Timestamp created = result.getTimestamp("created_at");
        Timestamp updated = result.getTimestamp("updated_at");
        user.setCreatedAt(created == null ? null : created.toLocalDateTime());
        user.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return user;
    }
}
