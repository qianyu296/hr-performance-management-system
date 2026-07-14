package com.hrpm.security;


import com.hrpm.common.ApiResponse;
import com.hrpm.common.exception.TokenValidationException;
import com.hrpm.service.TokenService;
import com.hrpm.mapper.UserAccountMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final SessionValidator sessionValidator;
    private final PermissionResolver permissionResolver;
    private final ObjectMapper objectMapper;
    private final UserAccountMapper userAccountMapper;

    public TokenAuthenticationFilter(
            TokenService tokenService,
            SessionValidator sessionValidator,
            PermissionResolver permissionResolver,
            ObjectMapper objectMapper, UserAccountMapper userAccountMapper) {
        this.tokenService = tokenService;
        this.sessionValidator = sessionValidator;
        this.permissionResolver = permissionResolver;
        this.objectMapper = objectMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthenticatedUser user = tokenService.verify(authorization.substring("Bearer ".length()));
            if (!sessionValidator.isValid(user)) {
                throw new TokenValidationException("Session token is invalid or expired");
            }
            var account = userAccountMapper == null ? null : userAccountMapper.findById(user.userId());
            if (userAccountMapper != null && (account == null || (account.passwordChangeRequired() && !allowsPasswordChange(request)))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ApiResponse<>(
                        "PASSWORD_CHANGE_REQUIRED", "Password must be changed before using the system", null, null));
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            permissionResolver.permissionsFor(user.userId()).stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (TokenValidationException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiResponse<>(
                    "AUTH_SESSION_INVALID", "Session token is invalid or expired", null, null));
        }
    }

    private boolean allowsPasswordChange(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.endsWith("/auth/change-password") || uri.endsWith("/auth/logout");
    }
}
