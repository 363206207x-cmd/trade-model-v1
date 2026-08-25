package org.example.trademodel.security;

import java.io.IOException;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ActiveUserSessionFilter extends OncePerRequestFilter {
    private final PersonalUserMapper userMapper;

    public ActiveUserSessionFilter(PersonalUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof PersonalUserPrincipal principal) {
            PersonalUserDO current = userMapper.findById(principal.userId());
            long currentVersion = current == null || current.getSessionVersion() == null
                    ? -1L : current.getSessionVersion();
            if (current == null || !Boolean.TRUE.equals(current.getEnabled())
                    || currentVersion != principal.sessionVersion()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                if (request.getRequestURI().startsWith("/api/")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?expired=true");
                }
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
