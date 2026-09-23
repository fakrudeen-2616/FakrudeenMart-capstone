package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.Category;
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

public class CategoryDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryDAO.class);
    private final DataSource dataSource;

    public CategoryDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public CategoryDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Category> findAll() {
        String sql = "SELECT id, name, description, created_at FROM categories ORDER BY name ASC";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) categories.add(map(result));
            return categories;
        } catch (SQLException exception) {
            LOGGER.error("Unable to load categories", exception);
            throw new DataAccessException("Unable to load categories", exception);
        }
    }

    public Optional<Category> findById(Long id) {
        String sql = "SELECT id, name, description, created_at FROM categories WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find category", exception);
            throw new DataAccessException("Unable to find category", exception);
        }
    }

    public Optional<Category> findByName(String name) {
        String sql = "SELECT id, name, description, created_at FROM categories WHERE name = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find category by name", exception);
            throw new DataAccessException("Unable to find category", exception);
        }
    }

    public Category create(Category category) {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new DataAccessException("Category was created without an id", null);
                category.setId(keys.getLong(1));
                return category;
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to create category", exception);
            throw new DataAccessException("Unable to create category", exception);
        }
    }

    private Category map(ResultSet result) throws SQLException {
        Category category = new Category(result.getString("name"), result.getString("description"));
        category.setId(result.getLong("id"));
        Timestamp created = result.getTimestamp("created_at");
        category.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return category;
    }
}
