package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CartServlet extends HttpServlet {
    private CartService cartService;
    @Override public void init() { cartService = new CartService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long userId = SessionUtil.getCurrentUserId(request);
        Cart cart = cartService.getCart(userId);
        request.setAttribute("cart", cart);
        request.setAttribute("message", request.getParameter("message"));
        request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
    }
}
