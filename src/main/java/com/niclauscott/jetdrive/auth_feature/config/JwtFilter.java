package com.niclauscott.jetdrive.auth_feature.config;

import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import com.niclauscott.jetdrive.auth_feature.services.JwtService;
import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/auth/") || request.getRequestURI().startsWith("/auth/google/")
                || request.getRequestURI().startsWith("/v3/") || request.getRequestURI().startsWith("/h2-console")
                || request.getRequestURI().startsWith("/public/")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"You need to be authenticated to use this feature\"}");
            return;
        }

        try {
            jwtService.validateAccessToken(authHeader);
        } catch (InvalidOrExpiredTokenException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"You need to be authenticated to use this feature\"}");
            return;
        }

        String userEmail = jwtService.getEmailFromToken(authHeader);
        Optional<User> optional = userRepository.findByEmail(userEmail);
        if (optional.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"You need to be authenticated to use this feature\"}");
            return;
        }

        User user = optional.get();
        UserPrincipal userprincipal =
                new UserPrincipal(user.getId(), user.getEmail(), user.isEnabled(), Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userprincipal, null, userprincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

}
