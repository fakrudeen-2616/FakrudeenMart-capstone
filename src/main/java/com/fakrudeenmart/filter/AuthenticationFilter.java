package com.fakrudeenmart.filter;

import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.servlet.RoleLoginServlet;
import com.fakrudeenmart.util.SessionUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        UserRole requiredRole = requiredRole(path);

        if (requiredRole == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!SessionUtil.isLoggedIn(httpRequest)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + RoleLoginServlet.loginPathFor(requiredRole));
            return;
        }
        if (!SessionUtil.hasRole(httpRequest, requiredRole)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private UserRole requiredRole(String path) {
        if (path.equals("/buyer/login") || path.equals("/buyer/register")
                || path.equals("/seller/login") || path.equals("/seller/register")
                || path.equals("/admin/login")) return null;
        if (path.startsWith("/admin/")) return UserRole.ADMIN;
        if (path.startsWith("/seller/")) return UserRole.SELLER;
        if (path.startsWith("/buyer/")) return UserRole.BUYER;
        if (path.equals("/cart") || path.startsWith("/cart/")) return UserRole.BUYER;
        if (path.equals("/checkout") || path.equals("/place-order") || path.equals("/orders")
                || path.equals("/order") || path.equals("/order-confirmation")
                || path.equals("/reviews") || path.startsWith("/reviews/")) return UserRole.BUYER;
        return null;
    }
}
