package com.fakrudeenmart;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.model.CatalogOptions;
import com.fakrudeenmart.model.Category;
import com.fakrudeenmart.model.PageResult;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogIntegrationTest {
    @Test
    void seededCatalogSupportsCategoriesSearchFiltersSortAndPagination() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:catalog_integration;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            CategoryDAO categoryDAO = new CategoryDAO(dataSource);
            ProductDAO productDAO = new ProductDAO(dataSource);
            ProductService service = new ProductService(productDAO, categoryDAO, new com.fakrudeenmart.dao.UserDAO(dataSource));

            List<Category> categories = categoryDAO.findAll();
            assertEquals(8, categories.size());
            PageResult<Product> all = productDAO.findAll(1, 12);
            assertEquals(30, all.getTotalItems());
            assertEquals(12, all.getItems().size());
            assertEquals(3, all.getTotalPages());
            assertTrue(all.getItems().stream().allMatch(product -> product.getStatus().name().equals("ACTIVE")));
            assertTrue(all.getItems().stream().allMatch(product -> product.getCategoryName() != null && product.getSellerName() != null));

            PageResult<Product> search = productDAO.search("HEADPHONES", 1, 12);
            assertEquals(1, search.getTotalItems());
            assertEquals("Wireless Headphones", search.getItems().get(0).getName());

            Long electronicsId = categories.stream().filter(category -> category.getName().equals("Electronics")).findFirst().orElseThrow().getId();
            PageResult<Product> category = productDAO.findByCategory(electronicsId, 1, 12);
            assertEquals(4, category.getTotalItems());

            PageResult<Product> combined = productDAO.findByFilters(new ProductDAO.CatalogFilter(null, null,
                    new BigDecimal("500"), new BigDecimal("2000"), CatalogOptions.StockFilter.IN_STOCK,
                    CatalogOptions.SortOption.PRICE_ASC, 1, 12));
            assertTrue(combined.getTotalItems() > 0);
            assertTrue(combined.getItems().stream().allMatch(product -> product.getPrice().compareTo(new BigDecimal("500")) >= 0
                    && product.getPrice().compareTo(new BigDecimal("2000")) <= 0 && product.isInStock()));
            for (int index = 1; index < combined.getItems().size(); index++) {
                assertTrue(combined.getItems().get(index - 1).getPrice().compareTo(combined.getItems().get(index).getPrice()) <= 0);
            }

            PageResult<Product> secondPage = productDAO.findAll(2, 12);
            assertEquals(12, secondPage.getItems().size());
            assertTrue(secondPage.isNextAvailable());
            assertTrue(secondPage.isPreviousAvailable());
        }
    }

    @Test
    void inactiveProductsAreNotReturnedAsPublicDetails() {
        try (HikariDataSource dataSource = dataSource("jdbc:h2:mem:inactive_catalog;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(dataSource);
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT id FROM products WHERE status = 'INACTIVE' LIMIT 1")) {
                assertTrue(result.next());
                assertTrue(new ProductDAO(dataSource).findById(result.getLong(1)).isEmpty());
            }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private HikariDataSource dataSource(String url) {
        Properties properties = new Properties();
        properties.setProperty("database.url", url);
        properties.setProperty("database.driver", "org.h2.Driver");
        properties.setProperty("database.username", "sa");
        properties.setProperty("database.password", "");
        properties.setProperty("database.pool.maximum-size", "2");
        properties.setProperty("database.pool.minimum-idle", "1");
        properties.setProperty("database.pool.connection-timeout-ms", "5000");
        return DatabaseConnectionManager.createDataSource(properties);
    }
}
