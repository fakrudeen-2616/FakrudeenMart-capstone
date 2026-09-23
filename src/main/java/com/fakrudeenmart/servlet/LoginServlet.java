package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServlet.class);
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("1".equals(request.getParameter("registered"))) {
            request.setAttribute("message", "Account created. Please log in.");
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = userService.authenticate(request.getParameter("email"), request.getParameter("password"));
            SessionUtil.establish(request, user);
            response.sendRedirect(request.getContextPath() + destinationFor(user.getRole()));
        } catch (UserService.AuthenticationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("email", request.getParameter("email"));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } catch (DataAccessException exception) {
            LOGGER.error("Login persistence failed", exception);
            request.setAttribute("error", "We could not sign you in right now. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    private String destinationFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case SELLER -> "/seller/dashboard";
            case BUYER -> "/buyer/home";
        };
    }
}
