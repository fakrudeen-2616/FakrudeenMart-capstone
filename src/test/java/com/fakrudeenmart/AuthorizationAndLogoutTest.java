package com.fakrudeenmart;

import com.fakrudeenmart.filter.AuthenticationFilter;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.servlet.LogoutServlet;
import com.fakrudeenmart.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationAndLogoutTest {
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock HttpSession session;
    @Mock FilterChain chain;

    @Test
    void unauthenticatedBuyerRequestRedirectsToLogin() throws Exception {
        configurePath("/buyer/home");
        when(request.getSession(false)).thenReturn(null);
        new AuthenticationFilter().doFilter(request, response, chain);
        verify(response).sendRedirect("/app/buyer/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void buyerCannotAccessAdminOrSellerRoutes() throws Exception {
        when(request.getContextPath()).thenReturn("/app");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionUtil.USER_ID)).thenReturn(1L);
        when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.BUYER.name());
        AuthenticationFilter filter = new AuthenticationFilter();

        when(request.getRequestURI()).thenReturn("/app/admin/dashboard");
        filter.doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        when(request.getRequestURI()).thenReturn("/app/seller/dashboard");
        filter.doFilter(request, response, chain);
        verify(response, org.mockito.Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void sellerCanAccessSellerRouteAndAdminCannotBeAccessedBySeller() throws Exception {
        when(request.getContextPath()).thenReturn("/app");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionUtil.USER_ID)).thenReturn(2L);
        when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.SELLER.name());
        when(request.getRequestURI()).thenReturn("/app/seller/dashboard");
        new AuthenticationFilter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        when(request.getRequestURI()).thenReturn("/app/admin/dashboard");
        new AuthenticationFilter().doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void logoutInvalidatesSessionAndRedirects() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");
        new LogoutServlet().doGet(request, response);
        verify(session).invalidate();
        verify(response).sendRedirect("/app/buyer/login?loggedOut=1");
    }

    private void configurePath(String path) {
        when(request.getContextPath()).thenReturn("/app");
        when(request.getRequestURI()).thenReturn("/app" + path);
    }
}
