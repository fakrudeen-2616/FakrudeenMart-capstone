package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.SellerService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerOrderDetailsServlet extends HttpServlet {
    private SellerService sellerService;
    @Override public void init() { sellerService = new SellerService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { try { request.setAttribute("order", sellerService.getSellerOrder(SellerServletSupport.sellerId(request), SellerServletSupport.id(request.getParameter("id"), "Order was not found."))); request.getRequestDispatcher("/WEB-INF/views/seller/order-details.jsp").forward(request, response); } catch (SellerService.SellerException e) { response.sendError(HttpServletResponse.SC_NOT_FOUND); } }
}
