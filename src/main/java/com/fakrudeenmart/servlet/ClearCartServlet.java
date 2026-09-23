package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ClearCartServlet extends HttpServlet {
    private CartService cartService;
    @Override public void init() { cartService = new CartService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try { cartService.clearCart(SessionUtil.getCurrentUserId(request)); redirect(response, request, "Cart cleared."); }
        catch (Exception exception) { redirect(response, request, exception.getMessage() == null ? "Unable to clear your cart." : exception.getMessage()); }
    }
    private void redirect(HttpServletResponse response, HttpServletRequest request, String message) throws IOException { response.sendRedirect(request.getContextPath() + "/cart?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8)); }
}
