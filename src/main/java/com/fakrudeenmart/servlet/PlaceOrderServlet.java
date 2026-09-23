package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PlaceOrderServlet extends HttpServlet {
    private OrderService orderService;
    @Override public void init() { orderService = new OrderService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try { response.sendRedirect(request.getContextPath() + "/order-confirmation?id=" + orderService.placeOrder(SessionUtil.getCurrentUserId(request)).getId()); }
        catch (OrderService.OrderException exception) { redirectError(request, response, exception.getMessage()); }
        catch (DataAccessException exception) { redirectError(request, response, "We couldn't complete your order. Please try again."); }
    }
    private void redirectError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException { response.sendRedirect(request.getContextPath() + "/checkout?error=" + URLEncoder.encode(message == null ? "We couldn't complete your order. Please try again." : message, StandardCharsets.UTF_8)); }
}
