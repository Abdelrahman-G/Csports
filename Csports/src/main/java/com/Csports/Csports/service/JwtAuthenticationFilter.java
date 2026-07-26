package com.Csports.Csports.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.Csports.Csports.model.User;
import com.Csports.Csports.security.JwtService;
import com.Csports.Csports.service.TokenBlacklistService;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final CustomUserDetailsService userDetailsService;
        private final TokenBlacklistService tokenBlacklistService;

        public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, TokenBlacklistService tokenBlacklistService) {

                this.jwtService = jwtService;
                this.userDetailsService = userDetailsService;
                this.tokenBlacklistService = tokenBlacklistService;
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException, java.io.IOException {
                String authHeader = request.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        filterChain.doFilter(request, response);
                        return;
                }

                String jwt = authHeader.substring(7);

                if (tokenBlacklistService.isBlacklisted(jwt)) {
                        filterChain.doFilter(request, response);
                        return;
                }

                String userId = jwtService.extractUserId(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                        if (jwtService.isTokenValid(jwt, (User) userDetails)) {

                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities());

                                authToken.setDetails(
                                                new WebAuthenticationDetailsSource()
                                                                .buildDetails(request));

                                SecurityContextHolder.getContext()
                                                .setAuthentication(authToken);
                        }
                }

                filterChain.doFilter(request, response);
        }
}