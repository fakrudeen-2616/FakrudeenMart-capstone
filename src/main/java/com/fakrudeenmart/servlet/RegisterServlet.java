package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RegisterServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServlet.class);
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        try {
            userService.register(fullName, email, phone, password, confirmPassword);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (UserService.DuplicateEmailException | IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            copyFields(request, fullName, email, phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        } catch (DataAccessException exception) {
            LOGGER.error("Registration persistence failed", exception);
            request.setAttribute("error", "We could not create your account right now. Please try again.");
            copyFields(request, fullName, email, phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }

    private void copyFields(HttpServletRequest request, String fullName, String email, String phone) {
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);
    }
}
