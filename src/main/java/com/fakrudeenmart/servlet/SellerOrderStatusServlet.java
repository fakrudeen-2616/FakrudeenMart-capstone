package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.service.SellerService;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerOrderStatusServlet extends HttpServlet {
    private SellerService sellerService;
    @Override public void init() { sellerService = new SellerService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException { try { sellerService.updateOrderStatus(SellerServletSupport.sellerId(request), SellerServletSupport.id(request.getParameter("id"), "Order was not found."), OrderStatus.valueOf(request.getParameter("status"))); response.sendRedirect(request.getContextPath() + "/seller/order?id=" + request.getParameter("id")); } catch (Exception e) { response.sendError(HttpServletResponse.SC_FORBIDDEN); } }
}
