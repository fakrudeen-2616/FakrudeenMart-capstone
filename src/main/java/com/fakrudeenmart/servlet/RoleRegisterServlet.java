package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RoleRegisterServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleRegisterServlet.class);
    private UserService userService;
    private UserRole role;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        userService = new UserService();
        role = UserRole.valueOf(config.getInitParameter("role"));
        if (role == UserRole.ADMIN) throw new ServletException("Administrator self-registration is disabled.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("registrationRole", role.name());
        request.setAttribute("registrationAction", request.getContextPath() + "/" + role.name().toLowerCase() + "/register");
        request.getRequestDispatcher("/WEB-INF/views/role-register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        try {
            if (role == UserRole.BUYER) userService.registerBuyer(fullName, email, phone, password, confirmPassword);
            else userService.registerSeller(fullName, email, phone, password, confirmPassword);
            response.sendRedirect(request.getContextPath() + RoleLoginServlet.loginPathFor(role) + "?registered=1");
        } catch (UserService.DuplicateEmailException | IllegalArgumentException exception) {
            forwardError(request, response, exception.getMessage(), fullName, email, phone);
        } catch (DataAccessException exception) {
            LOGGER.error("Role registration persistence failed", exception);
            forwardError(request, response, "We could not create your account right now. Please try again.", fullName, email, phone);
        }
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response, String error,
                              String fullName, String email, String phone) throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);
        request.setAttribute("registrationRole", role.name());
        request.setAttribute("registrationAction", request.getContextPath() + "/" + role.name().toLowerCase() + "/register");
        request.getRequestDispatcher("/WEB-INF/views/role-register.jsp").forward(request, response);
    }
}
