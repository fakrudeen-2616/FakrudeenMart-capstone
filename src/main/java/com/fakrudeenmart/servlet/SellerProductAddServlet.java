package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.SellerService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerProductAddServlet extends HttpServlet {
    private SellerService sellerService; private CategoryService categoryService;
    @Override public void init() { sellerService = new SellerService(); categoryService = new CategoryService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { prepare(request, false); request.getRequestDispatcher("/WEB-INF/views/seller/product-form.jsp").forward(request, response); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { try { sellerService.createProduct(SellerServletSupport.sellerId(request), SellerServletSupport.id(request.getParameter("categoryId"), "Select a valid category."), request.getParameter("name"), request.getParameter("description"), SellerServletSupport.price(request.getParameter("price")), SellerServletSupport.stock(request.getParameter("stockQuantity")), request.getParameter("imageUrl"), SellerServletSupport.status(request.getParameter("status"))); response.sendRedirect(request.getContextPath() + "/seller/products"); } catch (Exception e) { request.setAttribute("error", SellerServletSupport.message(e, "Unable to create product.")); prepare(request, false); request.getRequestDispatcher("/WEB-INF/views/seller/product-form.jsp").forward(request, response); } }
    private void prepare(HttpServletRequest request, boolean editing) { request.setAttribute("categories", categoryService.getAllCategories()); request.setAttribute("editing", editing); request.setAttribute("formAction", request.getContextPath() + "/seller/products/add"); }
}
