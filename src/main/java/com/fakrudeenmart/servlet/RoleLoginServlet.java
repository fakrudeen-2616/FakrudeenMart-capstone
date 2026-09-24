package com.fakrudeenmart.servlet;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RoleLoginServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleLoginServlet.class);
    private UserService userService;
    private UserRole role;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        userService = new UserService();
        role = UserRole.valueOf(config.getInitParameter("role"));
    }

    RoleLoginServlet(UserService userService, UserRole role) {
        this.userService = userService;
        this.role = role;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (SessionUtil.isLoggedIn(request)) {
            UserRole current = SessionUtil.getCurrentUserRole(request);
            if (current == role) {
                response.sendRedirect(request.getContextPath() + destinationFor(role));
                return;
            }
        }
        if ("1".equals(request.getParameter("registered"))) request.setAttribute("message", "Account created. Please log in.");
        if ("1".equals(request.getParameter("loggedOut"))) request.setAttribute("message", "You have been signed out.");
        request.setAttribute("loginRole", role.name());
        request.setAttribute("loginAction", request.getContextPath() + loginPathFor(role));
        request.getRequestDispatcher("/WEB-INF/views/role-login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = userService.authenticate(request.getParameter("email"), request.getParameter("password"));
            if (user.getRole() != role) {
                request.setAttribute("error", "This account belongs to a different application area.");
                request.setAttribute("email", request.getParameter("email"));
                request.setAttribute("loginRole", role.name());
                request.setAttribute("loginAction", request.getContextPath() + loginPathFor(role));
                request.getRequestDispatcher("/WEB-INF/views/role-login.jsp").forward(request, response);
                return;
            }
            SessionUtil.establish(request, user);
            response.sendRedirect(request.getContextPath() + destinationFor(role));
        } catch (UserService.AuthenticationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("email", request.getParameter("email"));
            request.setAttribute("loginRole", role.name());
            request.setAttribute("loginAction", request.getContextPath() + loginPathFor(role));
            request.getRequestDispatcher("/WEB-INF/views/role-login.jsp").forward(request, response);
        } catch (DataAccessException exception) {
            LOGGER.error("Role login persistence failed", exception);
            request.setAttribute("error", "We could not sign you in right now. Please try again.");
            request.setAttribute("email", request.getParameter("email"));
            request.setAttribute("loginRole", role.name());
            request.setAttribute("loginAction", request.getContextPath() + loginPathFor(role));
            request.getRequestDispatcher("/WEB-INF/views/role-login.jsp").forward(request, response);
        }
    }

    static String destinationFor(UserRole role) {
        return switch (role) {
            case BUYER -> "/buyer/home";
            case SELLER -> "/seller/dashboard";
            case ADMIN -> "/admin/dashboard";
        };
    }

    public static String loginPathFor(UserRole role) {
        return switch (role) {
            case BUYER -> "/buyer/login";
            case SELLER -> "/seller/login";
            case ADMIN -> "/admin/login";
        };
    }
}
