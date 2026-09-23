package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CheckoutServlet extends HttpServlet {
    private OrderService orderService;
    @Override public void init() { orderService = new OrderService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try { request.setAttribute("checkout", orderService.getCheckoutData(SessionUtil.getCurrentUserId(request))); }
        catch (OrderService.OrderException exception) { request.setAttribute("error", exception.getMessage()); }
        request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
    }
}
