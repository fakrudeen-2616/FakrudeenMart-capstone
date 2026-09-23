package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.CatalogOptions;
import com.fakrudeenmart.model.PageResult;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

public class ProductListServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListServlet.class);
    private ProductService productService;
    private CategoryService categoryService;

    @Override
    public void init() {
        productService = new ProductService();
        categoryService = new CategoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String search = trimToNull(request.getParameter("search"));
        String categoryText = trimToNull(request.getParameter("category"));
        String stockText = trimToNull(request.getParameter("stock"));
        String sortText = trimToNull(request.getParameter("sort"));
        int page = parsePositiveInt(request.getParameter("page"), 1);
        BigDecimal minPrice = parsePrice(request.getParameter("minPrice"));
        BigDecimal maxPrice = parsePrice(request.getParameter("maxPrice"));
        Long categoryId = parsePositiveLong(categoryText);
        CatalogOptions.StockFilter stock = CatalogOptions.StockFilter.ALL;
        CatalogOptions.SortOption sort = CatalogOptions.SortOption.NEWEST;
        String error = null;

        try {
            if (categoryText != null && categoryId == null) throw new IllegalArgumentException("Invalid category filter.");
            if (request.getParameter("minPrice") != null && minPrice == null) throw new IllegalArgumentException("Invalid minimum price.");
            if (request.getParameter("maxPrice") != null && maxPrice == null) throw new IllegalArgumentException("Invalid maximum price.");
            stock = CatalogOptions.StockFilter.from(stockText);
            sort = CatalogOptions.SortOption.from(sortText);
            PageResult<Product> result = productService.filterProducts(new com.fakrudeenmart.dao.ProductDAO.CatalogFilter(
                    search, categoryId, minPrice, maxPrice, stock, sort, page, ProductService.DEFAULT_PAGE_SIZE));
            setCatalogAttributes(request, result, search, categoryText, minPrice, maxPrice, stockText, sortText);
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
            LOGGER.debug("Invalid catalog request parameter: {}", exception.getMessage());
            PageResult<Product> result = productService.getAllProducts(1, ProductService.DEFAULT_PAGE_SIZE);
            setCatalogAttributes(request, result, null, null, null, null, null, null);
        }
        request.setAttribute("categories", categoryService.getAllCategories());
        request.setAttribute("error", error);
        request.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(request, response);
    }

    private void setCatalogAttributes(HttpServletRequest request, PageResult<Product> result, String search,
                                      String category, BigDecimal minPrice, BigDecimal maxPrice, String stock, String sort) {
        request.setAttribute("pageResult", result);
        request.setAttribute("products", result.getItems());
        request.setAttribute("search", search);
        request.setAttribute("selectedCategory", category);
        request.setAttribute("minPrice", minPrice);
        request.setAttribute("maxPrice", maxPrice);
        request.setAttribute("selectedStock", stock == null ? "all" : stock);
        request.setAttribute("selectedSort", sort == null ? "newest" : sort);
    }

    private int parsePositiveInt(String value, int fallback) {
        try { return value == null ? fallback : Math.max(1, Integer.parseInt(value)); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private Long parsePositiveLong(String value) {
        if (value == null) return null;
        try { long parsed = Long.parseLong(value); return parsed > 0 ? parsed : null; }
        catch (NumberFormatException exception) { return null; }
    }

    private BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException exception) { return null; }
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
