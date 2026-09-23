package com.fakrudeenmart.servlet;

import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RemoveCartItemServlet extends HttpServlet {
    private CartService cartService;
    @Override public void init() { cartService = new CartService(); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try { cartService.removeItem(SessionUtil.getCurrentUserId(request), parsePositiveLong(request.getParameter("itemId"))); redirect(response, request, "Item removed."); }
        catch (Exception exception) { redirect(response, request, message(exception)); }
    }
    private Long parsePositiveLong(String value) { try { long parsed = Long.parseLong(value == null ? "" : value); if (parsed > 0) return parsed; } catch (NumberFormatException ignored) {} throw new IllegalArgumentException("Invalid cart item."); }
    private String message(Exception exception) { return exception instanceof CartService.CartException || exception instanceof IllegalArgumentException ? exception.getMessage() : "Unable to remove that item."; }
    private void redirect(HttpServletResponse response, HttpServletRequest request, String message) throws IOException { response.sendRedirect(request.getContextPath() + "/cart?message=" + URLEncoder.encode(message == null ? "Unable to remove that item." : message, StandardCharsets.UTF_8)); }
}
