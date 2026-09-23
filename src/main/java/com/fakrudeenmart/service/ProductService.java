package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.CatalogOptions;
import com.fakrudeenmart.model.PageResult;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;

import java.math.BigDecimal;
import java.util.Optional;

public class ProductService {
    public static final int DEFAULT_PAGE_SIZE = 12;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;
    private final UserDAO userDAO;

    public ProductService() { this(new ProductDAO(), new CategoryDAO(), new UserDAO()); }
    public ProductService(ProductDAO productDAO, CategoryDAO categoryDAO, UserDAO userDAO) {
        this.productDAO = productDAO; this.categoryDAO = categoryDAO; this.userDAO = userDAO;
    }

    public PageResult<Product> getAllProducts(int page, int pageSize) {
        return filterProducts(new ProductDAO.CatalogFilter(null, null, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public Optional<Product> getProductById(Long id) {
        validateId(id, "Product id");
        return productDAO.findById(id);
    }

    public PageResult<Product> searchProducts(String search, int page, int pageSize) {
        return filterProducts(new ProductDAO.CatalogFilter(search, null, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public PageResult<Product> getProductsByCategory(Long categoryId, int page, int pageSize) {
        validateId(categoryId, "Category id");
        if (categoryDAO.findById(categoryId).isEmpty()) throw new IllegalArgumentException("Category was not found.");
        return filterProducts(new ProductDAO.CatalogFilter(null, categoryId, null, null, CatalogOptions.StockFilter.ALL,
                CatalogOptions.SortOption.NEWEST, page, pageSize));
    }

    public PageResult<Product> filterProducts(ProductDAO.CatalogFilter filter) {
        validateFilter(filter);
        return productDAO.findByFilters(filter);
    }

    public PageResult<Product> sortProducts(ProductDAO.CatalogFilter filter) {
        return filterProducts(filter);
    }

    public Product create(Product product, User seller) {
        validateProduct(product);
        if (seller == null || seller.getRole() != UserRole.SELLER || seller.getId() == null) {
            throw new IllegalArgumentException("Only a valid seller can own a product.");
        }
        User persistedSeller = userDAO.findById(seller.getId()).orElseThrow(
                () -> new IllegalArgumentException("Seller was not found."));
        if (persistedSeller.getRole() != UserRole.SELLER || persistedSeller.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active seller can own a product.");
        }
        if (categoryDAO.findById(product.getCategoryId()).isEmpty()) throw new IllegalArgumentException("Category was not found.");
        product.setSellerId(seller.getId());
        return productDAO.create(product);
    }

    public void changeStatus(Long productId, ProductStatus status) {
        validateId(productId, "Product id");
        if (status == null) throw new IllegalArgumentException("Product status is required.");
        productDAO.updateStatus(productId, status);
    }

    private void validateFilter(ProductDAO.CatalogFilter filter) {
        if (filter == null) throw new IllegalArgumentException("Catalog filters are required.");
        if (filter.page() < 1) throw new IllegalArgumentException("Page must be positive.");
        if (filter.pageSize() < 1 || filter.pageSize() > 24) throw new IllegalArgumentException("Page size must be between 1 and 24.");
        if (filter.categoryId() != null) validateId(filter.categoryId(), "Category id");
        if (filter.minPrice() != null && filter.minPrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Minimum price cannot be negative.");
        if (filter.maxPrice() != null && filter.maxPrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Maximum price cannot be negative.");
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) throw new IllegalArgumentException("Minimum price cannot exceed maximum price.");
        if (filter.search() != null && filter.search().length() > 100) throw new IllegalArgumentException("Search input is too long.");
        if (filter.stockFilter() == null || filter.sort() == null) throw new IllegalArgumentException("Invalid catalog option.");
    }

    private void validateProduct(Product product) {
        if (product == null || product.getName() == null || product.getName().isBlank()) throw new IllegalArgumentException("Product name is required.");
        validateId(product.getCategoryId(), "Category id");
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Product price cannot be negative.");
        if (product.getStockQuantity() < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        if (product.getStatus() == null) product.setStatus(ProductStatus.ACTIVE);
    }

    private void validateId(Long id, String label) {
        if (id == null || id <= 0) throw new IllegalArgumentException(label + " must be positive.");
    }
}
