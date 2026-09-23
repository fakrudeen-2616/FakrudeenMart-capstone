package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OrderDetailsServlet extends HttpServlet {
    private OrderService orderService;
    @Override public void init() { orderService = new OrderService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try { request.setAttribute("order", orderService.getOrderById(SessionUtil.getCurrentUserId(request), Long.parseLong(request.getParameter("id")))); request.getRequestDispatcher("/WEB-INF/views/order-details.jsp").forward(request, response); }
        catch (NumberFormatException | OrderService.OrderException exception) { response.sendError(HttpServletResponse.SC_NOT_FOUND); }
    }
}
