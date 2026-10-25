package com.taskmanagesys.auth.service;

import com.taskmanagesys.user.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getRequestURI().endsWith("/avatar")) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                jwt = tokenParam;
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtService.isTokenValid(jwt)) {
            try {
                Claims claims = jwtService.extractAllClaims(jwt);
                String email = claims.getSubject();
                Number userIdNum = claims.get("userId", Number.class);
                Long userId = userIdNum != null ? userIdNum.longValue() : null;
                String roleStr = claims.get("role", String.class);
                String firstName = claims.get("firstName", String.class);
                String lastName = claims.get("lastName", String.class);

                if (email != null && roleStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Role role = Role.valueOf(roleStr);
                    UserPrincipal principal = new UserPrincipal(userId, email, role, firstName, lastName);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // Invalid token - continue filter chain unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }
}
