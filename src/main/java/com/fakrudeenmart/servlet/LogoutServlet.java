package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectAfterLogout(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectAfterLogout(request, response);
    }

    private void redirectAfterLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserRole role = SessionUtil.getCurrentUserRole(request);
        invalidate(request);
        if (role == null) role = UserRole.BUYER;
        response.sendRedirect(request.getContextPath() + RoleLoginServlet.loginPathFor(role) + "?loggedOut=1");
    }

    private void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
