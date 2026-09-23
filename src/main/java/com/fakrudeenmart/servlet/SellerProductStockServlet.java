package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.SellerService;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerProductStockServlet extends HttpServlet {
    private SellerService sellerService;
    @Override public void init() { sellerService = new SellerService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException { try { sellerService.updateStock(SellerServletSupport.sellerId(request), SellerServletSupport.id(request.getParameter("id"), "Product was not found."), SellerServletSupport.stock(request.getParameter("stockQuantity"))); response.sendRedirect(request.getContextPath() + "/seller/products"); } catch (SellerService.SellerException e) { response.sendError(HttpServletResponse.SC_FORBIDDEN); } }
}
