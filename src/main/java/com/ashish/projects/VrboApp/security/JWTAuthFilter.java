package com.ashish.projects.VrboApp.security;

import com.ashish.projects.VrboApp.entity.User;
import com.ashish.projects.VrboApp.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Configuration

public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private  final UserService userService;

    public JWTAuthFilter(JWTService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
try {
    final String requestTokenHeader = request.getHeader("Authorization");
    if (requestTokenHeader == null || requestTokenHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    String token = requestTokenHeader.split("Bearer")[1];
    Long userId = jwtService.getUserIdFromToken(token);

    if (userId == null && SecurityContextHolder.getContext().getAuthentication() == null) {
        User user = userService.getUserById(userId);

        // check if the usershould be allowed


        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authenticationToken.setDetails
                (new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
    filterChain.doFilter(request, response);
} catch (JwtException ex) {
handlerExceptionResolver.resolveException(request, response, null, ex);
}
    }
}
