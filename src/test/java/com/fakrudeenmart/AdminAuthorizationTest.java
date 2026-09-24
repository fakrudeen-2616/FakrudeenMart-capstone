package com.fakrudeenmart;

import com.fakrudeenmart.filter.AuthenticationFilter;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.util.SessionUtil;
import org.junit.jupiter.api.Test;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static org.mockito.Mockito.*;

class AdminAuthorizationTest {
    @Test void unauthenticatedAdminRouteRedirectsToLogin() throws Exception { HttpServletRequest req=mock(HttpServletRequest.class); HttpServletResponse resp=mock(HttpServletResponse.class); FilterChain chain=mock(FilterChain.class); when(req.getContextPath()).thenReturn("/app"); when(req.getRequestURI()).thenReturn("/app/admin/dashboard"); when(req.getSession(false)).thenReturn(null); new AuthenticationFilter().doFilter(req,resp,chain); verify(resp).sendRedirect("/app/admin/login"); verify(chain,never()).doFilter(req,resp); }
    @Test void buyerAndSellerAreForbiddenButAdminPasses() throws Exception { HttpServletRequest req=mock(HttpServletRequest.class); HttpServletResponse resp=mock(HttpServletResponse.class); FilterChain chain=mock(FilterChain.class); HttpSession session=mock(HttpSession.class); when(req.getContextPath()).thenReturn("/app"); when(req.getRequestURI()).thenReturn("/app/admin/users"); when(req.getSession(false)).thenReturn(session); when(session.getAttribute(SessionUtil.USER_ID)).thenReturn(1L); when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.BUYER.name()); new AuthenticationFilter().doFilter(req,resp,chain); verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN); when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.SELLER.name()); new AuthenticationFilter().doFilter(req,resp,chain); verify(resp,times(2)).sendError(HttpServletResponse.SC_FORBIDDEN); when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.ADMIN.name()); new AuthenticationFilter().doFilter(req,resp,chain); verify(chain).doFilter(req,resp); }
}
