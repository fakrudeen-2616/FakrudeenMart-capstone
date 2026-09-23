package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.CategoryService;
import com.fakrudeenmart.service.SellerService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerProductEditServlet extends HttpServlet {
    private SellerService sellerService; private CategoryService categoryService;
    @Override public void init() { sellerService = new SellerService(); categoryService = new CategoryService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { long productId = SellerServletSupport.id(request.getParameter("id"), "Product was not found."); request.setAttribute("product", sellerService.getSellerProduct(SellerServletSupport.sellerId(request), productId)); prepare(request); request.setAttribute("editing", true); request.setAttribute("formAction", request.getContextPath() + "/seller/products/edit?id=" + productId); request.getRequestDispatcher("/WEB-INF/views/seller/product-form.jsp").forward(request, response); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { long productId; try { productId = SellerServletSupport.id(request.getParameter("id"), "Product was not found."); sellerService.updateProduct(SellerServletSupport.sellerId(request), productId, SellerServletSupport.id(request.getParameter("categoryId"), "Select a valid category."), request.getParameter("name"), request.getParameter("description"), SellerServletSupport.price(request.getParameter("price")), SellerServletSupport.stock(request.getParameter("stockQuantity")), request.getParameter("imageUrl"), SellerServletSupport.status(request.getParameter("status"))); response.sendRedirect(request.getContextPath() + "/seller/products"); } catch (Exception e) { request.setAttribute("error", SellerServletSupport.message(e, "Unable to update product.")); prepare(request); request.setAttribute("editing", true); request.setAttribute("formAction", request.getContextPath() + "/seller/products/edit?id=" + request.getParameter("id")); request.getRequestDispatcher("/WEB-INF/views/seller/product-form.jsp").forward(request, response); } }
    private void prepare(HttpServletRequest request) { request.setAttribute("categories", categoryService.getAllCategories()); }
}
