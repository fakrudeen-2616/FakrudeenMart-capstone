package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BuyerAccountServlet extends HttpServlet {
    private UserService userService;

    @Override
    public void init() { userService = new UserService(); }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        render(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = userService.updateBuyerProfile(SessionUtil.getCurrentUserId(request), request.getParameter("fullName"), request.getParameter("phone"));
            SessionUtil.refreshIdentity(request, user);
            response.sendRedirect(request.getContextPath() + "/buyer/account?updated=1");
        } catch (IllegalArgumentException | DataAccessException exception) {
            render(request, response, exception.getMessage());
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String error) throws ServletException, IOException {
        User user = userService.findById(SessionUtil.getCurrentUserId(request)).orElse(null);
        request.setAttribute("buyer", user);
        request.setAttribute("error", error);
        request.getRequestDispatcher("/WEB-INF/views/buyer-account.jsp").forward(request, response);
    }
}
