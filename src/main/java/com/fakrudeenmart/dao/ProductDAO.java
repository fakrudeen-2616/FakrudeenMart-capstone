package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.CatalogOptions;
import com.fakrudeenmart.model.PageResult;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDAO.class);
    private final DataSource dataSource;

    public ProductDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public ProductDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public PageResult<Product> findAll(int page, int pageSize) {
        return findByFilters(new CatalogFilter(null, null, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public Optional<Product> findById(Long id) {
        String sql = selectClause() + " WHERE p.id = ? AND p.status = 'ACTIVE'";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find product", exception);
            throw new DataAccessException("Unable to find product", exception);
        }
    }

    public PageResult<Product> findByCategory(Long categoryId, int page, int pageSize) {
        return findByFilters(new CatalogFilter(null, categoryId, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public PageResult<Product> search(String search, int page, int pageSize) {
        return findByFilters(new CatalogFilter(search, null, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public PageResult<Product> findByFilters(CatalogFilter filter) {
        List<Object> parameters = new ArrayList<>();
        String where = buildWhere(filter, parameters);
        String countSql = "SELECT COUNT(*) FROM products p JOIN users u ON u.id = p.seller_id JOIN categories c ON c.id = p.category_id " + where;
        String dataSql = selectClause() + " " + where + " ORDER BY " + filter.sort().getSql() + " LIMIT ? OFFSET ?";
        int total;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement count = connection.prepareStatement(countSql)) {
            bind(count, parameters);
            try (ResultSet result = count.executeQuery()) {
                result.next();
                total = result.getInt(1);
            }
            List<Product> products = new ArrayList<>();
            try (PreparedStatement data = connection.prepareStatement(dataSql)) {
                bind(data, parameters);
                data.setInt(parameters.size() + 1, filter.pageSize());
                data.setInt(parameters.size() + 2, (filter.page() - 1) * filter.pageSize());
                try (ResultSet result = data.executeQuery()) {
                    while (result.next()) products.add(map(result));
                }
            }
            return new PageResult<>(products, filter.page(), filter.pageSize(), total);
        } catch (SQLException exception) {
            LOGGER.error("Unable to load products", exception);
            throw new DataAccessException("Unable to load products", exception);
        }
    }

    public Product create(Product product) {
        String sql = "INSERT INTO products (seller_id, category_id, name, description, price, stock_quantity, image_url, active, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, product.getSellerId()); statement.setLong(2, product.getCategoryId());
            statement.setString(3, product.getName()); statement.setString(4, product.getDescription());
            statement.setBigDecimal(5, product.getPrice()); statement.setInt(6, product.getStockQuantity());
            statement.setString(7, product.getImageUrl()); statement.setBoolean(8, product.getStatus() == ProductStatus.ACTIVE);
            statement.setString(9, product.getStatus().name()); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new DataAccessException("Product was created without an id", null);
                product.setId(keys.getLong(1)); return product;
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to create product", exception);
            throw new DataAccessException("Unable to create product", exception);
        }
    }

    public void update(Product product) {
        String sql = "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, stock_quantity = ?, image_url = ?, active = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND seller_id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, product.getCategoryId()); statement.setString(2, product.getName());
            statement.setString(3, product.getDescription()); statement.setBigDecimal(4, product.getPrice());
            statement.setInt(5, product.getStockQuantity()); statement.setString(6, product.getImageUrl());
            statement.setBoolean(7, product.getStatus() == ProductStatus.ACTIVE); statement.setString(8, product.getStatus().name());
            statement.setLong(9, product.getId()); statement.setLong(10, product.getSellerId()); statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to update product", exception);
            throw new DataAccessException("Unable to update product", exception);
        }
    }

    public void updateStatus(Long productId, ProductStatus status) {
        String sql = "UPDATE products SET active = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, status == ProductStatus.ACTIVE); statement.setString(2, status.name()); statement.setLong(3, productId); statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to update product status", exception);
            throw new DataAccessException("Unable to update product status", exception);
        }
    }

    public void delete(Long productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId); statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Unable to delete product", exception);
            throw new DataAccessException("Unable to delete product", exception);
        }
    }

    public Optional<Product> findById(Connection connection, Long id) {
        String sql = selectClause() + " WHERE p.id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to find product inside checkout transaction", exception);
            throw new DataAccessException("Unable to find product", exception);
        }
    }

    public boolean decreaseStock(Connection connection, Long productId, int quantity) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'ACTIVE' AND stock_quantity >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            LOGGER.error("Unable to deduct product stock", exception);
            throw new DataAccessException("Unable to deduct product stock", exception);
        }
    }

    private String buildWhere(CatalogFilter filter, List<Object> parameters) {
        StringBuilder where = new StringBuilder("WHERE p.status = 'ACTIVE'");
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append(" AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?)");
            String search = "%" + filter.search().trim().toLowerCase() + "%";
            parameters.add(search); parameters.add(search);
        }
        if (filter.categoryId() != null) { where.append(" AND p.category_id = ?"); parameters.add(filter.categoryId()); }
        if (filter.minPrice() != null) { where.append(" AND p.price >= ?"); parameters.add(filter.minPrice()); }
        if (filter.maxPrice() != null) { where.append(" AND p.price <= ?"); parameters.add(filter.maxPrice()); }
        if (filter.stockFilter() == CatalogOptions.StockFilter.IN_STOCK) where.append(" AND p.stock_quantity > 0");
        if (filter.stockFilter() == CatalogOptions.StockFilter.OUT_OF_STOCK) where.append(" AND p.stock_quantity <= 0");
        return where.toString();
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            if (parameter instanceof Long value) statement.setLong(index + 1, value);
            else if (parameter instanceof BigDecimal value) statement.setBigDecimal(index + 1, value);
            else statement.setObject(index + 1, parameter);
        }
    }

    private String selectClause() {
        return "SELECT p.id, p.seller_id, p.category_id, p.name, p.description, p.price, p.stock_quantity, p.image_url, p.status, p.created_at, p.updated_at, c.name AS category_name, u.full_name AS seller_name, (SELECT AVG(rv.rating) FROM reviews rv WHERE rv.product_id = p.id) AS average_rating, (SELECT COUNT(*) FROM reviews rc WHERE rc.product_id = p.id) AS review_count FROM products p JOIN users u ON u.id = p.seller_id JOIN categories c ON c.id = p.category_id";
    }

    private Product map(ResultSet result) throws SQLException {
        Product product = new Product();
        product.setId(result.getLong("id")); product.setSellerId(result.getLong("seller_id")); product.setCategoryId(result.getLong("category_id"));
        product.setName(result.getString("name")); product.setDescription(result.getString("description")); product.setPrice(result.getBigDecimal("price"));
        product.setStockQuantity(result.getInt("stock_quantity")); product.setImageUrl(result.getString("image_url")); product.setStatus(ProductStatus.valueOf(result.getString("status")));
        product.setCategoryName(result.getString("category_name")); product.setSellerName(result.getString("seller_name")); product.setAverageRating(result.getBigDecimal("average_rating")); product.setReviewCount(result.getInt("review_count"));
        Timestamp created = result.getTimestamp("created_at"); Timestamp updated = result.getTimestamp("updated_at");
        product.setCreatedAt(created == null ? null : created.toLocalDateTime()); product.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return product;
    }

    public record CatalogFilter(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                CatalogOptions.StockFilter stockFilter, CatalogOptions.SortOption sort,
                                int page, int pageSize) {}
}
