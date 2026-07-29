package com.csports.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.csports.user.User;
import com.csports.security.JwtService;
import com.csports.security.TokenBlacklistService;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final CustomUserDetailsService userDetailsService;
        private final TokenBlacklistService tokenBlacklistService;
        private final ApiAuthenticationEntryPoint authenticationEntryPoint;

        public JwtAuthenticationFilter(
                        JwtService jwtService,
                        CustomUserDetailsService userDetailsService,
                        TokenBlacklistService tokenBlacklistService,
                        ApiAuthenticationEntryPoint authenticationEntryPoint) {

                this.jwtService = jwtService;
                this.userDetailsService = userDetailsService;
                this.tokenBlacklistService = tokenBlacklistService;
                this.authenticationEntryPoint = authenticationEntryPoint;
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

                try {
                        if (tokenBlacklistService.isBlacklisted(jwt)) {
                                authenticationEntryPoint.commence(
                                                request,
                                                response,
                                                new BadCredentialsException("Access token has been revoked"));
                                return;
                        }

                        String userId = jwtService.extractUserId(jwt);

                        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                                if (!jwtService.isTokenValid(jwt, (User) userDetails)) {
                                        authenticationEntryPoint.commence(
                                                        request,
                                                        response,
                                                        new BadCredentialsException("Invalid access token"));
                                        return;
                                }

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
                } catch (JwtException | IllegalArgumentException ex) {
                        SecurityContextHolder.clearContext();
                        authenticationEntryPoint.commence(
                                        request,
                                        response,
                                        new BadCredentialsException("Invalid or expired access token", ex));
                        return;
                } catch (AuthenticationException ex) {
                        SecurityContextHolder.clearContext();
                        authenticationEntryPoint.commence(request, response, ex);
                        return;
                }

                filterChain.doFilter(request, response);
        }
}
