package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.SellerService;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerProductStatusServlet extends HttpServlet {
    private SellerService sellerService;
    @Override public void init() { sellerService = new SellerService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException { try { long id = SellerServletSupport.id(request.getParameter("id"), "Product was not found."); if ("activate".equals(request.getParameter("action"))) sellerService.activateProduct(SellerServletSupport.sellerId(request), id); else if ("deactivate".equals(request.getParameter("action"))) sellerService.deactivateProduct(SellerServletSupport.sellerId(request), id); else throw new SellerService.SellerException("Product status is invalid."); response.sendRedirect(request.getContextPath() + "/seller/products"); } catch (SellerService.SellerException e) { response.sendError(HttpServletResponse.SC_FORBIDDEN); } }
}
