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

class SellerAuthorizationTest {
    @Test
    void unauthenticatedSellerDashboardRedirectsToLogin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class); HttpServletResponse response = mock(HttpServletResponse.class); FilterChain chain = mock(FilterChain.class);
        when(request.getContextPath()).thenReturn("/app"); when(request.getRequestURI()).thenReturn("/app/seller/dashboard"); when(request.getSession(false)).thenReturn(null);
        new AuthenticationFilter().doFilter(request, response, chain);
        verify(response).sendRedirect("/app/seller/login"); verify(chain, never()).doFilter(request, response);
    }

    @Test
    void onlySellerRoleCanPassSellerFilter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class); HttpServletResponse response = mock(HttpServletResponse.class); FilterChain chain = mock(FilterChain.class); HttpSession session = mock(HttpSession.class);
        when(request.getContextPath()).thenReturn("/app"); when(request.getRequestURI()).thenReturn("/app/seller/dashboard"); when(request.getSession(false)).thenReturn(session); when(session.getAttribute(SessionUtil.USER_ID)).thenReturn(2L);
        when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.SELLER.name()); new AuthenticationFilter().doFilter(request, response, chain); verify(chain).doFilter(request, response);
        when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.BUYER.name()); new AuthenticationFilter().doFilter(request, response, chain); verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        when(session.getAttribute(SessionUtil.USER_ROLE)).thenReturn(UserRole.ADMIN.name()); new AuthenticationFilter().doFilter(request, response, chain); verify(response, times(2)).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
