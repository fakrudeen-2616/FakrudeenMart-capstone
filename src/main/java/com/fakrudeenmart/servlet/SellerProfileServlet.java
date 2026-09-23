package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.SellerService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SellerProfileServlet extends HttpServlet {
    private SellerService sellerService;
    @Override public void init() { sellerService = new SellerService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { request.setAttribute("seller", sellerService.getSellerProfile(SellerServletSupport.sellerId(request))); request.getRequestDispatcher("/WEB-INF/views/seller/profile.jsp").forward(request, response); }
}
