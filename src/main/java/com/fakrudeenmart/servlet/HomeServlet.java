package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.PageResult;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.ProductService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Serves the public storefront homepage using the existing catalog services. */
public class HomeServlet extends HttpServlet {
    private ProductService productService;
    private CategoryService categoryService;

    @Override
    public void init() {
        productService = new ProductService();
        categoryService = new CategoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PageResult<Product> featured = productService.getAllProducts(1, 8);
            request.setAttribute("featuredProducts", featured.getItems());
            request.setAttribute("categories", categoryService.getAllCategories());
        } catch (RuntimeException exception) {
            request.setAttribute("error", "The marketplace catalog is temporarily unavailable.");
        }
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    }
}
