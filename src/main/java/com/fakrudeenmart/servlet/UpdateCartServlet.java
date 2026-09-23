package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UpdateCartServlet extends HttpServlet {
    private CartService cartService;
    @Override public void init() { cartService = new CartService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try { cartService.updateQuantity(SessionUtil.getCurrentUserId(request), parsePositiveLong(request.getParameter("itemId")), parseQuantity(request.getParameter("quantity"))); redirect(response, request, "Cart updated."); }
        catch (Exception exception) { redirect(response, request, message(exception)); }
    }
    private Long parsePositiveLong(String value) { try { long parsed = Long.parseLong(value == null ? "" : value); if (parsed > 0) return parsed; } catch (NumberFormatException ignored) {} throw new IllegalArgumentException("Invalid cart item."); }
    private int parseQuantity(String value) { try { return Integer.parseInt(value == null ? "" : value); } catch (NumberFormatException ignored) { throw new IllegalArgumentException("Quantity must be a whole number."); } }
    private String message(Exception exception) { return exception instanceof CartService.CartException || exception instanceof IllegalArgumentException ? exception.getMessage() : "Unable to update your cart."; }
    private void redirect(HttpServletResponse response, HttpServletRequest request, String message) throws IOException { response.sendRedirect(request.getContextPath() + "/cart?message=" + URLEncoder.encode(message == null ? "Unable to update your cart." : message, StandardCharsets.UTF_8)); }
}
