package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OrdersServlet extends HttpServlet {
    private OrderService orderService;
    @Override public void init() { orderService = new OrderService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { request.setAttribute("orders", orderService.getUserOrders(SessionUtil.getCurrentUserId(request))); request.getRequestDispatcher("/WEB-INF/views/orders.jsp").forward(request, response); }
}
